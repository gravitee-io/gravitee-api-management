/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ADMIN_USER, API_PUBLISHER_USER, LOW_PERMISSION_USER } from 'fixtures/fakers/users/users';
import { Api, ApiDefinition, ApiLifecycleState, ApiQualityRule, ApiState, ApiVisibility, ApiWorkflowState } from 'model/apis';
import { ManagementError } from 'model/technical';
import { ApiAssertions, ApiQualityMetricsAssertions } from 'assertions/api.assertion';
import { TechnicalErrorAssertions } from 'assertions/error.assertion';
import { PortalSettings } from 'model/portal-settings';
import { PortalSettingsAssertions } from 'assertions/portal-settings.assertion';
import { QualityRule } from 'model/quality-rules';
import { EnvironmentGroupAssertions, EnvironmentQualityRuleAssertions } from 'assertions/environment-configuration.assertion';
import { Group, GroupEvent } from 'model/groups';
import { Task, TaskQuality, TaskType, User } from 'model/users';
import { Member } from 'model/members';
import { ApiFakers } from 'fixtures/fakers/apis';
import * as faker from 'faker';
import Response = Cypress.Response;
import { gio } from 'commands/gravitee.commands';

context('API - Quality', () => {
  let createdQualityRule: QualityRule;
  let createdGroup: Group;
  let lowPermissionUser: User;
  let createdApi: Api;

  describe('Prepare', () => {
    it('Enable API review and set weights', () => {
      const settings: PortalSettings = {
        apiReview: {
          enabled: true,
        },
        apiQualityMetrics: {
          enabled: true,
          functionalDocumentationWeight: null,
          technicalDocumentationWeight: 0,
          descriptionWeight: 25,
          descriptionMinLength: 25,
          logoWeight: 0,
          categoriesWeight: 0,
          labelsWeight: 25,
          healthcheckWeight: 0,
        },
      };
      gio
        .management(ADMIN_USER)
        .portalSettings()
        .postPortalSettings(settings)
        .ok()
        .should((response) => {
          PortalSettingsAssertions.assertThat(response).hasApiQualityMetricsEnabled().hasApiReviewEnabled();
        });
    });

    it('Should create a manual rule', () => {
      const qualityRule: QualityRule = {
        description: 'This is a manual rule to check',
        name: 'Cypress quality rule',
        weight: 50,
      };

      gio
        .management(ADMIN_USER)
        .environmentConfiguration()
        .addQualityRule(qualityRule)
        .ok()
        .should((response) => {
          EnvironmentQualityRuleAssertions.assertThat(response)
            .hasName(qualityRule.name)
            .hasDescription(qualityRule.description)
            .hasWeight(qualityRule.weight);
        })
        .then((response) => {
          createdQualityRule = response.body;
        });
    });

    it('Should create REVIEWER group', () => {
      const group: Group = {
        name: faker.random.words(3),
        event_rules: [
          {
            event: GroupEvent.API_CREATE,
          },
        ],
        lock_api_role: false,
        lock_application_role: false,
        disable_membership_notifications: false,
      };

      gio
        .management(ADMIN_USER)
        .environmentConfiguration()
        .createGroup(group)
        .created()
        .should((response) => {
          EnvironmentGroupAssertions.assertThat(response).hasName(group.name);
        })
        .then((response) => {
          createdGroup = response.body;
        });
    });

    it('Should associate REVIEWER group to existings APIS', () => {
      gio
        .management(ADMIN_USER)
        .environmentConfiguration()
        .associateGroup(createdGroup.id, { type: 'api' })
        .ok()
        .should((response) => {
          EnvironmentGroupAssertions.assertThat(response).hasName(createdGroup.name);
        });
    });

    // TODO: should be preferable to create a user for this scenario
    // indeed, since this user is used in other scenario, the fact that he now belong to a group can make other scenario fails
    it('Should get LOW_PERMISSION user', () => {
      gio
        .management(ADMIN_USER)
        .users()
        .searchUser(LOW_PERMISSION_USER.username)
        .ok()
        .should((response) => {
          const users = response.body;
          const lowPermUserFromResponse = users.find((u) => u.displayName === LOW_PERMISSION_USER.username);

          expect(lowPermUserFromResponse).to.exist;
          lowPermissionUser = lowPermUserFromResponse;
        });
    });

    it('Should add LOW_PERMISSION user to REVIEWER group', () => {
      const members: Member[] = [
        {
          reference: lowPermissionUser.reference,
          roles: [
            {
              scope: 'API',
              name: 'REVIEWER',
            },
            {
              scope: 'APPLICATION',
              name: 'USER',
            },
          ],
        },
      ];
      gio
        .management(ADMIN_USER)
        .environmentConfiguration()
        .addMembersToGroup(createdGroup.id, members)
        .ok()
        .should((response) => {
          expect(response.body).to.be.undefined;
        });
    });
  });

  describe('API_PUBLISHER asks for review', () => {
    it('Should import API', () => {
      const apiDefinition: ApiDefinition = ApiFakers.apiDefinition();
      apiDefinition.description = faker.datatype.string(15);
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .importApi(apiDefinition)
        .ok()
        .should((response) => {
          ApiAssertions.assertThat(response)
            .hasId()
            .hasName(apiDefinition.name)
            .hasVisibility(ApiVisibility.PRIVATE)
            .hasLifecycleState(ApiLifecycleState.CREATED);
        })
        .then((response) => {
          createdApi = response.body;
        });
    });

    it('Should add label to API', () => {
      const apiWithLabel = {
        ...createdApi,
        labels: ['quality'],
      };
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .update(createdApi.id, apiWithLabel)
        .ok()
        .should((response) => {
          ApiAssertions.assertThat(response).hasLabels().containsLabel('quality');
        });
    });

    it('Should ask for review', () => {
      gio.management(API_PUBLISHER_USER).apisQuality().doApiReview(createdApi.id, { action: 'ASK' }).noContent();
    });

    it('Should not be able to start API', () => {
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .start<ManagementError>(createdApi.id)
        .badRequest()
        .should((response) => {
          TechnicalErrorAssertions.assertThat(response).containsMessage('API can not be started without being reviewed');
        });
    });
  });

  describe('REVIEWER do the review', () => {
    it('Should have task to review', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .users()
        .getTasks()
        .ok()
        .should((response) => {
          expect(response.body.data).to.be.not.empty;
          const inReviewTasks = response.body.data.filter((d) => d.type === TaskType.IN_REVIEW);
          expect(inReviewTasks).to.not.be.empty;
          const task: Task = inReviewTasks.find((t: Task) => (t.data as TaskQuality).referenceId === createdApi.id);
          const taskData: TaskQuality = task.data as TaskQuality;
          expect(taskData).to.exist;
          expect(taskData.referenceType).to.eq('API');
          expect(taskData.type).to.eq('REVIEW');
          expect(taskData.state).to.eq('IN_REVIEW');
        });
    });

    it('Should get APIs and find the one being reviewed with IN_REVIEW status', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apis()
        .getAll()
        .ok()
        .should((response) => {
          const api: Api = response.body.find((api) => api.id === createdApi.id);
          expect(api).to.exist;
          const apiResponse = {
            ...response,
            body: api,
          };
          ApiAssertions.assertThat(apiResponse).hasWorkflowState(ApiWorkflowState.IN_REVIEW);
        });
    });

    it('Should get the API and check its status', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apis()
        .getApiById(createdApi.id)
        .ok()
        .should((response) => {
          ApiAssertions.assertThat(response)
            .hasState(ApiState.STOPPED)
            .hasVisibility(ApiVisibility.PRIVATE)
            .hasLifecycleState(ApiLifecycleState.CREATED)
            .hasWorkflowState(ApiWorkflowState.IN_REVIEW);
        });
    });

    it('Should get quality rules', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .environmentConfiguration()
        .getQualityRule()
        .ok()
        .should((response) => {
          const rules: QualityRule[] = response.body;
          const rule: QualityRule = rules.find((r) => r.id === createdQualityRule.id);
          expect(rule).to.exist;
          const ruleResponse: Response<QualityRule> = {
            ...response,
            body: rule,
          };
          EnvironmentQualityRuleAssertions.assertThat(ruleResponse).hasWeight(50);
        });
    });

    it('Should get quality score', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apisQuality()
        .getApiQualityScore(createdApi.id)
        .ok()
        .should((response) => {
          ApiQualityMetricsAssertions.assertThat(response)
            .hasMetric('api.quality.metrics.description.weight', false)
            .hasMetric('api.quality.metrics.labels.weight', true)
            .hasMetric(createdQualityRule.id, false)
            .hasScore(0.25);
        });
    });

    it('Should accept custom rule', () => {
      const qualityRule: ApiQualityRule = {
        api: createdApi.id,
        quality_rule: createdQualityRule.id,
        checked: true,
      };
      gio.management(LOW_PERMISSION_USER).apisQuality().addApiQualityRule(createdApi.id, qualityRule).ok();
    });

    it('Should get quality score', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apisQuality()
        .getApiQualityScore(createdApi.id)
        .ok()
        .should((response) => {
          ApiQualityMetricsAssertions.assertThat(response)
            .hasScore(0.75)
            .hasMetric('api.quality.metrics.description.weight', false)
            .hasMetric('api.quality.metrics.labels.weight', true)
            .hasMetric(createdQualityRule.id, true);
        });
    });

    it('Should reject review', () => {
      gio.management(LOW_PERMISSION_USER).apisQuality().doApiReview(createdApi.id, { action: 'REJECT' }).noContent();
    });
  });

  describe('API_PUBLISHER fixes API and asks for review', () => {
    it('Should have task to review', () => {
      gio
        .management(API_PUBLISHER_USER)
        .users()
        .getTasks()
        .ok()
        .should((response) => {
          expect(response.body.data).to.be.not.empty;
          const requestedChangeTasks = response.body.data.filter((d) => d.type === TaskType.REQUEST_FOR_CHANGES);
          expect(requestedChangeTasks).to.not.be.empty;
          const task: Task = requestedChangeTasks.find((t: Task) => (t.data as TaskQuality).referenceId === createdApi.id);
          const taskData: TaskQuality = task.data as TaskQuality;
          expect(taskData).to.exist;
          expect(taskData.referenceType).to.eq('API');
          expect(taskData.type).to.eq('REVIEW');
          expect(taskData.state).to.eq('REQUEST_FOR_CHANGES');
        });
    });

    it('Should add a valid description to the API', () => {
      const apiToUpdate: Api = {
        ...createdApi,
        description: 'This is a more than 25 characters description',
      };
      delete apiToUpdate.id;
      delete apiToUpdate.id;
      delete apiToUpdate.state;
      delete apiToUpdate.created_at;
      delete apiToUpdate.updated_at;
      delete apiToUpdate.owner;
      delete apiToUpdate.contextPath;
      delete apiToUpdate.workflow_state;
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .update(createdApi.id, apiToUpdate)
        .ok()
        .should((response) => {
          ApiAssertions.assertThat(response).hasLabels().hasDescription(apiToUpdate.description);
        });
    });

    it('Should ask for review', () => {
      gio.management(API_PUBLISHER_USER).apisQuality().doApiReview(createdApi.id, { action: 'ASK' }).noContent();
    });

    it('Should get quality score', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apisQuality()
        .getApiQualityScore(createdApi.id)
        .ok()
        .should((response) => {
          ApiQualityMetricsAssertions.assertThat(response)
            .hasScore(1)
            .hasMetric('api.quality.metrics.description.weight', true)
            .hasMetric('api.quality.metrics.labels.weight', true)
            .hasMetric(createdQualityRule.id, true);
        });
    });

    it('Should not be able to start API', () => {
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .start<ManagementError>(createdApi.id)
        .badRequest()
        .should((response) => {
          TechnicalErrorAssertions.assertThat(response).containsMessage('API can not be started without being reviewed');
        });
    });
  });

  describe('REVIEWER do the review again', () => {
    it('Should have task to review', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .users()
        .getTasks()
        .ok()
        .should((response) => {
          expect(response.body.data).to.be.not.empty;
          const inReviewTasks = response.body.data.filter((d) => d.type === TaskType.IN_REVIEW);
          expect(inReviewTasks).to.not.be.empty;
          const task: Task = inReviewTasks.find((t: Task) => (t.data as TaskQuality).referenceId === createdApi.id);
          const taskData: TaskQuality = task.data as TaskQuality;
          expect(taskData).to.exist;
          expect(taskData.referenceType).to.eq('API');
          expect(taskData.type).to.eq('REVIEW');
          expect(taskData.state).to.eq('IN_REVIEW');
        });
    });

    it('Should get APIs and find the one being reviewed with IN_REVIEW status', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apis()
        .getAll()
        .ok()
        .should((response) => {
          const api: Api = response.body.find((api) => api.id === createdApi.id);
          expect(api).to.exist;
          const apiResponse = {
            ...response,
            body: api,
          };
          ApiAssertions.assertThat(apiResponse).hasWorkflowState(ApiWorkflowState.IN_REVIEW);
        });
    });

    it('Should get the API and check its status', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apis()
        .getApiById(createdApi.id)
        .ok()
        .should((response) => {
          ApiAssertions.assertThat(response)
            .hasState(ApiState.STOPPED)
            .hasVisibility(ApiVisibility.PRIVATE)
            .hasLifecycleState(ApiLifecycleState.CREATED)
            .hasWorkflowState(ApiWorkflowState.IN_REVIEW);
        });
    });

    it('Should get quality rules', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .environmentConfiguration()
        .getQualityRule()
        .ok()
        .should((response) => {
          const rules: QualityRule[] = response.body;
          const rule: QualityRule = rules.find((r) => r.id === createdQualityRule.id);
          expect(rule).to.exist;
          const ruleResponse: Response<QualityRule> = {
            ...response,
            body: rule,
          };
          EnvironmentQualityRuleAssertions.assertThat(ruleResponse).hasWeight(50);
        });
    });

    it('Should get quality score', () => {
      gio
        .management(LOW_PERMISSION_USER)
        .apisQuality()
        .getApiQualityScore(createdApi.id)
        .ok()
        .should((response) => {
          ApiQualityMetricsAssertions.assertThat(response)
            .hasMetric('api.quality.metrics.description.weight', true)
            .hasMetric('api.quality.metrics.labels.weight', true)
            .hasMetric(createdQualityRule.id, true)
            .hasScore(1);
        });
    });

    it('Should accept review', () => {
      gio.management(LOW_PERMISSION_USER).apisQuality().doApiReview(createdApi.id, { action: 'ACCEPT' }).noContent();
    });
  });

  describe('API_PUBLISHER can start the API', () => {
    it('Get APIs should contain created api', () => {
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .getAll<Api[]>()
        .ok()
        .should((response) => {
          let apiIds = response.body.map((api) => api.id);
          expect(apiIds).to.contain(createdApi.id);
        });
    });

    it('Should get API', () => {
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .getApiById<Api>(createdApi.id)
        .ok()
        .should((response) => {
          ApiAssertions.assertThat(response)
            .hasId(createdApi.id)
            .hasState(ApiState.STOPPED)
            .hasVisibility(ApiVisibility.PRIVATE)
            .hasLifecycleState(ApiLifecycleState.CREATED)
            .hasWorkflowState(ApiWorkflowState.REVIEW_OK);
        });
    });

    it('Should get quality score', () => {
      gio
        .management(API_PUBLISHER_USER)
        .apisQuality()
        .getApiQualityScore(createdApi.id)
        .ok()
        .should((response) => {
          ApiQualityMetricsAssertions.assertThat(response)
            .hasMetric('api.quality.metrics.description.weight', true)
            .hasMetric('api.quality.metrics.labels.weight', true)
            .hasMetric(createdQualityRule.id, true)
            .hasScore(1);
        });
    });

    it('Should be able to start the API', () => {
      gio.management(API_PUBLISHER_USER).apis().start(createdApi.id).noContent();
    });
  });

  describe('Clean up', () => {
    it('Should stop the API', () => {
      gio.management(ADMIN_USER).apis().stop(createdApi.id).noContent();
    });

    it('Should delete the API', () => {
      gio.management(ADMIN_USER).apis().delete(createdApi.id).noContent();
    });

    it('Should disable Api review and reset weigths', () => {
      const settings: PortalSettings = {
        apiReview: {
          enabled: false,
        },
        apiQualityMetrics: {
          enabled: false,
          functionalDocumentationWeight: null,
          technicalDocumentationWeight: 0,
          descriptionWeight: 0,
          descriptionMinLength: 100,
          logoWeight: 0,
          categoriesWeight: 0,
          labelsWeight: 25,
          healthcheckWeight: 0,
        },
      };
      gio
        .management(ADMIN_USER)
        .portalSettings()
        .postPortalSettings(settings)
        .ok()
        .should((response) => {
          PortalSettingsAssertions.assertThat(response).hasApiQualityMetricsDisabled().hasApiReviewDisabled();
        });
    });

    it('Should delete the manual rule', () => {
      gio.management(ADMIN_USER).environmentConfiguration().deleteQualityRule(createdQualityRule.id).noContent();
    });

    it('Should delete REVIEWER group', () => {
      gio.management(ADMIN_USER).environmentConfiguration().deleteGroup(createdGroup.id).noContent();
    });
  });
});

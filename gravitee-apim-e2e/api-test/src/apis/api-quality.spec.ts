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
import { forManagementAsAdminUser, forManagementAsApiUser, forManagementAsSimpleUser } from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { PortalApi } from '@management-apis/PortalApi';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { UsersApi } from '@management-apis/UsersApi';
import { SearchableUser } from '@management-models/SearchableUser';
import { QualityRuleEntity } from '@management-models/QualityRuleEntity';
import { RoleScope } from '@management-models/RoleScope';
import { GroupEntity } from '@management-models/GroupEntity';
import { APIsApi } from '@management-apis/APIsApi';
import { ApiEntity, ApiEntityStateEnum } from '@management-models/ApiEntity';
import { WorkflowState } from '@management-models/WorkflowState';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { ReviewAction } from '@management-models/ReviewAction';
import { fail, succeed } from '@lib/jest-utils';
import { CurrentUserApi } from '@management-apis/CurrentUserApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { GroupsFaker } from '@management-fakers/GroupsFaker';
import { cloneDeep, find } from 'lodash';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

// admin resources
const portalResourceAsAdmin = new PortalApi(forManagementAsAdminUser());
const configurationResourceAsAdmin = new ConfigurationApi(forManagementAsAdminUser());
const usersResourceAsAdmin = new UsersApi(forManagementAsAdminUser());

// publisher resources
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());
const currentUserResourceAsPublisher = new CurrentUserApi(forManagementAsApiUser());

// reviewer resources
const apisResourceAsReviewer = new APIsApi(forManagementAsSimpleUser());
const currentUserResourceAsReviewer = new CurrentUserApi(forManagementAsSimpleUser());
const configurationResourceAsReviewer = new ConfigurationApi(forManagementAsSimpleUser());

let createdRule: QualityRuleEntity;
let reviewerGroup: GroupEntity;
let userMember: SearchableUser;
let apiEntity: ApiEntity;

describe('API - Quality', () => {
  beforeAll(async () => {
    // enable API review and set weights
    await portalResourceAsAdmin.savePortalConfig({
      envId,
      orgId,
      portalSettingsEntity: {
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
      },
    });

    // create a custom quality rule
    createdRule = await configurationResourceAsAdmin.createQualityRule({
      envId,
      orgId,
      newQualityRuleEntity: {
        description: 'This is a manual rule to check',
        name: 'E2E tests quality rule',
        weight: 50,
      },
    });

    // create a REVIEWER group
    reviewerGroup = await configurationResourceAsAdmin.createGroup({
      envId,
      orgId,
      newGroupEntity: GroupsFaker.newGroup({ event_rules: [{ event: 'API_CREATE' }] }),
    });

    // associate REVIEWER group to existing APIs
    await configurationResourceAsAdmin.addGroupMember({
      envId,
      orgId,
      group: reviewerGroup.id,
      type: 'api',
    });

    // get user member
    const users: SearchableUser[] = await usersResourceAsAdmin.searchUsers({
      envId,
      orgId,
      q: process.env.SIMPLE_USERNAME,
    });
    userMember = find(users, (user) => user.displayName === process.env.SIMPLE_USERNAME);

    // add REVIEWER member to group
    await configurationResourceAsAdmin.addOrUpdateGroupMember({
      envId,
      orgId,
      group: reviewerGroup.id,
      groupMembership: [
        {
          reference: userMember.reference,
          roles: [
            {
              scope: RoleScope.API,
              name: 'REVIEWER',
            },
            {
              scope: RoleScope.APPLICATION,
              name: 'USER',
            },
          ],
        },
      ],
    });
  });

  describe('Api publisher asks for review', () => {
    test('should create API', async () => {
      apiEntity = await succeed(
        apisResourceAsPublisher.importApiDefinitionRaw({
          envId,
          orgId,
          body: ApisFaker.apiImport({ description: 'This is an API' }),
        }),
      );

      expect(apiEntity.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(apiEntity.visibility).toBe('PRIVATE');
      expect(apiEntity.workflow_state).toBe(WorkflowState.DRAFT);
      expect(apiEntity.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    test('should add label to API', async () => {
      let updatedApi = cloneDeep(apiEntity);
      delete updatedApi.id;
      delete updatedApi.state;
      delete updatedApi.created_at;
      delete updatedApi.updated_at;
      delete updatedApi.owner;
      delete updatedApi.context_path;
      delete updatedApi.workflow_state;
      updatedApi.labels = ['quality'];

      apiEntity = await succeed(
        apisResourceAsPublisher.updateApiRaw({
          envId,
          orgId,
          api: apiEntity.id,
          updateApiEntity: updatedApi,
        }),
      );

      expect(apiEntity.labels).toContain('quality');
    });

    test('should ask for API review', async () => {
      await succeed(
        apisResourceAsPublisher.doApiReviewActionRaw({
          envId,
          orgId,
          api: apiEntity.id,
          action: ReviewAction.ASK,
          reviewEntity: {},
        }),
        204,
      );
    });

    test('should fail to start the API that has not been reviewed', async () => {
      await fail(
        apisResourceAsPublisher.doApiLifecycleAction({
          envId,
          orgId,
          api: apiEntity.id,
          action: LifecycleAction.START,
        }),
        400,
        'API can not be started without being reviewed',
      );
    });
  });

  describe('Reviewer reviews and reject it', () => {
    test('should have tasks to review', async () => {
      let tasks = await succeed(currentUserResourceAsReviewer.getUserTasks1Raw({ envId, orgId }));
      let apiInReview = find(tasks.data, (task) => task.data.referenceId === apiEntity.id);

      expect(apiInReview.data.type).toBe('REVIEW');
      expect(apiInReview.data.state).toBe('IN_REVIEW');
    });

    test('should get APIs list containing created api with IN_REVIEW status', async () => {
      let foundApis = await succeed(apisResourceAsReviewer.getApisRaw({ envId, orgId }));
      let foundApi = find(foundApis, (a) => a.id === apiEntity.id);

      expect(foundApi).toBeTruthy();
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
    });

    test('should get API with IN_REVIEW status', async () => {
      let foundApi = await succeed(apisResourceAsReviewer.getApiRaw({ envId, orgId, api: apiEntity.id }));

      expect(foundApi).toBeTruthy();
      expect(foundApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(foundApi.visibility).toBe('PRIVATE');
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
      expect(foundApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    test('should get quality rules containing created rule', async () => {
      let foundRules = await succeed(configurationResourceAsReviewer.getQualityRulesRaw({ envId, orgId }));
      let foundRule = find(foundRules, (rule) => rule.id === createdRule.id);

      expect(foundRule).toBeTruthy();
      expect(foundRule.weight).toBe(50);
    });

    test('should get 25% quality score, custom rule should not be passed', async () => {
      let metric = await succeed(apisResourceAsReviewer.getApiQualityMetricsRaw({ envId, orgId, api: apiEntity.id }));

      expect(metric.score).toBe(0.25);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeFalsy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeFalsy();
    });

    test('should accept custom rule', async () => {
      let qualityRule = await succeed(
        apisResourceAsReviewer.createApiQualityRuleRaw({
          envId,
          orgId,
          api: apiEntity.id,
          newApiQualityRuleEntity: {
            api: apiEntity.id,
            quality_rule: createdRule.id,
            checked: true,
          },
        }),
      );

      expect(qualityRule.quality_rule).toBe(createdRule.id);
      expect(qualityRule.checked).toBeTruthy();
    });

    test('should get 75% quality score, custom rule should be passed', async () => {
      let metric = await succeed(apisResourceAsReviewer.getApiQualityMetricsRaw({ envId, orgId, api: apiEntity.id }));

      expect(metric.score).toBe(0.75);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeFalsy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    test('should reject review', async () => {
      await succeed(
        apisResourceAsReviewer.doApiReviewActionRaw({
          envId,
          orgId,
          api: apiEntity.id,
          action: ReviewAction.REJECT,
          reviewEntity: {},
        }),
        204,
      );
    });
  });

  describe('Api publisher fixes API and asks for review', () => {
    test('should have tasks to review', async () => {
      let tasks = await succeed(currentUserResourceAsPublisher.getUserTasks1Raw({ envId, orgId }));
      let apiInReview = find(tasks.data, (task) => task.data.referenceId === apiEntity.id);

      expect(apiInReview.data.type).toBe('REVIEW');
      expect(apiInReview.data.state).toBe('REQUEST_FOR_CHANGES');
    });

    test('should add a valid description to API', async () => {
      let updatedApi = cloneDeep(apiEntity);
      delete updatedApi.id;
      delete updatedApi.state;
      delete updatedApi.created_at;
      delete updatedApi.updated_at;
      delete updatedApi.owner;
      delete updatedApi.context_path;
      delete updatedApi.workflow_state;
      updatedApi.description = 'This is a more than 25 characters description';

      apiEntity = await succeed(
        apisResourceAsPublisher.updateApiRaw({
          envId,
          orgId,
          api: apiEntity.id,
          updateApiEntity: updatedApi,
        }),
      );

      expect(apiEntity.description).toBe('This is a more than 25 characters description');
    });

    test('should ask for API review', async () => {
      await succeed(
        apisResourceAsPublisher.doApiReviewActionRaw({
          envId,
          orgId,
          api: apiEntity.id,
          action: ReviewAction.ASK,
          reviewEntity: {},
        }),
        204,
      );
    });

    test('should get 100% quality score, all metrics passed', async () => {
      let metric = await succeed(apisResourceAsPublisher.getApiQualityMetricsRaw({ envId, orgId, api: apiEntity.id }));

      expect(metric.score).toBe(1);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeTruthy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    test('should fail to start the API that has not been reviewed', async () => {
      await fail(
        apisResourceAsPublisher.doApiLifecycleAction({
          envId,
          orgId,
          api: apiEntity.id,
          action: LifecycleAction.START,
        }),
        400,
        'API can not be started without being reviewed',
      );
    });
  });

  describe('Reviewer reviews and accept it', () => {
    test('should have tasks to review', async () => {
      let tasks = await succeed(currentUserResourceAsReviewer.getUserTasks1Raw({ envId, orgId }));
      let apiInReview = find(tasks.data, (task) => task.data.referenceId === apiEntity.id);

      expect(apiInReview.data.type).toBe('REVIEW');
      expect(apiInReview.data.state).toBe('IN_REVIEW');
    });

    test('should get APIs list containing created api with IN_REVIEW status', async () => {
      let foundApis = await succeed(apisResourceAsReviewer.getApisRaw({ envId, orgId }));
      let foundApi = find(foundApis, (a) => a.id === apiEntity.id);

      expect(foundApi).toBeTruthy();
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
    });

    test('should get API with IN_REVIEW status', async () => {
      let foundApi = await succeed(apisResourceAsReviewer.getApiRaw({ envId, orgId, api: apiEntity.id }));

      expect(foundApi).toBeTruthy();
      expect(foundApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(foundApi.visibility).toBe('PRIVATE');
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
      expect(foundApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    test('should get quality rules containing created rule', async () => {
      let foundRules = await succeed(configurationResourceAsReviewer.getQualityRulesRaw({ envId, orgId }));
      let foundRule = find(foundRules, (rule) => rule.id === createdRule.id);

      expect(foundRule).toBeTruthy();
      expect(foundRule.weight).toBe(50);
    });

    test('should get 100% quality score, all metrics passed', async () => {
      let metric = await succeed(apisResourceAsReviewer.getApiQualityMetricsRaw({ envId, orgId, api: apiEntity.id }));

      expect(metric.score).toBe(1);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeTruthy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    test('should accept review', async () => {
      await succeed(
        apisResourceAsReviewer.doApiReviewActionRaw({
          envId,
          orgId,
          api: apiEntity.id,
          action: ReviewAction.ACCEPT,
          reviewEntity: {},
        }),
        204,
      );
    });
  });

  describe('Api publisher can start the API', () => {
    test('should get APIs list containing created api with REVIEW_OK status', async () => {
      let foundApis = await succeed(apisResourceAsPublisher.getApisRaw({ envId, orgId }));
      let foundApi = find(foundApis, (a) => a.id === apiEntity.id);

      expect(foundApi).toBeTruthy();
      expect(foundApi.workflow_state).toBe(WorkflowState.REVIEWOK);
    });

    test('should get API with REVIEW_OK status', async () => {
      let foundApi = await succeed(apisResourceAsPublisher.getApiRaw({ envId, orgId, api: apiEntity.id }));

      expect(foundApi).toBeTruthy();
      expect(foundApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(foundApi.visibility).toBe('PRIVATE');
      expect(foundApi.workflow_state).toBe(WorkflowState.REVIEWOK);
      expect(foundApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    test('should get 100% quality score, all metrics passed', async () => {
      let metric = await succeed(apisResourceAsPublisher.getApiQualityMetricsRaw({ envId, orgId, api: apiEntity.id }));

      expect(metric.score).toBe(1);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeTruthy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    test('should start the reviewed API', async () => {
      await succeed(
        apisResourceAsPublisher.doApiLifecycleActionRaw({
          envId,
          orgId,
          api: apiEntity.id,
          action: LifecycleAction.START,
        }),
        204,
      );
      apiEntity.state = ApiEntityStateEnum.STARTED;
    });
  });

  afterAll(async () => {
    // stop and delete created API
    if (apiEntity) {
      if (apiEntity.state != ApiEntityStateEnum.STOPPED) {
        await apisResourceAsPublisher.doApiLifecycleAction({
          envId,
          orgId,
          api: apiEntity.id,
          action: LifecycleAction.STOP,
        });
      }

      await apisResourceAsPublisher.deleteApi({
        envId,
        orgId,
        api: apiEntity.id,
      });
    }

    // disable API review and reset weights
    await portalResourceAsAdmin.savePortalConfig({
      envId,
      orgId,
      portalSettingsEntity: {
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
          labelsWeight: 0,
          healthcheckWeight: 0,
        },
      },
    });

    // delete custom quality rule
    if (createdRule) {
      await configurationResourceAsAdmin.deleteQualityRule({
        envId,
        orgId,
        id: createdRule.id,
      });
    }

    // delete REVIEWER group
    if (reviewerGroup) {
      await configurationResourceAsAdmin.deleteGroup({
        envId,
        orgId,
        group: reviewerGroup.id,
      });
    }
  });
});

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
import * as _ from 'lodash';
import { QualityRuleEntity } from '@management-models/QualityRuleEntity';
import { RoleScope } from '@management-models/RoleScope';
import { GroupEntity } from '@management-models/GroupEntity';
import { APIsApi } from '@management-apis/APIsApi';
import { ApiEntity, ApiEntityStateEnum } from '@management-models/ApiEntity';
import { WorkflowState } from '@management-models/WorkflowState';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { ReviewAction } from '@management-models/ReviewAction';
import { fail } from '../../lib/jest-utils';
import { CurrentUserApi } from '@management-apis/CurrentUserApi';

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
let createdApi: ApiEntity;

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
      newGroupEntity: {
        name: 'E2E tests Reviewer',
        event_rules: [
          {
            event: 'API_CREATE',
          },
        ],
        lock_api_role: false,
        lock_application_role: false,
        disable_membership_notifications: false,
      },
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
    userMember = _.find(users, (user) => user.displayName === process.env.SIMPLE_USERNAME);

    // add REVIEWER member to group
    configurationResourceAsAdmin.addOrUpdateGroupMember({
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
    it('should create API', async () => {
      createdApi = await apisResourceAsPublisher.importApiDefinition({
        envId,
        orgId,
        body: {
          proxy: {
            endpoints: [
              {
                name: 'default',
                target: 'https://api.gravitee.io/echo',
                inherit: true,
              },
            ],
            context_path: '/e2e-tests-review',
          },
          pages: [],
          plans: [],
          tags: [],
          name: 'APIM E2E tests',
          description: 'This is an API',
          version: '1',
        },
      });

      expect(createdApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(createdApi.visibility).toBe('PRIVATE');
      expect(createdApi.workflow_state).toBe(WorkflowState.DRAFT);
      expect(createdApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    it('should add label to API', async () => {
      let updatedApi = _.cloneDeep(createdApi);
      delete updatedApi.id;
      delete updatedApi.state;
      delete updatedApi.created_at;
      delete updatedApi.updated_at;
      delete updatedApi.owner;
      delete updatedApi.context_path;
      delete updatedApi.workflow_state;
      updatedApi.labels = ['quality'];

      updatedApi = await apisResourceAsPublisher.updateApi({
        envId,
        orgId,
        api: createdApi.id,
        updateApiEntity: updatedApi,
      });

      expect(updatedApi.labels).toContain('quality');
    });

    it('should ask for API review', async () => {
      await apisResourceAsPublisher.doApiReviewAction({
        envId,
        orgId,
        api: createdApi.id,
        action: ReviewAction.ASK,
        reviewEntity: {},
      });
    });

    it('should fail to start the API that has not been reviewed', async () => {
      await fail(
        apisResourceAsPublisher.doApiLifecycleAction({
          envId,
          orgId,
          api: createdApi.id,
          action: LifecycleAction.START,
        }),
        400,
        'API can not be started without being reviewed',
      );
    });
  });

  describe('Reviewer reviews and reject it', () => {
    it('should have tasks to review', async () => {
      let tasks = await currentUserResourceAsReviewer.getUserTasks1({ envId, orgId });
      let apiInReview = _.find(tasks.data, (task) => task.data.referenceId === createdApi.id);

      expect(apiInReview.data.type).toBe('REVIEW');
      expect(apiInReview.data.state).toBe('IN_REVIEW');
    });

    it('should get APIs list containing created api with IN_REVIEW status', async () => {
      let foundApis = await apisResourceAsReviewer.getApis({ envId, orgId });
      let foundApi = _.find(foundApis, (a) => a.id === createdApi.id);

      expect(foundApi).toBeTruthy();
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
    });

    it('should get API with IN_REVIEW status', async () => {
      let foundApi = await apisResourceAsReviewer.getApi({ envId, orgId, api: createdApi.id });

      expect(foundApi).toBeTruthy();
      expect(foundApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(foundApi.visibility).toBe('PRIVATE');
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
      expect(foundApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    it('should get quality rules containing created rule', async () => {
      let foundRules = await configurationResourceAsReviewer.getQualityRules({ envId, orgId });
      let foundRule = _.find(foundRules, (rule) => rule.id === createdRule.id);

      expect(foundRule).toBeTruthy();
      expect(foundRule.weight).toBe(50);
    });

    it('should get 25% quality score, custom rule should not be passed', async () => {
      let metric = await apisResourceAsReviewer.getApiQualityMetrics({ envId, orgId, api: createdApi.id });

      expect(metric.score).toBe(0.25);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeFalsy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeFalsy();
    });

    it('should accept custom rule', async () => {
      let qualityRule = await apisResourceAsReviewer.createApiQualityRule({
        envId,
        orgId,
        api: createdApi.id,
        newApiQualityRuleEntity: {
          api: createdApi.id,
          quality_rule: createdRule.id,
          checked: true,
        },
      });

      expect(qualityRule.quality_rule).toBe(createdRule.id);
      expect(qualityRule.checked).toBeTruthy();
    });

    it('should get 75% quality score, custom rule should be passed', async () => {
      let metric = await apisResourceAsReviewer.getApiQualityMetrics({ envId, orgId, api: createdApi.id });

      expect(metric.score).toBe(0.75);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeFalsy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    it('should reject review', async () => {
      await apisResourceAsReviewer.doApiReviewAction({
        envId,
        orgId,
        api: createdApi.id,
        action: ReviewAction.REJECT,
        reviewEntity: {},
      });
    });
  });

  describe('Api publisher fixes API and asks for review', () => {
    it('should have tasks to review', async () => {
      let tasks = await currentUserResourceAsPublisher.getUserTasks1({ envId, orgId });
      let apiInReview = _.find(tasks.data, (task) => task.data.referenceId === createdApi.id);

      expect(apiInReview.data.type).toBe('REVIEW');
      expect(apiInReview.data.state).toBe('REQUEST_FOR_CHANGES');
    });

    it('should add a valid description to API', async () => {
      let updatedApi = _.cloneDeep(createdApi);
      delete updatedApi.id;
      delete updatedApi.state;
      delete updatedApi.created_at;
      delete updatedApi.updated_at;
      delete updatedApi.owner;
      delete updatedApi.context_path;
      delete updatedApi.workflow_state;
      updatedApi.description = 'This is a more than 25 characters description';

      updatedApi = await apisResourceAsPublisher.updateApi({
        envId,
        orgId,
        api: createdApi.id,
        updateApiEntity: updatedApi,
      });

      expect(updatedApi.description).toBe('This is a more than 25 characters description');
    });

    it('should ask for API review', async () => {
      await apisResourceAsPublisher.doApiReviewAction({
        envId,
        orgId,
        api: createdApi.id,
        action: ReviewAction.ASK,
        reviewEntity: {},
      });
    });

    it('should get 100% quality score, all metrics passed', async () => {
      let metric = await apisResourceAsPublisher.getApiQualityMetrics({ envId, orgId, api: createdApi.id });

      expect(metric.score).toBe(1);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeTruthy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    it('should fail to start the API that has not been reviewed', async () => {
      await fail(
        apisResourceAsPublisher.doApiLifecycleAction({
          envId,
          orgId,
          api: createdApi.id,
          action: LifecycleAction.START,
        }),
        400,
        'API can not be started without being reviewed',
      );
    });
  });

  describe('Reviewer reviews and accept it', () => {
    it('should have tasks to review', async () => {
      let tasks = await currentUserResourceAsReviewer.getUserTasks1({ envId, orgId });
      let apiInReview = _.find(tasks.data, (task) => task.data.referenceId === createdApi.id);

      expect(apiInReview.data.type).toBe('REVIEW');
      expect(apiInReview.data.state).toBe('IN_REVIEW');
    });

    it('should get APIs list containing created api with IN_REVIEW status', async () => {
      let foundApis = await apisResourceAsReviewer.getApis({ envId, orgId });
      let foundApi = _.find(foundApis, (a) => a.id === createdApi.id);

      expect(foundApi).toBeTruthy();
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
    });

    it('should get API with IN_REVIEW status', async () => {
      let foundApi = await apisResourceAsReviewer.getApi({ envId, orgId, api: createdApi.id });

      expect(foundApi).toBeTruthy();
      expect(foundApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(foundApi.visibility).toBe('PRIVATE');
      expect(foundApi.workflow_state).toBe(WorkflowState.INREVIEW);
      expect(foundApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    it('should get quality rules containing created rule', async () => {
      let foundRules = await configurationResourceAsReviewer.getQualityRules({ envId, orgId });
      let foundRule = _.find(foundRules, (rule) => rule.id === createdRule.id);

      expect(foundRule).toBeTruthy();
      expect(foundRule.weight).toBe(50);
    });

    it('should get 100% quality score, all metrics passed', async () => {
      let metric = await apisResourceAsReviewer.getApiQualityMetrics({ envId, orgId, api: createdApi.id });

      expect(metric.score).toBe(1);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeTruthy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    it('should accept review', async () => {
      await apisResourceAsReviewer.doApiReviewAction({
        envId,
        orgId,
        api: createdApi.id,
        action: ReviewAction.ACCEPT,
        reviewEntity: {},
      });
    });
  });

  describe('Api publisher can start the API', () => {
    it('should get APIs list containing created api with REVIEW_OK status', async () => {
      let foundApis = await apisResourceAsPublisher.getApis({ envId, orgId });
      let foundApi = _.find(foundApis, (a) => a.id === createdApi.id);

      expect(foundApi).toBeTruthy();
      expect(foundApi.workflow_state).toBe(WorkflowState.REVIEWOK);
    });

    it('should get API with REVIEW_OK status', async () => {
      let foundApi = await apisResourceAsPublisher.getApi({ envId, orgId, api: createdApi.id });

      expect(foundApi).toBeTruthy();
      expect(foundApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(foundApi.visibility).toBe('PRIVATE');
      expect(foundApi.workflow_state).toBe(WorkflowState.REVIEWOK);
      expect(foundApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);
    });

    it('should get 100% quality score, all metrics passed', async () => {
      let metric = await apisResourceAsPublisher.getApiQualityMetrics({ envId, orgId, api: createdApi.id });

      expect(metric.score).toBe(1);
      expect(metric.metrics_passed['api.quality.metrics.description.weight']).toBeTruthy();
      expect(metric.metrics_passed['api.quality.metrics.labels.weight']).toBeTruthy();
      expect(metric.metrics_passed[createdRule.id]).toBeTruthy();
    });

    it('should start the reviewed API', async () => {
      await apisResourceAsPublisher.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.START,
      });
      createdApi.state = ApiEntityStateEnum.STARTED;
    });
  });

  afterAll(async () => {
    // stop and delete created API
    if (createdApi) {
      if (createdApi.state != ApiEntityStateEnum.STOPPED) {
        await apisResourceAsPublisher.doApiLifecycleAction({
          envId,
          orgId,
          api: createdApi.id,
          action: LifecycleAction.STOP,
        });
      }

      await apisResourceAsPublisher.deleteApi({
        envId,
        orgId,
        api: createdApi.id,
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

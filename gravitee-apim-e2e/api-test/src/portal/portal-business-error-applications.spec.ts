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
import { afterAll, beforeAll, describe, test, expect } from '@jest/globals';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { NotificationConfigType } from '@management-models/NotificationConfigType';
import { fail, forbidden, notFound } from '@lib/jest-utils';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { forManagementAsAdminUser, forPortalAsAdminUser, forPortalAsSimpleUser } from '@client-conf/*';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { ApplicationApi, GetApplicationAnalyticsTypeEnum } from '@portal-apis/ApplicationApi';
import { PortalApi as PortalManagementApi } from '@management-apis/PortalApi';
import { RoleScope } from '@management-models/RoleScope';
import { RoleEntity } from '@management-models/RoleEntity';
import { UpdateRoleEntityFromJSON } from '@management-models/UpdateRoleEntity';
import { ApiResponse } from '../../../lib/portal-webclient-sdk/src/lib';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const portalManagementApiAsAdmin = new PortalManagementApi(forManagementAsAdminUser());
const applicationsManagementApiAsAdmin = new ApplicationsApi(forManagementAsAdminUser());
const configurationManagementApiAsAdmin = new ConfigurationApi(forManagementAsAdminUser());

const applicationPortalApiAsAdminUser = new ApplicationApi(forPortalAsAdminUser());
const applicationPortalApiAsSimpleUser = new ApplicationApi(forPortalAsSimpleUser());

const memberId = 'MEMBER';
const logId = 'LOG';

describe('Portal: Business Error - applications', () => {
  let createApplication: ApplicationEntity;
  beforeAll(async () => {
    createApplication = await applicationsManagementApiAsAdmin.createApplication({
      orgId,
      envId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });
  });

  describe('400', () => {
    let notificationSettings;
    beforeAll(async () => {
      await portalManagementApiAsAdmin.savePortalConfig({
        orgId,
        envId,
        portalSettingsEntity: { application: { registration: { enabled: true } } },
      });
      const notificationSettingsAsString = await configurationManagementApiAsAdmin.createPortalNotificationSetting({
        orgId,
        envId,
        genericNotificationConfigEntity: {
          name: '400GenericConfig',
          config_type: NotificationConfigType.GENERIC,
          referenceId: 'DEFAULT',
          notifier: 'default-email',
          hooks: [],
          referenceType: 'PORTAL',
        },
      });
      notificationSettings = JSON.parse(notificationSettingsAsString);
      expect(notificationSettings.id).toBeDefined();
    });

    const nullInputErrorMessage = 'Input must not be null.';
    test(`should fail with ${nullInputErrorMessage}`, async () => {
      const expectedError = { message: nullInputErrorMessage };
      await fail(applicationPortalApiAsAdminUser.createApplicationRaw({}), 400, expectedError);
      await fail(
        applicationPortalApiAsAdminUser.updateApplicationByApplicationIdRaw({
          applicationId: createApplication.id,
        }),
        400,
        expectedError,
      );
      await fail(
        applicationPortalApiAsAdminUser.updateApplicationNotificationsRaw({
          applicationId: createApplication.id,
        }),
        400,
        expectedError,
      );
      await fail(
        applicationPortalApiAsAdminUser.createApplicationMemberRaw({
          applicationId: createApplication.id,
        }),
        400,
        expectedError,
      );
      await fail(
        applicationPortalApiAsAdminUser.transferMemberOwnershipRaw({
          applicationId: createApplication.id,
        }),
        400,
        expectedError,
      );
      await fail(
        applicationPortalApiAsAdminUser.updateApplicationMemberByApplicationIdAndMemberIdRaw({
          applicationId: createApplication.id,
          memberId: 'MEMBER',
        }),
        400,
        expectedError,
      );
    });

    const applicationIdErrorMessage = `'applicationId' is not the same that the application in payload`;
    test(`should fail with ${applicationIdErrorMessage}`, async () => {
      await fail(
        applicationPortalApiAsAdminUser.updateApplicationByApplicationIdRaw({
          applicationId: createApplication.id,
          application: {
            id: 'APPLICATION',
          },
        }),
        400,
        { message: applicationIdErrorMessage },
      );
    });

    test('should not renew application secret', async () => {
      const expectedError = { message: `Client secret for application [${createApplication.name}] can not be renew.` };
      await fail(applicationPortalApiAsAdminUser.renewApplicationSecretRaw({ applicationId: createApplication.id }), 400, expectedError);
    });

    test('export should fail with wrong parameters', async () => {
      await fail(
        applicationPortalApiAsAdminUser.exportApplicationLogsByApplicationIdRaw({
          applicationId: createApplication.id,
          from: -1,
        }),
        400,
      );
      await fail(
        applicationPortalApiAsAdminUser.exportApplicationLogsByApplicationIdRaw({
          applicationId: createApplication.id,
          to: -1,
        }),
        400,
      );
      await fail(
        applicationPortalApiAsAdminUser.exportApplicationLogsByApplicationIdRaw({
          applicationId: createApplication.id,
          from: 0,
          to: 0,
        }),
        400,
      );
    });

    test('analytics should fail with wrong parameters', async () => {
      await fail(applicationPortalApiAsAdminUser.getApplicationAnalyticsRaw({ applicationId: createApplication.id }), 400);
      await fail(
        applicationPortalApiAsAdminUser.getApplicationAnalyticsRaw({
          applicationId: createApplication.id,
          type: GetApplicationAnalyticsTypeEnum.GROUPBY,
          from: -1,
        }),
        400,
      );
      await fail(
        applicationPortalApiAsAdminUser.getApplicationAnalyticsRaw({
          applicationId: createApplication.id,
          type: GetApplicationAnalyticsTypeEnum.GROUPBY,
          to: -1,
        }),
        400,
      );
      await fail(
        applicationPortalApiAsAdminUser.getApplicationAnalyticsRaw({
          applicationId: createApplication.id,
          type: GetApplicationAnalyticsTypeEnum.GROUPBY,
          from: 0,
          to: 0,
        }),
        400,
      );
      await fail(
        applicationPortalApiAsAdminUser.getApplicationAnalyticsRaw({
          applicationId: createApplication.id,
          type: GetApplicationAnalyticsTypeEnum.GROUPBY,
          from: 0,
          to: 1,
          interval: -1,
        }),
        400,
      );
      await fail(
        applicationPortalApiAsAdminUser.getApplicationAnalyticsRaw({
          applicationId: createApplication.id,
          type: GetApplicationAnalyticsTypeEnum.GROUPBY,
          from: 0,
          to: 1,
          interval: 100,
        }),
        400,
      );
    });

    afterAll(async () => {
      await applicationsManagementApiAsAdmin.deleteApplication({ orgId, envId, application: createApplication.id });

      await configurationManagementApiAsAdmin.deleteNotificationSettings({
        orgId,
        envId,
        notificationId: notificationSettings.id,
      });
    });
  });

  describe('403', () => {
    let userEnvRole: RoleEntity;

    beforeAll(async () => {
      userEnvRole = await configurationManagementApiAsAdmin.getRole({
        orgId,
        scope: RoleScope.ENVIRONMENT,
        role: 'USER',
      });
      const updateRoleEntity = UpdateRoleEntityFromJSON(userEnvRole);
      updateRoleEntity.permissions.APPLICATION = [];
      await configurationManagementApiAsAdmin.updateRole({
        orgId,
        scope: RoleScope.ENVIRONMENT,
        role: 'USER',
        updateRoleEntity,
      });
    });

    test('should forbidden with empty APPLICATION permission', async () => {
      const applicationId = createApplication.id;
      await Promise.all(
        [
          applicationPortalApiAsSimpleUser.getApplicationsRaw({}),
          applicationPortalApiAsSimpleUser.createApplicationRaw({}),
          applicationPortalApiAsSimpleUser.getApplicationByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.updateApplicationByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.deleteApplicationByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.getApplicationPictureByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.getNotificationsByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.updateApplicationNotificationsRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.getMembersByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.createApplicationMemberRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.getApplicationMemberByApplicationIdAndMemberIdRaw({
            applicationId,
            memberId,
          }),
          applicationPortalApiAsSimpleUser.updateApplicationMemberByApplicationIdAndMemberIdRaw({
            applicationId,
            memberId,
          }),
          applicationPortalApiAsSimpleUser.deleteApplicationMemberRaw({ applicationId, memberId }),
          applicationPortalApiAsSimpleUser.getApplicationAnalyticsRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.getApplicationLogsRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.exportApplicationLogsByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.getApplicationLogsRaw({ applicationId }),
          applicationPortalApiAsSimpleUser.getApplicationLogByApplicationIdAndLogIdRaw({
            applicationId,
            logId,
          }),
          applicationPortalApiAsSimpleUser.renewApplicationSecretRaw({ applicationId }),
        ].map((p: Promise<ApiResponse<unknown>>) => forbidden(p)),
      );
    });

    afterAll(async () => {
      const updateRoleEntity = UpdateRoleEntityFromJSON(userEnvRole);
      updateRoleEntity.permissions.APPLICATION = ['C', 'R', 'U', 'D'];
      await configurationManagementApiAsAdmin.updateRole({
        orgId,
        scope: RoleScope.ENVIRONMENT,
        role: 'USER',
        updateRoleEntity,
      });
    });
  });

  describe('404', () => {
    test('should return application not found', async () => {
      const applicationId = 'application';
      await Promise.all(
        [
          applicationPortalApiAsAdminUser.getApplicationByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsAdminUser.updateApplicationByApplicationIdRaw({ applicationId, application: { id: applicationId } }),
          applicationPortalApiAsAdminUser.deleteApplicationByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsAdminUser.getApplicationPictureByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsAdminUser.getNotificationsByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsAdminUser.updateApplicationNotificationsRaw({
            applicationId,
            notificationInput: {
              hooks: [],
            },
          }),
          applicationPortalApiAsAdminUser.getMembersByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsAdminUser.createApplicationMemberRaw({ applicationId, memberInput: {} }),
          applicationPortalApiAsAdminUser.transferMemberOwnershipRaw({ applicationId, transferOwnershipInput: {} }),
          applicationPortalApiAsAdminUser.getApplicationAnalyticsRaw({ applicationId }),
          applicationPortalApiAsAdminUser.getApplicationLogsRaw({ applicationId }),
          applicationPortalApiAsAdminUser.exportApplicationLogsByApplicationIdRaw({ applicationId }),
          applicationPortalApiAsAdminUser.getApplicationLogsRaw({ applicationId }),
          applicationPortalApiAsAdminUser.renewApplicationSecretRaw({ applicationId }),
        ].map((p: Promise<ApiResponse<unknown>>) => notFound(p, { message: `Application [${applicationId}] can not be found.` })),
      );
    });

    test('should return member not found', async () => {
      const applicationId = createApplication.id;
      await Promise.all(
        [
          applicationPortalApiAsAdminUser.getApplicationMemberByApplicationIdAndMemberIdRaw({
            applicationId,
            memberId,
          }),
          applicationPortalApiAsAdminUser.updateApplicationMemberByApplicationIdAndMemberIdRaw({
            applicationId,
            memberId,
            memberInput: {},
          }),
          applicationPortalApiAsAdminUser.deleteApplicationMemberRaw({ applicationId, memberId }),
        ].map((p: Promise<ApiResponse<unknown>>) => notFound(p, { message: `User [${memberId}] can not be found.` })),
      );
    });

    test('should return log not found', async () => {
      const applicationId = createApplication.id;
      await Promise.all(
        [
          applicationPortalApiAsAdminUser.getApplicationLogByApplicationIdAndLogIdRaw({
            applicationId,
            logId,
          }),
        ].map((p) => notFound(p, { message: `Log [${logId}] can not be found.` })),
      );
    });
  });

  afterAll(async () => {
    await applicationsManagementApiAsAdmin.deleteApplication({ orgId, envId, application: createApplication.id });
  });
});

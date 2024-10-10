import { API_USER, forManagementAsAdminUser } from '@gravitee/utils/configuration';
import { RolesApi } from '../../../../../lib/management-webclient-sdk/src/lib/apis/RolesApi';
import { UsersApi } from '../../../../../lib/management-webclient-sdk/src/lib/apis/UsersApi';
import { RoleScope, UserEntity } from '../../../../../lib/management-webclient-sdk/src/lib/models';
import { describe, expect } from '@jest/globals';
import { noContent, succeed } from '@lib/jest-utils';
import { ConfigurationApi } from '../../../../../lib/management-webclient-sdk/src/lib/apis/ConfigurationApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const managementRolesResourceAsAdminUser = new RolesApi(forManagementAsAdminUser());
const managementUserResourceAsAdminUser = new UsersApi(forManagementAsAdminUser());
const configurationResourceAsAdmin = new ConfigurationApi(forManagementAsAdminUser());

describe('Notification settings tests', () => {
  let apiUser: UserEntity;

  describe('Create', () => {
    it('should create a notification setting', async () => {
      const users = await managementUserResourceAsAdminUser.getAllUsers({ orgId, envId });
      apiUser = users.data.find((user) => user.displayName === API_USER.username);

      const userOrganizationRole = await succeed(
        managementRolesResourceAsAdminUser.getRole1Raw({
          scope: RoleScope.ORGANIZATION,
          role: 'USER',
          envId,
          orgId,
        }),
      );

      await succeed(
        managementUserResourceAsAdminUser.updateUserRolesRaw({
          userId: apiUser.id,
          userReferenceRoleEntity: {
            referenceId: orgId,
            referenceType: 'ORGANIZATION',
            roles: [userOrganizationRole.id],
            user: apiUser.id,
          },
          orgId,
          envId,
        }),
      );

      const adminEnvironmentRole = await succeed(
        managementRolesResourceAsAdminUser.getRole1Raw({
          scope: RoleScope.ENVIRONMENT,
          role: 'ADMIN',
          envId,
          orgId,
        }),
      );

      await succeed(
        managementUserResourceAsAdminUser.updateUserRolesRaw({
          userId: apiUser.id,
          userReferenceRoleEntity: {
            referenceId: envId,
            referenceType: 'ENVIRONMENT',
            roles: [adminEnvironmentRole.id],
            user: apiUser.id,
          },
          orgId,
          envId,
        }),
      );

      const notification = JSON.parse(
        await succeed(
          configurationResourceAsAdmin.createPortalNotificationSetting1Raw({
            envId,
            orgId,
            genericNotificationConfigEntity: {
              name: 'test',
              referenceType: 'PORTAL',
              referenceId: 'DEFAULT',
              notifier: 'default-email',
              config_type: 'GENERIC',
            },
          }),
        ),
      );

      expect(notification).toBeTruthy();

      await noContent(
        configurationResourceAsAdmin.deleteNotificationSettings1Raw({
          notificationId: notification.id,
          envId,
          orgId,
        }),
      );
    });
  });
});

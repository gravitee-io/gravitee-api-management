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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { OrgSettingsGeneralComponent } from './console/org-settings-general.component';
import { OrgSettingsUsersComponent } from './users/org-settings-users.component';
import { OrgSettingsNewUserComponent } from './user/new/org-settings-new-user.component';
import { OrgSettingsIdentityProvidersComponent } from './identity-providers/org-settings-identity-providers.component';
import { OrgSettingsIdentityProviderComponent } from './identity-provider/org-settings-identity-provider.component';
import { OrgSettingsNotificationTemplatesComponent } from './notification-templates/org-settings-notification-templates.component';
import { OrgSettingsCockpitComponent } from './cockpit/org-settings-cockpit.component';
import { OrgSettingsNotificationTemplateComponent } from './notification-templates/org-settings-notification-template.component';
import { OrgSettingsUserDetailComponent } from './user/detail/org-settings-user-detail.component';
import { OrgSettingsPlatformPoliciesComponent } from './policies/org-settings-platform-policies.component';
import { OrgSettingsTenantsComponent } from './tenants/org-settings-tenants.component';
import { OrgSettingsRolesComponent } from './roles/org-settings-roles.component';
import { OrgSettingsTagsComponent } from './tags/org-settings-tags.component';
import { OrgSettingsRoleMembersComponent } from './roles/org-settings-role-members.component';
import { OrgSettingsRoleComponent } from './roles/role/org-settings-role.component';
import { OrgSettingsAuditComponent } from './audit/org-settings-audit.component';
import { OrgNavigationComponent } from './navigation/org-navigation.component';
import { AsOrganizationPermissionGuard } from './as-organization-permission.guard';
import { OrganizationSettingsModule } from './organization-settings.module';

import { AsLicenseGuard } from '../../shared/components/gio-license/as-license.guard';

const organizationRoutes: Routes = [
  {
    path: '',
    component: OrgNavigationComponent,
    canActivate: [AsOrganizationPermissionGuard],
    canActivateChild: [AsOrganizationPermissionGuard, AsLicenseGuard],
    children: [
      {
        path: 'settings',
        component: OrgSettingsGeneralComponent,
        data: {
          docs: {
            page: 'organization-configuration-console',
          },
          perms: {
            only: ['organization-settings-r'],
          },
        },
      },
      {
        path: 'identities/new',
        component: OrgSettingsIdentityProviderComponent,
        data: {
          docs: {
            page: 'organization-configuration-identityproviders',
          },
          perms: {
            only: ['organization-identity_provider-c'],
          },
        },
      },
      {
        path: 'identities/:id',
        component: OrgSettingsIdentityProviderComponent,
        data: {
          docs: {
            page: 'organization-configuration-identityproviders',
          },
          perms: {
            only: ['organization-identity_provider-r', 'organization-identity_provider-u', 'organization-identity_provider-d'],
          },
        },
      },
      {
        path: 'identities',
        component: OrgSettingsIdentityProvidersComponent,
        data: {
          docs: {
            page: 'organization-configuration-identityproviders',
          },
          perms: {
            only: ['organization-identity_provider-r'],
          },
        },
      },
      {
        path: 'users/new',
        component: OrgSettingsNewUserComponent,
        data: {
          docs: {
            page: 'organization-configuration-create-user',
          },
          perms: {
            only: ['organization-user-c'],
          },
        },
      },
      {
        path: 'users/:userId',
        component: OrgSettingsUserDetailComponent,
        data: {
          docs: {
            page: 'organization-configuration-user',
          },
          perms: {
            only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
          },
        },
      },
      {
        path: 'users',
        component: OrgSettingsUsersComponent,
        data: {
          docs: {
            page: 'organization-configuration-users',
          },
          perms: {
            only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
          },
        },
      },
      {
        path: 'role/:roleScope/:role/members',
        component: OrgSettingsRoleMembersComponent,
        data: {
          docs: {
            page: 'organization-configuration-roles',
          },
          perms: {
            only: ['organization-role-u'],
          },
        },
      },
      {
        path: 'role/:roleScope/:role',
        component: OrgSettingsRoleComponent,
        data: {
          docs: {
            page: 'organization-configuration-roles',
          },
          perms: {
            only: ['organization-role-u'],
          },
        },
      },
      {
        path: 'role/:roleScope',
        component: OrgSettingsRoleComponent,
        data: {
          requireLicense: {
            license: { feature: 'apim-custom-roles' },
            redirect: '/_organization/roles',
          },
          docs: {
            page: 'organization-configuration-roles',
          },
          perms: {
            only: ['organization-role-u'],
          },
        },
      },
      {
        path: 'roles',
        component: OrgSettingsRolesComponent,
        data: {
          docs: {
            page: 'organization-configuration-roles',
          },
          perms: {
            only: ['organization-role-r'],
          },
        },
      },
      {
        path: 'tags',
        component: OrgSettingsTagsComponent,
        data: {
          docs: {
            page: 'management-configuration-sharding-tags',
          },
          perms: {
            only: ['organization-tag-r'],
          },
        },
      },
      {
        path: 'tenants',
        component: OrgSettingsTenantsComponent,
        data: {
          docs: {
            page: 'management-configuration-tenants',
          },
          perms: {
            only: ['organization-tenant-r'],
          },
        },
      },
      {
        path: 'policies',
        component: OrgSettingsPlatformPoliciesComponent,
        data: {
          docs: {
            page: 'management-configuration-policies',
          },
          perms: {
            only: ['organization-policies-r'],
          },
        },
      },
      {
        path: 'notification-templates/:scope/:hook',
        component: OrgSettingsNotificationTemplateComponent,
        data: {
          docs: {
            page: 'organization-configuration-notification-template',
          },
          perms: {
            only: ['organization-notification_templates-r'],
          },
        },
      },
      {
        path: 'notification-templates',
        component: OrgSettingsNotificationTemplatesComponent,
        data: {
          docs: {
            page: 'organization-configuration-notification-templates',
          },
          perms: {
            only: ['organization-notification_templates-r'],
          },
        },
      },
      {
        path: 'audit',
        component: OrgSettingsAuditComponent,
        data: {
          requireLicense: {
            license: { feature: 'apim-audit-trail' },
            redirect: '/_organization/settings',
          },
          docs: {
            page: 'management-audit',
          },
          perms: {
            only: ['organization-audit-r'],
          },
        },
      },
      {
        path: 'cockpit',
        component: OrgSettingsCockpitComponent,
        data: {
          docs: {
            page: 'organization-configuration-cockpit',
          },
          perms: {
            only: ['organization-installation-r'],
          },
        },
      },

      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'settings',
      },
    ],
  },
];

@NgModule({
  imports: [OrganizationSettingsModule, RouterModule.forChild(organizationRoutes)],
  exports: [RouterModule],
})
export class OrganizationSettingsRoutingModule {}

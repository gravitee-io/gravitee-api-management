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
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatBadgeModule } from '@angular/material/badge';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatRadioModule } from '@angular/material/radio';
import { MatNativeDateModule, MatRippleModule } from '@angular/material/core';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatDatepickerModule } from '@angular/material/datepicker';
import {
  GioAvatarModule,
  GioBannerModule,
  GioFormTagsInputModule,
  GioSaveBarModule,
  GioFormSlideToggleModule,
  GioFormFocusInvalidModule,
  GioConfirmDialogModule,
  GioFormJsonSchemaModule,
  GioMenuModule,
  GioSubmenuModule,
  GioClipboardModule,
  GioLicenseModule,
} from '@gravitee/ui-particles-angular';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterModule, Routes } from '@angular/router';

import { OrgSettingsGeneralComponent } from './console/org-settings-general.component';
import { OrgSettingsUsersComponent } from './users/org-settings-users.component';
import { OrgSettingsNewUserComponent } from './user/new/org-settings-new-user.component';
import { OrgSettingsIdentityProvidersComponent } from './identity-providers/org-settings-identity-providers.component';
import { OrgSettingsIdentityProviderComponent } from './identity-provider/org-settings-identity-provider.component';
import { OrgSettingsIdentityProviderGithubComponent } from './identity-provider/org-settings-identity-provider-github/org-settings-identity-provider-github.component';
import { OrgSettingsIdentityProviderGoogleComponent } from './identity-provider/org-settings-identity-provider-google/org-settings-identity-provider-google.component';
import { OrgSettingsIdentityProviderGraviteeioAmComponent } from './identity-provider/org-settings-identity-provider-graviteeio-am/org-settings-identity-provider-graviteeio-am.component';
import { OrgSettingsIdentityProviderOidcComponent } from './identity-provider/org-settings-identity-provider-oidc/org-settings-identity-provider-oidc.component';
import { OrgSettingsNotificationTemplatesComponent } from './notification-templates/org-settings-notification-templates.component';
import { OrgSettingsCockpitComponent } from './cockpit/org-settings-cockpit.component';
import { OrgSettingsNotificationTemplateComponent } from './notification-templates/org-settings-notification-template.component';
import { OrgSettingsUserDetailComponent } from './user/detail/org-settings-user-detail.component';
import { OrgSettingsPlatformPoliciesComponent } from './policies/org-settings-platform-policies.component';
import { OrgSettingsTenantsComponent } from './tenants/org-settings-tenants.component';
import { OrgSettingAddTenantComponent } from './tenants/org-settings-add-tenant.component';
import { OrgSettingsRolesComponent } from './roles/org-settings-roles.component';
import { OrgSettingsTagsComponent } from './tags/org-settings-tags.component';
import { OrgSettingsRoleMembersComponent } from './roles/org-settings-role-members.component';
import { OrgSettingAddTagDialogComponent } from './tags/org-settings-add-tag-dialog.component';
import { OrgSettingAddMappingDialogComponent } from './tags/org-settings-add-mapping-dialog.component';
import { OrgSettingsRoleComponent } from './roles/role/org-settings-role.component';
import { OrgSettingsUserDetailAddGroupDialogComponent } from './user/detail/org-settings-user-detail-add-group-dialog.component';
import { OrgSettingsUserGenerateTokenComponent } from './user/detail/tokens/org-settings-user-generate-token.component';
import { OrgSettingsAuditComponent } from './audit/org-settings-audit.component';
import { OrgSettingsPlatformPoliciesConfigComponent } from './policies/config/org-settings-platform-policies-config.component';
import { OrgNavigationComponent } from './navigation/org-navigation.component';
import { OrgSettingsPlatformPoliciesStudioModule } from './policies/studio/org-settings-platform-policies-studio.module';
import { AsOrganizationPermissionGuard } from './as-organization-permission.guard';

import { GioTableOfContentsModule } from '../../shared/components/gio-table-of-contents/gio-table-of-contents.module';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { GioFormCardGroupModule } from '../../shared/components/gio-form-card-group/gio-form-card-group.module';
import { GioFormColorInputModule } from '../../shared/components/gio-form-color-input/gio-form-color-input.module';
import { GioGoBackButtonModule } from '../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioUsersSelectorModule } from '../../shared/components/gio-users-selector/gio-users-selector.module';
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
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatDividerModule,
    MatTooltipModule,
    MatChipsModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTableModule,
    MatBadgeModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatDividerModule,
    MatRadioModule,
    MatRippleModule,
    MatListModule,
    MatProgressBarModule,
    MatSortModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatTabsModule,

    GioPermissionModule,
    GioConfirmDialogModule,
    GioAvatarModule,
    GioTableOfContentsModule,
    GioFormSlideToggleModule,
    GioFormCardGroupModule,
    GioFormTagsInputModule,
    GioFormColorInputModule,
    GioGoBackButtonModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    GioBannerModule,
    GioClipboardModule,
    GioTableWrapperModule,
    GioUsersSelectorModule,
    GioFormJsonSchemaModule,
    GioLicenseModule,
    GioMenuModule,
    GioSubmenuModule,
    OrgSettingsPlatformPoliciesStudioModule,

    RouterModule.forChild(organizationRoutes),
  ],
  declarations: [
    OrgNavigationComponent,
    OrgSettingsGeneralComponent,
    OrgSettingsUsersComponent,
    OrgSettingsNewUserComponent,
    OrgSettingsUserGenerateTokenComponent,
    OrgSettingsUserDetailComponent,
    OrgSettingsIdentityProvidersComponent,
    OrgSettingsIdentityProviderComponent,
    OrgSettingsIdentityProviderGoogleComponent,
    OrgSettingsIdentityProviderOidcComponent,
    OrgSettingsIdentityProviderGraviteeioAmComponent,
    OrgSettingsIdentityProviderGithubComponent,
    OrgSettingsNotificationTemplatesComponent,
    OrgSettingsNotificationTemplateComponent,
    OrgSettingsCockpitComponent,
    OrgSettingsPlatformPoliciesComponent,
    OrgSettingsPlatformPoliciesConfigComponent,
    OrgSettingsTenantsComponent,
    OrgSettingAddTenantComponent,
    OrgSettingsRolesComponent,
    OrgSettingsTagsComponent,
    OrgSettingsRoleMembersComponent,
    OrgSettingAddTagDialogComponent,
    OrgSettingAddMappingDialogComponent,
    OrgSettingsRoleComponent,
    OrgSettingsUserDetailAddGroupDialogComponent,
    OrgSettingsAuditComponent,
  ],
})
export class OrganizationSettingsModule {}

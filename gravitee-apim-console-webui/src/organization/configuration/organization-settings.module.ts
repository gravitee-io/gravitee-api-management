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
} from '@gravitee/ui-particles-angular';
import { MatTabsModule } from '@angular/material/tabs';
import { Ng2StateDeclaration, UIRouterModule } from '@uirouter/angular';

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
import { OrgSettingsPlatformPoliciesStudioComponent } from './policies/studio/org-settings-platform-policies-studio.component';
import { OrgSettingsPlatformPoliciesConfigComponent } from './policies/config/org-settings-platform-policies-config.component';
import { OrgNavigationComponent } from './navigation/org-navigation.component';

import { GioTableOfContentsModule } from '../../shared/components/gio-table-of-contents/gio-table-of-contents.module';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { GioFormCardGroupModule } from '../../shared/components/gio-form-card-group/gio-form-card-group.module';
import { GioFormColorInputModule } from '../../shared/components/gio-form-color-input/gio-form-color-input.module';
import { GioGoBackButtonModule } from '../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioUsersSelectorModule } from '../../shared/components/gio-users-selector/gio-users-selector.module';
import { GioLicenseModule } from '../../shared/components/gio-license/gio-license.module';

const states: Ng2StateDeclaration[] = [
  {
    name: 'organization',
    url: '/organization',
    abstract: true,
    component: OrgNavigationComponent,
  },
  {
    name: 'organization.settings',
    url: '/settings',
    component: OrgSettingsGeneralComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-console',
      },
      perms: {
        only: ['organization-settings-r'],
      },
    },
  },
  {
    name: 'organization.identities',
    url: '/identities',
    component: OrgSettingsIdentityProvidersComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-identityproviders',
      },
      perms: {
        only: ['organization-identity_provider-r'],
      },
    },
  },
  {
    name: 'organization.identity-edit',
    url: '/identities/:id',
    component: OrgSettingsIdentityProviderComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-identityproviders',
      },
      perms: {
        only: ['organization-identity_provider-r', 'organization-identity_provider-u', 'organization-identity_provider-d'],
      },
    },
  },
  {
    name: 'organization.identity-new',
    url: '/identities/new',
    component: OrgSettingsIdentityProviderComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-identityproviders',
      },
      perms: {
        only: ['organization-identity_provider-c'],
      },
    },
  },
  {
    name: 'organization.users',
    url: '/users?q&page',
    component: OrgSettingsUsersComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-users',
      },
      perms: {
        only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
      },
    },
    params: {
      page: {
        value: '1',
        dynamic: true,
      },
      q: {
        dynamic: true,
      },
    },
  },
  {
    name: 'organization.user-new',
    url: '/users/new',
    component: OrgSettingsNewUserComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-create-user',
      },
      perms: {
        only: ['organization-user-c'],
      },
    },
  },
  {
    name: 'organization.user-edit',
    url: '/users/:userId',
    component: OrgSettingsUserDetailComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-user',
      },
      perms: {
        only: ['organization-user-c', 'organization-user-r', 'organization-user-u', 'organization-user-d'],
      },
    },
  },
  {
    name: 'organization.roles',
    url: '/roles',
    component: OrgSettingsRolesComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-roles',
      },
      perms: {
        only: ['organization-role-r'],
      },
    },
  },
  {
    name: 'organization.role-new',
    url: '/role/:roleScope',
    component: OrgSettingsRoleComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-roles',
      },
      perms: {
        only: ['organization-role-u'],
      },
    },
  },
  {
    name: 'organization.role-edit',
    url: '/role/:roleScope/:role',
    component: OrgSettingsRoleComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-roles',
      },
      perms: {
        only: ['organization-role-u'],
      },
    },
  },
  {
    name: 'organization.role-members',
    url: '/role/:roleScope/:role/members',
    component: OrgSettingsRoleMembersComponent,
    data: {
      useAngularMaterial: true,
      docs: {
        page: 'organization-configuration-roles',
      },
      perms: {
        only: ['organization-role-u'],
      },
    },
  },
  {
    name: 'organization.tags',
    url: '/tags',
    component: OrgSettingsTagsComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'management-configuration-sharding-tags',
      },
      perms: {
        only: ['organization-tag-r'],
      },
    },
  },
  {
    name: 'organization.tenants',
    url: '/tenants',
    component: OrgSettingsTenantsComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'management-configuration-tenants',
      },
      perms: {
        only: ['organization-tenant-r'],
      },
    },
  },
  {
    name: 'organization.policies',
    url: '/policies',
    component: OrgSettingsPlatformPoliciesComponent,
    data: {
      useAngularMaterial: true,
      docs: {
        page: 'management-configuration-policies',
      },
      perms: {
        only: ['organization-policies-r'],
      },
    },
  },
  {
    name: 'organization.notificationTemplates',
    url: '/notification-templates',
    component: OrgSettingsNotificationTemplatesComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-notification-templates',
      },
      perms: {
        only: ['organization-notification_templates-r'],
      },
    },
  },
  {
    name: 'organization.notificationTemplate',
    url: '/notification-templates/:scope/:hook',
    component: OrgSettingsNotificationTemplateComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-notification-template',
      },
      perms: {
        only: ['organization-notification_templates-r'],
      },
    },
  },
  {
    name: 'organization.audit',
    url: '/audit',
    component: OrgSettingsAuditComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'management-audit',
      },
      perms: {
        only: ['organization-audit-r'],
      },
    },
  },
  {
    name: 'organization.cockpit',
    url: '/cockpit',
    component: OrgSettingsCockpitComponent,
    data: {
      useAngularMaterial: true,
      menu: null,
      docs: {
        page: 'organization-configuration-cockpit',
      },
      perms: {
        only: ['organization-installation-r'],
      },
    },
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

    UIRouterModule.forChild({ states }),
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
    OrgSettingsPlatformPoliciesStudioComponent,
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
  exports: [OrgSettingsGeneralComponent, OrgSettingsUsersComponent],
})
export class OrganizationSettingsModule {}

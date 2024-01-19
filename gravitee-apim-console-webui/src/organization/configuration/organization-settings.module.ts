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
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacyCheckboxModule as MatCheckboxModule } from '@angular/material/legacy-checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatLegacyChipsModule as MatChipsModule } from '@angular/material/legacy-chips';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacyAutocompleteModule as MatAutocompleteModule } from '@angular/material/legacy-autocomplete';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatBadgeModule } from '@angular/material/badge';
import { MatLegacyPaginatorModule as MatPaginatorModule } from '@angular/material/legacy-paginator';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacyRadioModule as MatRadioModule } from '@angular/material/legacy-radio';
import { MatNativeDateModule, MatRippleModule } from '@angular/material/core';
import { MatLegacyListModule as MatListModule } from '@angular/material/legacy-list';
import { MatLegacyProgressBarModule as MatProgressBarModule } from '@angular/material/legacy-progress-bar';
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
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';
import { RouterModule } from '@angular/router';

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

import { GioTableOfContentsModule } from '../../shared/components/gio-table-of-contents/gio-table-of-contents.module';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { GioFormCardGroupModule } from '../../shared/components/gio-form-card-group/gio-form-card-group.module';
import { GioFormColorInputModule } from '../../shared/components/gio-form-color-input/gio-form-color-input.module';
import { GioGoBackButtonModule } from '../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioUsersSelectorModule } from '../../shared/components/gio-users-selector/gio-users-selector.module';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

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
    MatLegacyDialogModule,
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

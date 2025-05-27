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
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatRadioModule } from '@angular/material/radio';
import { ErrorStateMatcher, ShowOnDirtyErrorStateMatcher } from '@angular/material/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSortModule } from '@angular/material/sort';
import {
  GioBannerModule,
  GioBreadcrumbModule,
  GioClipboardModule,
  GioFormSelectionInlineModule,
  GioLoaderModule,
  GioSubmenuModule,
  GioSaveBarModule,
  GioLicenseModule,
  GioFormSlideToggleModule,
  GioCardEmptyStateModule,
  GioIconsModule,
} from '@gravitee/ui-particles-angular';

import { MapProviderNamePipe } from './pipes/map-provider-name.pipe';
import { IntegrationsComponent } from './integrations.component';
import { IntegrationsRoutingModule } from './integrations-routing.module';
import { IntegrationsNavigationComponent } from './integrations-navigation/integrations-navigation.component';
import { IntegrationConfigurationComponent } from './integration-configuration/integration-configuration.component';
import { IntegrationStatusComponent } from './components/integration-status/integration-status.component';
import { IntegrationGeneralConfigurationComponent } from './integration-configuration/general/integration-general-configuration.component';
import { IntegrationUserPermissionsComponent } from './integration-configuration/user-permissions/integration-user-permissions.component';
import { IntegrationUserGroupModule } from './user-group-access/integration-user-group.module';
import { CreateIntegrationModule } from './create-integration/create-integration.module';
import { IntegrationAgentModule } from './integration-agent/integration-agent.module';
import { DiscoveryPreviewModule } from './discovery-preview/discovery-preview.module';
import { IntegrationOverviewModule } from './integration-overview/integration-overview.module';
import { IsApiIntegration } from './pipes/is-api-integration.pipe';
import { IsA2aIntegration } from './pipes/is-a2a-integration.pipe';

import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [
    IntegrationsComponent,
    IntegrationsNavigationComponent,
    IntegrationConfigurationComponent,
    IntegrationGeneralConfigurationComponent,
    IntegrationUserPermissionsComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatTabsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatSlideToggleModule,
    MatIconModule,
    MatTooltipModule,

    GioBreadcrumbModule,
    GioSubmenuModule,
    GioBannerModule,
    GioClipboardModule,
    GioLoaderModule,
    GioPermissionModule,
    GioFormSelectionInlineModule,
    GioSaveBarModule,
    GioLicenseModule,
    GioFormSlideToggleModule,
    GioTableWrapperModule,
    GioIconsModule,
    GioCardEmptyStateModule,

    IntegrationsRoutingModule,
    IntegrationUserGroupModule,
    CreateIntegrationModule,
    DiscoveryPreviewModule,
    IntegrationAgentModule,
    IntegrationOverviewModule,
    IntegrationStatusComponent,
    MapProviderNamePipe,
    IsApiIntegration,
    IsA2aIntegration,
  ],
  providers: [{ provide: ErrorStateMatcher, useClass: ShowOnDirtyErrorStateMatcher }],
})
export class IntegrationsModule {}

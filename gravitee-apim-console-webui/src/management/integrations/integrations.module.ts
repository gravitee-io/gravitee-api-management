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
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatTooltip } from '@angular/material/tooltip';
import { MatCell, MatCellDef, MatColumnDef, MatHeaderCell, MatTableModule } from '@angular/material/table';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { CdkAccordion, CdkAccordionItem } from '@angular/cdk/accordion';
import { MatStep, MatStepLabel, MatStepper, MatStepperIcon, MatStepperNext, MatStepperPrevious } from '@angular/material/stepper';
import { ErrorStateMatcher, ShowOnDirtyErrorStateMatcher } from '@angular/material/core';
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
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';
import { MatSortModule } from '@angular/material/sort';

import { MapProviderNamePipe } from './pipes/map-provider-name.pipe';
import { IntegrationsComponent } from './integrations.component';
import { CreateIntegrationComponent } from './create-integration/create-integration.component';
import { IntegrationsRoutingModule } from './integrations-routing.module';
import { IntegrationOverviewComponent } from './integration-overview/integration-overview.component';
import { IntegrationsNavigationComponent } from './integrations-navigation/integrations-navigation.component';
import { IntegrationConfigurationComponent } from './integration-configuration/integration-configuration.component';
import { IntegrationAgentComponent } from './integration-agent/integration-agent.component';
import { IntegrationStatusComponent } from './components/integration-status/integration-status.component';
import { DiscoveryPreviewComponent } from './discovery-preview/discovery-preview.component';
import { IntegrationGeneralConfigurationComponent } from './integration-configuration/general/integration-general-configuration.component';
import { IntegrationUserPermissionsComponent } from './integration-configuration/user-permissions/integration-user-permissions.component';
import { IntegrationUserGroupModule } from './user-group-access/integration-user-group.module';

import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [
    IntegrationsComponent,
    CreateIntegrationComponent,
    IntegrationOverviewComponent,
    IntegrationsNavigationComponent,
    IntegrationConfigurationComponent,
    IntegrationAgentComponent,
    IntegrationStatusComponent,
    DiscoveryPreviewComponent,
    IntegrationGeneralConfigurationComponent,
    IntegrationUserPermissionsComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IntegrationsRoutingModule,
    IntegrationUserGroupModule,

    MatCard,
    MatCardTitle,
    MatCardHeader,
    MatCardContent,
    MatIcon,
    MatButton,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell,
    MatIconButton,
    MatTooltip,
    MatTableModule,
    MatRadioGroup,
    MatRadioButton,

    MatStepper,
    MatStep,
    MatStepLabel,
    MatStepperPrevious,
    MatStepperNext,
    MatStepperIcon,

    MapProviderNamePipe,
    MatSlideToggle,
    MatTableModule,
    MatTabNavPanel,
    MatTabLink,
    MatTabNav,
    MatSortModule,

    CdkAccordion,
    CdkAccordionItem,

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
  ],
  providers: [{ provide: ErrorStateMatcher, useClass: ShowOnDirtyErrorStateMatcher }],
})
export class IntegrationsModule {}

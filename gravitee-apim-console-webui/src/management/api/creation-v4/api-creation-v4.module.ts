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
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  GioBannerModule,
  GioConfirmDialogModule,
  GioFormFocusInvalidModule,
  GioFormJsonSchemaModule,
  GioIconsModule,
} from '@gravitee/ui-particles-angular';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatListModule } from '@angular/material/list';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRadioModule } from '@angular/material/radio';
import { MatTableModule } from '@angular/material/table';
import { MatMenuModule } from '@angular/material/menu';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { RouterModule } from '@angular/router';

import { ApiCreationV4Component } from './api-creation-v4.component';
import { Step1ApiDetailsComponent } from './steps/step-1-api-details/step-1-api-details.component';
import { Step2Entrypoints1ListComponent } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.component';
import { Step5SummaryComponent } from './steps/step-5-summary/step-5-summary.component';
import { ApiCreationV4ConfirmationComponent } from './api-creation-v4-confirmation.component';
import { ApiCreationStepperMenuModule } from './components/api-creation-stepper-menu/api-creation-stepper-menu.module';
import { Step1MenuItemComponent } from './steps/step-1-menu-item/step-1-menu-item.component';
import { Step4Security1PlansListComponent } from './steps/step-4-security/step-4-security-1-plans-list.component';
import { Step2Entrypoints2ConfigComponent } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.component';
import { Step3Endpoints1ListComponent } from './steps/step-3-endpoints/step-3-endpoints-1-list.component';
import { Step3Endpoints2ConfigComponent } from './steps/step-3-endpoints/step-3-endpoints-2-config.component';
import { StepEntrypointMenuItemComponent } from './steps/step-connector-menu-item/step-entrypoint-menu-item.component';
import { StepEndpointMenuItemComponent } from './steps/step-connector-menu-item/step-endpoint-menu-item.component';
import { Step2Entrypoints0ArchitectureComponent } from './steps/step-2-entrypoints/step-2-entrypoints-0-architecture.component';
import { Step4MenuItemComponent } from './steps/step-4-menu-item/step-4-menu-item.component';
import { Step4Security1PlansAddComponent } from './steps/step-4-security/step-4-security-1-plans-add.component';
import { Step4Security1PlansComponent } from './steps/step-4-security/step-4-security-1-plans.component';

import { GioSelectionListModule } from '../../../shared/components/gio-selection-list-option/gio-selection-list.module';
import { GioFormListenersContextPathModule } from '../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.module';
import { GioFormListenersVirtualHostModule } from '../component/gio-form-listeners/gio-form-listeners-virtual-host/gio-form-listeners-virtual-host.module';
import { GioEntrypointsSelectionListModule } from '../component/gio-entrypoints-selection-list/gio-entrypoints-selection-list.module';
import { GioInformationDialogModule } from '../component/gio-information-dialog/gio-information-dialog.module';
import { ApiPlanFormModule } from '../component/plan/api-plan-form.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioFormQosModule } from '../component/gio-form-qos/gio-form-qos.module';
import { GioLicenseBannerModule } from '../../../shared/components/gio-license-banner/gio-license-banner.module';
import { GioFormListenersTcpHostsModule } from '../component/gio-form-listeners/gio-form-listeners-tcp-hosts/gio-form-listeners-tcp-hosts.module';
import { GioFormListenersKafkaHostPortComponent } from '../component/gio-form-listeners/gio-form-listeners-kafka/gio-form-listeners-kafka-host-port.component';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatCardModule,
    MatButtonModule,
    MatInputModule,
    MatCheckboxModule,
    MatRadioModule,
    MatListModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressBarModule,

    GioConfirmDialogModule,
    GioIconsModule,
    GioSelectionListModule,
    GioConfirmDialogModule,
    GioFormJsonSchemaModule,
    ApiCreationStepperMenuModule,
    GioFormQosModule,
    GioFormListenersContextPathModule,
    GioFormListenersKafkaHostPortComponent,
    GioFormListenersTcpHostsModule,
    GioFormListenersVirtualHostModule,
    GioEntrypointsSelectionListModule,
    GioInformationDialogModule,
    GioBannerModule,
    ApiPlanFormModule,
    GioFormFocusInvalidModule,
    GioPermissionModule,
    MatMenuModule,
    MatOptionModule,
    MatSelectModule,
    GioLicenseBannerModule,
    RouterModule,
  ],
  declarations: [
    ApiCreationV4Component,
    ApiCreationV4ConfirmationComponent,

    // Steps components
    Step1ApiDetailsComponent,
    Step1MenuItemComponent,
    Step2Entrypoints0ArchitectureComponent,
    Step2Entrypoints1ListComponent,
    StepEntrypointMenuItemComponent,
    Step2Entrypoints2ConfigComponent,
    Step3Endpoints1ListComponent,
    Step3Endpoints2ConfigComponent,
    StepEndpointMenuItemComponent,
    Step4MenuItemComponent,
    Step4Security1PlansComponent,
    Step4Security1PlansListComponent,
    Step4Security1PlansAddComponent,
    Step5SummaryComponent,
  ],
  exports: [ApiCreationV4Component],
})
export class ApiCreationV4Module {}

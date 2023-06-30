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
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioClipboardModule, GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_MOMENT_DATE_ADAPTER_OPTIONS, MatMomentDateModule } from '@angular/material-moment-adapter';

import { ApiPortalSubscriptionCreationDialogComponent } from './components/creation-dialog/api-portal-subscription-creation-dialog.component';
import { ApiPortalSubscriptionTransferDialogComponent } from './components/transfer-dialog/api-portal-subscription-transfer-dialog.component';
import { ApiPortalSubscriptionEditComponent } from './edit/api-portal-subscription-edit.component';
import { ApiPortalSubscriptionListComponent } from './list/api-portal-subscription-list.component';
import { ApiPortalSubscriptionChangeEndDateDialogComponent } from './components/change-end-date-dialog/api-portal-subscription-change-end-date-dialog.component';
import { ApiPortalSubscriptionValidateDialogComponent } from './components/validate-dialog/api-portal-subscription-validate-dialog.component';
import { ApiKeyValidationComponent } from './components/api-key-validation/api-key-validation.component';
import { ApiPortalSubscriptionRejectDialogComponent } from './components/reject-dialog/api-portal-subscription-reject-dialog.component';
import { ApiPortalSubscriptionRenewDialogComponent } from './components/renew-dialog/api-portal-subscription-renew-dialog.component';

import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [
    ApiPortalSubscriptionEditComponent,
    ApiPortalSubscriptionListComponent,
    ApiPortalSubscriptionChangeEndDateDialogComponent,
    ApiPortalSubscriptionCreationDialogComponent,
    ApiPortalSubscriptionTransferDialogComponent,
    ApiPortalSubscriptionRejectDialogComponent,
    ApiPortalSubscriptionRenewDialogComponent,
    ApiPortalSubscriptionValidateDialogComponent,

    ApiKeyValidationComponent,
  ],
  exports: [ApiPortalSubscriptionEditComponent, ApiPortalSubscriptionListComponent, ApiKeyValidationComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,

    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatMomentDateModule,
    MatOptionModule,
    MatSelectModule,
    MatSnackBarModule,
    MatRadioModule,
    MatTableModule,
    MatTooltipModule,

    GioClipboardModule,
    GioIconsModule,
    GioLoaderModule,
    GioPermissionModule,
    GioTableWrapperModule,
  ],
  providers: [DatePipe, { provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS, useValue: { useUtc: true } }],
})
export class ApiPortalSubscriptionsModule {}

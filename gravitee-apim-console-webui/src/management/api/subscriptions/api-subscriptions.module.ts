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
import { MatLegacyAutocompleteModule as MatAutocompleteModule } from '@angular/material/legacy-autocomplete';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacyOptionModule as MatOptionModule } from '@angular/material/legacy-core';
import { MatLegacyRadioModule as MatRadioModule } from '@angular/material/legacy-radio';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import {
  GioAvatarModule,
  GioClipboardModule,
  GioFormJsonSchemaModule,
  GioFormTagsInputModule,
  GioIconsModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { RouterModule } from '@angular/router';

import { ApiPortalSubscriptionCreationDialogComponent } from './components/dialogs/creation/api-portal-subscription-creation-dialog.component';
import { ApiPortalSubscriptionTransferDialogComponent } from './components/dialogs/transfer/api-portal-subscription-transfer-dialog.component';
import { ApiSubscriptionEditComponent } from './edit/api-subscription-edit.component';
import { ApiSubscriptionListComponent } from './list/api-subscription-list.component';
import { ApiPortalSubscriptionChangeEndDateDialogComponent } from './components/dialogs/change-end-date/api-portal-subscription-change-end-date-dialog.component';
import { ApiPortalSubscriptionValidateDialogComponent } from './components/dialogs/validate/api-portal-subscription-validate-dialog.component';
import { ApiKeyValidationComponent } from './components/api-key-validation/api-key-validation.component';
import { ApiPortalSubscriptionRejectDialogComponent } from './components/dialogs/reject/api-portal-subscription-reject-dialog.component';
import { ApiPortalSubscriptionRenewDialogComponent } from './components/dialogs/renew/api-portal-subscription-renew-dialog.component';
import { ApiPortalSubscriptionExpireApiKeyDialogComponent } from './components/dialogs/expire-api-key/api-portal-subscription-expire-api-key-dialog.component';

import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [
    ApiSubscriptionEditComponent,
    ApiSubscriptionListComponent,
    ApiPortalSubscriptionChangeEndDateDialogComponent,
    ApiPortalSubscriptionCreationDialogComponent,
    ApiPortalSubscriptionTransferDialogComponent,
    ApiPortalSubscriptionRejectDialogComponent,
    ApiPortalSubscriptionRenewDialogComponent,
    ApiPortalSubscriptionValidateDialogComponent,

    ApiPortalSubscriptionExpireApiKeyDialogComponent,

    ApiKeyValidationComponent,
  ],
  exports: [ApiSubscriptionEditComponent, ApiSubscriptionListComponent, ApiKeyValidationComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,

    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatLegacyDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule,
    MatSnackBarModule,
    MatRadioModule,
    MatTableModule,
    MatTooltipModule,
    MatButtonToggleModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,

    GioAvatarModule,
    GioClipboardModule,
    GioFormJsonSchemaModule,
    GioFormTagsInputModule,
    GioIconsModule,
    GioLoaderModule,
    GioPermissionModule,
    GioTableWrapperModule,
  ],
  providers: [DatePipe],
})
export class ApiSubscriptionsModule {}

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
import {
  GioAvatarModule,
  GioBannerModule,
  GioClipboardModule,
  GioFormJsonSchemaModule,
  GioFormTagsInputModule,
  GioIconsModule,
  GioLoaderModule,
  GioMonacoEditorModule,
} from '@gravitee/ui-particles-angular';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { RouterModule } from '@angular/router';
import { MatSortModule } from '@angular/material/sort';

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

import { SubscriptionEditPushConfigComponent } from '../../../components/subscription-edit-push-config/subscription-edit-push-config.component';
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
    MatFormFieldModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule,
    MatSnackBarModule,

    MatRadioModule,
    MatTableModule,
    MatSortModule,
    MatTooltipModule,
    MatButtonToggleModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,

    GioAvatarModule,
    GioClipboardModule,
    GioFormJsonSchemaModule,
    GioMonacoEditorModule,
    GioFormTagsInputModule,
    GioIconsModule,
    GioLoaderModule,
    GioPermissionModule,
    GioTableWrapperModule,
    GioBannerModule,
    SubscriptionEditPushConfigComponent,
  ],
  providers: [DatePipe],
})
export class ApiSubscriptionsModule {}

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
import {
  GioBannerModule,
  GioBreadcrumbModule,
  GioFormSlideToggleModule,
  GioIconsModule,
  GioLicenseModule,
  GioLoaderModule,
  GioMenuModule,
  GioSubmenuModule,
} from '@gravitee/ui-particles-angular';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { RouterModule } from '@angular/router';

import { ApiReviewDialogComponent } from './api-review-dialog/api-review-dialog.component';
import { ApiConfirmDeploymentDialogComponent } from './api-confirm-deployment-dialog/api-confirm-deployment-dialog.component';
import { ApiNavigationTabsComponent } from './api-navigation-tabs/api-navigation-tabs.component';
import { ApiNavigationTitleComponent } from './api-navigation-title/api-navigation-title.component';
import { ApiNavigationComponent } from './api-navigation.component';
import { ApiNavigationDisabledComponent } from './api-navigation-disabled/api-navigation-disabled.component';
import { ApiNavigationHeaderComponent } from './api-navigation-header/api-navigation-header.component';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    GioSubmenuModule,
    GioIconsModule,
    MatButtonModule,
    MatTooltipModule,
    MatTabsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    GioBreadcrumbModule,
    GioBannerModule,
    GioFormSlideToggleModule,
    GioLoaderModule,
    GioLicenseModule,
    MatSnackBarModule,
    RouterModule,
    GioMenuModule,
  ],
  declarations: [
    ApiNavigationComponent,
    ApiNavigationTitleComponent,
    ApiNavigationTabsComponent,
    ApiNavigationHeaderComponent,
    ApiNavigationDisabledComponent,
    ApiConfirmDeploymentDialogComponent,
    ApiReviewDialogComponent,
  ],
  exports: [ApiNavigationComponent],
})
export class ApiNavigationModule {}

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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import {
  GioBannerModule,
  GioClipboardModule,
  GioIconsModule,
  GioLoaderModule,
  GioMonacoEditorModule,
} from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { RouterLink } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';

import { ApiHistoryV4DeploymentCompareDialogComponent } from './deployment-compare-dialog/api-history-v4-deployment-compare-dialog.component';
import { ApiHistoryV4DeploymentsTableComponent } from './deployments-table/api-history-v4-deployments-table.component';
import { ApiHistoryV4Component } from './api-history-v4.component';
import { ApiHistoryV4DeploymentInfoDialogComponent } from './deployment-info-dialog/api-history-v4-deployment-info-dialog.component';

import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioDiffModule } from '../../../shared/components/gio-diff/gio-diff.module';

@NgModule({
  declarations: [ApiHistoryV4Component, ApiHistoryV4DeploymentsTableComponent, ApiHistoryV4DeploymentCompareDialogComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatSelectModule,
    MatTableModule,
    MatButtonToggleModule,

    GioIconsModule,
    GioPermissionModule,
    GioBannerModule,
    GioTableWrapperModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatDialogModule,
    MatDividerModule,
    GioMonacoEditorModule,
    RouterLink,
    GioLoaderModule,
    GioDiffModule,
    GioClipboardModule,
    ApiHistoryV4DeploymentInfoDialogComponent,
  ],
})
export class ApiHistoryV4Module {}

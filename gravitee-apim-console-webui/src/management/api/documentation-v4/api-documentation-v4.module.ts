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
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { CommonModule } from '@angular/common';
import { GioFormFilePickerModule, GioIconsModule, GioMonacoEditorModule, GioRadioButtonModule } from '@gravitee/ui-particles-angular';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacyRadioModule as MatRadioModule } from '@angular/material/legacy-radio';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTableModule } from '@angular/material/table';
import { MarkdownModule } from 'ngx-markdown';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { RouterModule } from '@angular/router';

import { ApiDocumentationV4EmptyStateComponent } from './components/documentation-empty-state/api-documentation-v4-empty-state.component';
import { ApiDocumentationV4ListNavigationHeaderComponent } from './components/documentation-list-navigation-header/api-documentation-v4-list-navigation-header.component';
import { ApiDocumentationV4Component } from './api-documentation-v4.component';
import { ApiDocumentationV4EditFolderDialog } from './dialog/documentation-edit-folder-dialog/api-documentation-v4-edit-folder-dialog.component';
import { ApiDocumentationV4EditPageComponent } from './documentation-edit-page/api-documentation-v4-edit-page.component';
import { ApiDocumentationV4VisibilityComponent } from './components/api-documentation-v4-visibility/api-documentation-v4-visibility.component';
import { ApiDocumentationV4PagesListComponent } from './documentation-pages-list/api-documentation-v4-pages-list.component';
import { ApiDocumentationV4ContentEditorComponent } from './components/api-documentation-v4-content-editor/api-documentation-v4-content-editor.component';
import { ApiDocumentationV4PageTitleComponent } from './components/api-documentation-v4-page-title/api-documentation-v4-page-title.component';
import { ApiDocumentationV4BreadcrumbComponent } from './components/api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.component';
import { ApiDocumentationV4FileUploadComponent } from './components/api-documentation-v4-file-upload/api-documentation-v4-file-upload.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTooltipOnEllipsisModule } from '../../../shared/components/gio-tooltip-on-ellipsis/gio-tooltip-on-ellipsis.module';

@NgModule({
  declarations: [
    ApiDocumentationV4Component,
    ApiDocumentationV4EmptyStateComponent,
    ApiDocumentationV4ListNavigationHeaderComponent,
    ApiDocumentationV4VisibilityComponent,
    ApiDocumentationV4ContentEditorComponent,
    ApiDocumentationV4PageTitleComponent,
    ApiDocumentationV4BreadcrumbComponent,
    ApiDocumentationV4EditFolderDialog,
    ApiDocumentationV4EditPageComponent,
    ApiDocumentationV4PagesListComponent,
    ApiDocumentationV4BreadcrumbComponent,
    ApiDocumentationV4FileUploadComponent,
  ],
  exports: [ApiDocumentationV4Component],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,

    MatCardModule,
    MatButtonModule,
    MatInputModule,
    MatRadioModule,
    MatLegacyDialogModule,
    MatDialogModule,
    MatStepperModule,
    MatButtonToggleModule,
    MatTableModule,
    MatSnackBarModule,
    MatTooltipModule,

    MarkdownModule.forRoot(),

    GioIconsModule,
    GioMonacoEditorModule,
    GioRadioButtonModule,
    GioPermissionModule,
    GioTooltipOnEllipsisModule,
    GioFormFilePickerModule,
  ],
})
export class ApiDocumentationV4Module {}

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
import { MatCardModule } from '@angular/material/card';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import {
  GioFormFilePickerModule,
  GioFormJsonSchemaModule,
  GioFormSelectionInlineModule,
  GioIconsModule,
  GioMonacoEditorModule,
} from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatDialogModule } from '@angular/material/dialog';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTableModule } from '@angular/material/table';
import { MarkdownModule } from 'ngx-markdown';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { MatMenu, MatMenuItem, MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';

import { ApiDocumentationV4EmptyStateComponent } from './components/documentation-empty-state/api-documentation-v4-empty-state.component';
import { ApiDocumentationV4ListNavigationHeaderComponent } from './components/documentation-list-navigation-header/api-documentation-v4-list-navigation-header.component';
import { ApiDocumentationV4Component } from './api-documentation-v4.component';
import { ApiDocumentationV4EditFolderDialog } from './dialog/documentation-edit-folder-dialog/api-documentation-v4-edit-folder-dialog.component';
import { ApiDocumentationV4EditPageComponent } from './documentation-edit-page/api-documentation-v4-edit-page.component';
import { ApiDocumentationV4VisibilityComponent } from './components/api-documentation-v4-visibility/api-documentation-v4-visibility.component';
import { ApiDocumentationV4PagesListComponent } from './components/api-documentation-v4-pages-list/api-documentation-v4-pages-list.component';
import { ApiDocumentationV4ContentEditorComponent } from './components/api-documentation-v4-content-editor/api-documentation-v4-content-editor.component';
import { ApiDocumentationV4PageTitleComponent } from './components/api-documentation-v4-page-title/api-documentation-v4-page-title.component';
import { ApiDocumentationV4BreadcrumbComponent } from './components/api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.component';
import { ApiDocumentationV4FileUploadComponent } from './components/api-documentation-v4-file-upload/api-documentation-v4-file-upload.component';
import { ApiDocumentationV4MetadataComponent } from './documentation-metadata/api-documentation-v4-metadata.component';
import { ApiDocumentationV4DefaultPageComponent } from './documentation-default-page/api-documentation-v4-default-page.component';
import { ApiDocumentationV4HomePageHeaderComponent } from './components/documentation-home-page-header/api-documentation-v4-home-page-header.component';
import { ApiDocumentationV4AddPageButtonComponent } from './components/api-documentation-v4-add-page-button/api-documentation-v4-add-page-button.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTooltipOnEllipsisModule } from '../../../shared/components/gio-tooltip-on-ellipsis/gio-tooltip-on-ellipsis.module';
import { GioSwaggerUiModule } from '../../../components/documentation/gio-swagger-ui/gio-swagger-ui.module';
import { GioAsyncApiModule } from '../../../components/documentation/gio-async-api/gio-async-api-module';
import { GioMetadataModule } from '../../../components/gio-metadata/gio-metadata.module';
import { GioApiMetadataListModule } from '../component/gio-api-metadata-list/gio-api-metadata-list.module';

@NgModule({
  declarations: [
    ApiDocumentationV4Component,
    ApiDocumentationV4EmptyStateComponent,
    ApiDocumentationV4ListNavigationHeaderComponent,
    ApiDocumentationV4HomePageHeaderComponent,
    ApiDocumentationV4VisibilityComponent,
    ApiDocumentationV4ContentEditorComponent,
    ApiDocumentationV4PageTitleComponent,
    ApiDocumentationV4BreadcrumbComponent,
    ApiDocumentationV4EditFolderDialog,
    ApiDocumentationV4EditPageComponent,
    ApiDocumentationV4DefaultPageComponent,
    ApiDocumentationV4AddPageButtonComponent,
    ApiDocumentationV4PagesListComponent,
    ApiDocumentationV4FileUploadComponent,
    ApiDocumentationV4MetadataComponent,
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
    MatDialogModule,
    MatStepperModule,
    MatButtonToggleModule,
    MatTableModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatIconModule,

    MarkdownModule.forRoot(),

    GioIconsModule,
    GioMonacoEditorModule,
    GioPermissionModule,
    GioTooltipOnEllipsisModule,
    GioFormFilePickerModule,
    GioFormSelectionInlineModule,
    NgOptimizedImage,
    MatMenuModule,
    GioSwaggerUiModule,
    GioAsyncApiModule,
    GioMetadataModule,
    GioApiMetadataListModule,
<<<<<<< HEAD
    GioFormJsonSchemaModule,
    MatMenu,
    MatMenuItem,
=======
    MatOption,
    MatSelect,
    GioFormSlideToggleModule,
    MatSlideToggle,
    GioFormJsonSchemaModule,
>>>>>>> 2eb1d7f2cd (feat(console): User can import and publish a page from a remote URL)
  ],
})
export class ApiDocumentationV4Module {}

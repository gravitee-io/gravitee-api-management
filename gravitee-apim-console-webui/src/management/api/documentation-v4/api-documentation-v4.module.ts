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
import { CommonModule } from '@angular/common';
import { GioIconsModule, GioMonacoEditorModule, GioRadioButtonModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatDialogModule } from '@angular/material/dialog';
import { UIRouterModule } from '@uirouter/angular';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTableModule } from '@angular/material/table';
import { MarkdownModule } from 'ngx-markdown';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

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
  ],
  exports: [ApiDocumentationV4Component],
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    GioIconsModule,
    MatButtonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatRadioModule,
    MatDialogModule,
    UIRouterModule,
    MatStepperModule,
    MatButtonToggleModule,
    GioMonacoEditorModule,
    GioRadioButtonModule,
    MatTableModule,
    MatSnackBarModule,
    MarkdownModule.forRoot(),
    MatTooltipModule,
  ],
})
export class ApiDocumentationV4Module {}

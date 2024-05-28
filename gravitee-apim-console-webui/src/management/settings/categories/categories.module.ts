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
import { MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import {
  GioAvatarModule,
  GioConfirmDialogModule,
  GioFormFilePickerModule,
  GioFormSlideToggleModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';

import { CategoryComponent } from './category/category.component';
import { CategoriesComponent } from './categories.component';
import { AddApiToCategoryDialogComponent } from './add-api-to-category-dialog/add-api-to-category-dialog.component';

import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@NgModule({
  declarations: [CategoriesComponent, CategoryComponent, AddApiToCategoryDialogComponent],
  exports: [CategoriesComponent, CategoryComponent, AddApiToCategoryDialogComponent],
  imports: [
    CommonModule,
    GioConfirmDialogModule,
    MatDialogModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    RouterModule,
    GioFormSlideToggleModule,
    ReactiveFormsModule,
    MatSlideToggle,
    GioAvatarModule,
    GioPermissionModule,
    GioGoBackButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInput,
    GioFormFilePickerModule,
    GioSaveBarModule,
    GioTableWrapperModule,
    MatRadioGroup,
    MatRadioButton,
  ],
})
export class CategoriesModule {}

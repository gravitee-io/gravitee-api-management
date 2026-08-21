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
import { Component, computed, inject } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { PortalNavigationApiProduct } from '../../../../entities/management-api-v2';

export interface ApiProductCategoryCandidate {
  navigationItem: PortalNavigationApiProduct;
  name: string;
  version: string;
}

export interface AddApiProductToCategoryDialogData {
  title: string;
  candidates: ApiProductCategoryCandidate[];
}

export type AddApiProductToCategoryDialogResult = ApiProductCategoryCandidate;

@Component({
  selector: 'add-api-product-to-category-dialog',
  templateUrl: './add-api-product-to-category-dialog.component.html',
  styleUrl: './add-api-product-to-category-dialog.component.scss',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
})
export class AddApiProductToCategoryDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AddApiProductToCategoryDialogComponent, AddApiProductToCategoryDialogResult>);
  protected readonly data = inject<AddApiProductToCategoryDialogData>(MAT_DIALOG_DATA);

  protected readonly searchApiProductControl = new FormControl<string | ApiProductCategoryCandidate>('', { nonNullable: true });

  private readonly searchValue = toSignal(this.searchApiProductControl.valueChanges, {
    initialValue: this.searchApiProductControl.value,
  });

  protected readonly isApiProductSelected = computed(() => this.isApiProductCandidate(this.searchValue()));

  protected readonly filteredOptions = computed(() => {
    const value = this.searchValue();
    const term = (this.isApiProductCandidate(value) ? value.name : value).trim().toLowerCase();
    return this.data.candidates.filter(candidate => candidate.name.toLowerCase().includes(term));
  });

  protected displayFn(option: ApiProductCategoryCandidate): string {
    return option?.name ?? '';
  }

  protected resetSearchTerm(): void {
    this.searchApiProductControl.setValue('');
  }

  protected onCancelClick(): void {
    this.dialogRef.close();
  }

  protected submit(): void {
    const value = this.searchApiProductControl.getRawValue();
    if (this.isApiProductCandidate(value)) {
      this.dialogRef.close(value);
    }
  }

  private isApiProductCandidate(value: string | ApiProductCategoryCandidate): value is ApiProductCategoryCandidate {
    return typeof value !== 'string';
  }
}

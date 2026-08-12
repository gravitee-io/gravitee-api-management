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
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { PortalNavigationApi } from '../../../../entities/management-api-v2';

export interface AddApiToCategoryDialogData {
  title: string;
  /** Published API navigation items not yet assigned to this category. */
  candidates: PortalNavigationApi[];
}

export type AddApiToCategoryDialogResult = PortalNavigationApi;

@Component({
  selector: 'add-api-to-category-dialog',
  templateUrl: './add-api-to-category-dialog.component.html',
  styleUrls: ['./add-api-to-category-dialog.component.scss'],
  imports: [
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatIconModule,
  ],
})
export class AddApiToCategoryDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AddApiToCategoryDialogComponent, AddApiToCategoryDialogResult>);
  protected readonly data = inject<AddApiToCategoryDialogData>(MAT_DIALOG_DATA);

  protected readonly searchApiControl = new FormControl<string | PortalNavigationApi>('', { nonNullable: true });

  private readonly searchValue = toSignal(this.searchApiControl.valueChanges, { initialValue: this.searchApiControl.value });

  protected readonly isApiSelected = computed(() => this.isNavigationApi(this.searchValue()));

  protected readonly filteredOptions = computed(() => {
    const value = this.searchValue();
    const term = (this.isNavigationApi(value) ? value.title : value).trim().toLowerCase();
    return this.data.candidates.filter(candidate => candidate.title.toLowerCase().includes(term));
  });

  protected displayFn(option: PortalNavigationApi): string {
    return option?.title ?? '';
  }

  protected resetSearchTerm(): void {
    this.searchApiControl.setValue('');
  }

  protected onCancelClick(): void {
    this.dialogRef.close();
  }

  protected submit(): void {
    const value = this.searchApiControl.getRawValue();
    if (this.isNavigationApi(value)) {
      this.dialogRef.close(value);
    }
  }

  private isNavigationApi(value: string | PortalNavigationApi): value is PortalNavigationApi {
    return typeof value !== 'string';
  }
}

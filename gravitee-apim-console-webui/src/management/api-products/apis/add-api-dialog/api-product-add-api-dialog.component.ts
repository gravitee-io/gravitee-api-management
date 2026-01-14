/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { Component, Inject, OnInit, DestroyRef, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, switchMap, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { GioAvatarModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api } from '../../../../entities/management-api-v2';

export interface ApiProductAddApiDialogData {
  apiProductId: string;
  existingApiIds?: string[];
}

export type ApiProductAddApiDialogResult = Api;

@Component({
  selector: 'api-product-add-api-dialog',
  templateUrl: './api-product-add-api-dialog.component.html',
  styleUrls: ['./api-product-add-api-dialog.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatIconModule,
    GioIconsModule,
    GioAvatarModule,
  ],
  standalone: true,
})
export class ApiProductAddApiDialogComponent implements OnInit {
  public searchApiControl: FormControl<string | Api> = new FormControl('');
  public filteredOptions$: Observable<Api[]>;
  public isApiSelected = false;
  private destroyRef = inject(DestroyRef);

  constructor(
    public dialogRef: MatDialogRef<ApiProductAddApiDialogComponent>,
    private apiService: ApiV2Service,
    @Inject(MAT_DIALOG_DATA) public data: ApiProductAddApiDialogData,
  ) {}

  ngOnInit(): void {
    this.filteredOptions$ = this.searchApiControl.valueChanges.pipe(
      filter((v) => typeof v === 'string'),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((term: string) =>
        this.apiService.search({
          query: term,
        }),
      ),
      tap(() => (this.isApiSelected = false)),
      map((apisResponse) => {
        // Filter out APIs that are already in the API Product
        const existingIds = this.data.existingApiIds || [];
        return apisResponse.data.filter((api) => !existingIds.includes(api.id));
      }),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  public displayFn(option: Api): string {
    if (option && option.name && option.apiVersion) {
      return option.name + ' - ' + option.apiVersion;
    }
    return option.toString();
  }

  resetSearchTerm() {
    this.searchApiControl.setValue('');
  }

  onCancelClick(): void {
    this.dialogRef.close();
  }

  selectAPI(): void {
    this.isApiSelected = true;
  }

  public submit() {
    const selectedApi = this.searchApiControl.getRawValue();
    if (selectedApi && typeof selectedApi !== 'string') {
      this.dialogRef.close(selectedApi);
    }
  }
}

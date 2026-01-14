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
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { GioAvatarModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiProductV2Service } from '../../../../services-ngx/api-product-v2.service';
import { Api } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

export interface ApiProductAddApiDialogData {
  apiProductId: string;
  existingApiIds?: string[];
}

export type ApiProductAddApiDialogResult = boolean;

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
  public isSubmitting = false;
  private destroyRef = inject(DestroyRef);

  constructor(
    public dialogRef: MatDialogRef<ApiProductAddApiDialogComponent>,
    private apiService: ApiV2Service,
    private apiProductV2Service: ApiProductV2Service,
    private snackBarService: SnackBarService,
    @Inject(MAT_DIALOG_DATA) public data: ApiProductAddApiDialogData,
  ) {}

  ngOnInit(): void {
    // Filtered options for autocomplete - only show when input has text
    this.filteredOptions$ = this.searchApiControl.valueChanges.pipe(
      filter((v) => typeof v === 'string'),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((term: string) => {
        // Don't search if input is empty or only whitespace
        if (!term || term.trim() === '') {
          return of<Api[]>([]);
        }
        return this.apiService.search({
          query: term,
        });
      }),
      tap(() => (this.isApiSelected = false)),
      map((apisResponse) => {
        // Filter out APIs that are already in the API Product
        const existingIds = this.data.existingApiIds || [];
        // Handle both array (from of([])) and ApisResponse (from search)
        if (Array.isArray(apisResponse)) {
          return apisResponse;
        }
        return apisResponse.data ? apisResponse.data.filter((api) => !existingIds.includes(api.id)) : [];
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
    if (!selectedApi || typeof selectedApi === 'string') {
      return;
    }

    this.isSubmitting = true;

    // Get current API Product to get existing API IDs
    this.apiProductV2Service
      .get(this.data.apiProductId)
      .pipe(
        switchMap((apiProduct) => {
          const currentApiIds = apiProduct.apiIds || [];
          
          // Check if API is already in the product
          if (currentApiIds.includes(selectedApi.id)) {
            this.snackBarService.error(`API "${selectedApi.name}" is already in this API Product`);
            this.isSubmitting = false;
            return of(null);
          }

          // Add the new API ID to the list
          const updatedApiIds = [...currentApiIds, selectedApi.id];

          // Update the API Product with the new API list
          return this.apiProductV2Service.updateApiProductApis(this.data.apiProductId, updatedApiIds);
        }),
        catchError((error) => {
          this.snackBarService.error(error.error?.message || 'An error occurred while adding the API');
          this.isSubmitting = false;
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        if (result) {
          this.snackBarService.success(`API "${selectedApi.name}" has been added to the API Product`);
          // Close dialog with success indicator
          this.dialogRef.close(true);
        } else {
          this.isSubmitting = false;
        }
      });
  }

  public isNonEmptyString(value: string | Api | null | undefined): boolean {
    return typeof value === 'string' && value.trim().length > 0;
  }
}

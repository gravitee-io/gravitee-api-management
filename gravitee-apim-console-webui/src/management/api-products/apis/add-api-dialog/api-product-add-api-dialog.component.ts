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
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, startWith, switchMap, take, tap } from 'rxjs/operators';
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
  public recommendedApis$: Observable<Api[]>;
  public selectedApis: Api[] = [];
  private selectedApis$ = new BehaviorSubject<Api[]>([]);
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
    this.filteredOptions$ = combineLatest([
      this.searchApiControl.valueChanges.pipe(
        filter((v) => typeof v === 'string'),
        debounceTime(300),
        distinctUntilChanged(),
        startWith(''),
      ),
      this.selectedApis$.asObservable().pipe(startWith([])),
    ]).pipe(
      switchMap(([term, selectedApis]) => {
        // Don't search if input is empty or only whitespace
        if (!term || term.trim() === '') {
          return of<Api[]>([]);
        }
        return this.apiService.search({
          query: term,
        }).pipe(
          map((apisResponse) => {
            // Filter out APIs that are already in the API Product or already selected
            const existingIds = this.data.existingApiIds || [];
            const selectedIds = selectedApis.map((api) => api.id);
            // Handle both array (from of([])) and ApisResponse (from search)
            if (Array.isArray(apisResponse)) {
              return apisResponse.filter((api) => !existingIds.includes(api.id) && !selectedIds.includes(api.id));
            }
            return apisResponse.data
              ? apisResponse.data.filter((api) => !existingIds.includes(api.id) && !selectedIds.includes(api.id))
              : [];
          }),
        );
      }),
      tap(() => (this.isApiSelected = false)),
      takeUntilDestroyed(this.destroyRef),
    );

    // Recommended APIs - show when input is empty
    this.recommendedApis$ = combineLatest([
      this.searchApiControl.valueChanges.pipe(startWith('')),
      this.selectedApis$.asObservable().pipe(startWith([])),
    ]).pipe(
      switchMap(([value, selectedApis]) => {
        // Show recommended APIs only when input is empty
        if (!value || (typeof value === 'string' && value.trim() === '')) {
          // Only show these two specific API IDs in recommended list
          const allowedApiIds = [ '3cdb5fa8-b8b6-37db-8818-d80979dd39e3'];
          return this.apiService.search({}, undefined, 1, 10).pipe(
            map((apisResponse) => {
              const existingIds = this.data.existingApiIds || [];
              const selectedIds = selectedApis.map((api) => api.id);
              return apisResponse.data
                ? apisResponse.data.filter(
                    (api) =>
                      allowedApiIds.includes(api.id) && !existingIds.includes(api.id) && !selectedIds.includes(api.id)
                  )
                : [];
            }),
          );
        }
        return of<Api[]>([]);
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
    const selectedApi = this.searchApiControl.getRawValue();
    if (selectedApi && typeof selectedApi !== 'string') {
      // Check if API is already selected
      if (!this.selectedApis.find((api) => api.id === selectedApi.id)) {
        this.selectedApis.push(selectedApi);
        this.selectedApis$.next(this.selectedApis);
        this.isApiSelected = true;
      }
      // Clear the input field
      this.searchApiControl.setValue('');
    }
  }

  public submit() {
    // Submit all APIs that are displayed as chips
    if (this.selectedApis.length === 0) {
      return;
    }

    this.isSubmitting = true;

    // Get current API Product to get existing API IDs
    this.apiProductV2Service
      .get(this.data.apiProductId)
      .pipe(
        switchMap((apiProduct) => {
          const currentApiIds = apiProduct.apiIds || [];
          const newApiIds: string[] = [];
          const errors: string[] = [];

          // Process each API from the chips (selectedApis array)
          this.selectedApis.forEach((api) => {
            if (currentApiIds.includes(api.id)) {
              errors.push(`API "${api.name}" is already in this API Product`);
            } else {
              newApiIds.push(api.id);
            }
          });

          // Show errors if any
          if (errors.length > 0) {
            this.snackBarService.error(errors.join(', '));
            this.isSubmitting = false;
            return of(null);
          }

          // Add all new API IDs to the list
          const updatedApiIds = [...currentApiIds, ...newApiIds];

          // Update the API Product with the new API list
          return this.apiProductV2Service.updateApiProductApis(this.data.apiProductId, updatedApiIds);
        }),
        catchError((error) => {
          this.snackBarService.error(error.error?.message || 'An error occurred while adding the APIs');
          this.isSubmitting = false;
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        if (result) {
          const apiNames = this.selectedApis.map((api) => api.name).join(', ');
          this.snackBarService.success(
            this.selectedApis.length === 1
              ? `API "${apiNames}" has been added to the API Product`
              : `${this.selectedApis.length} APIs have been added to the API Product`,
          );
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

  public selectRecommendedApi(api: Api): void {
    // Check if API is already selected
    if (!this.selectedApis.find((selectedApi) => selectedApi.id === api.id)) {
      this.selectedApis.push(api);
      this.selectedApis$.next(this.selectedApis);
      this.isApiSelected = true;
    }
    // Clear the input field
    this.searchApiControl.setValue('');
  }

  public removeApi(apiToRemove: Api): void {
    this.selectedApis = this.selectedApis.filter((api) => api.id !== apiToRemove.id);
    this.selectedApis$.next(this.selectedApis);
    this.isApiSelected = this.selectedApis.length > 0;
  }

  public selectAllRecommendedApis(): void {
    // Get current recommended APIs and select all that aren't already selected
    this.recommendedApis$
      .pipe(
        filter((apis) => apis.length > 0 && !this.isNonEmptyString(this.searchApiControl.getRawValue())),
        take(1),
      )
      .subscribe((recommendedApis) => {
        const selectedIds = this.selectedApis.map((api) => api.id);
        // Filter out APIs that are already selected
        const newApis = recommendedApis.filter((api) => !selectedIds.includes(api.id));
        
        if (newApis.length > 0) {
          // Add all new APIs to selectedApis
          this.selectedApis.push(...newApis);
          this.selectedApis$.next(this.selectedApis);
          this.isApiSelected = true;
        }
      });
  }

  public getContextPath(api: Api): string | undefined {
    if ('contextPath' in api) {
      return api.contextPath;
    }
    return undefined;
  }
}

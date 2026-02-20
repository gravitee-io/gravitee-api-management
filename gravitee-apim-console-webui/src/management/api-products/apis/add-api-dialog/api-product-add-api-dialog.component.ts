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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AsyncPipe } from '@angular/common';
import { GioAvatarModule, GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';

import { Api } from '../../../../entities/management-api-v2';
import { ApiProductV2Service } from '../../../../services-ngx/api-product-v2.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { getApiContextPath } from '../../../../shared/utils/api.util';

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
    AsyncPipe,
    FormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
    GioAvatarModule,
    GioBannerModule,
    GioIconsModule,
  ],
  standalone: true,
})
export class ApiProductAddApiDialogComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<ApiProductAddApiDialogComponent>);
  private readonly apiService = inject(ApiV2Service);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly snackBarService = inject(SnackBarService);
  readonly data = inject<ApiProductAddApiDialogData>(MAT_DIALOG_DATA);
  readonly getContextPath = getApiContextPath;

  get hasNonEmptySearchTerm(): boolean {
    const v = this.searchApiControl.value;
    return typeof v === 'string' && v.trim().length > 0;
  }

  readonly searchApiControl = new FormControl<string | Api>('');
  filteredOptions$!: Observable<Api[]>;
  selectedApis: Api[] = [];
  private readonly selectedApis$ = new BehaviorSubject<Api[]>([]);
  isApiSelected = false;
  isSubmitting = false;

  ngOnInit(): void {
    this.filteredOptions$ = combineLatest([
      this.searchApiControl.valueChanges.pipe(
        filter((v): v is string => typeof v === 'string'),
        debounceTime(300),
        distinctUntilChanged(),
        startWith(''),
      ),
      this.selectedApis$.asObservable().pipe(startWith([])),
    ]).pipe(
      switchMap(([term, selectedApis]) => {
        if (!term || term.trim() === '') {
          return of([]);
        }
        return this.apiService.search({ query: term, apiTypes: ['V4_HTTP_PROXY'], allowedInApiProducts: true }).pipe(
          map(apisResponse => {
            const existingIds = this.data.existingApiIds || [];
            const selectedIds = selectedApis.map(api => api.id);
            const apis = apisResponse.data || [];
            return apis.filter(api => !existingIds.includes(api.id) && !selectedIds.includes(api.id));
          }),
        );
      }),
      tap(() => (this.isApiSelected = false)),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  readonly displayFn = (option: Api): string => (option?.name && option?.apiVersion ? `${option.name} - ${option.apiVersion}` : '');

  resetSearchTerm(): void {
    this.searchApiControl.setValue('');
  }

  onCancelClick(): void {
    this.dialogRef.close();
  }

  selectAPI(): void {
    const selectedApi = this.searchApiControl.getRawValue();
    if (selectedApi && typeof selectedApi !== 'string') {
      if (!this.selectedApis.find(api => api.id === selectedApi.id)) {
        this.selectedApis.push(selectedApi);
        this.selectedApis$.next(this.selectedApis);
        this.isApiSelected = true;
      }
      this.searchApiControl.setValue('');
    }
  }

  submit(): void {
    if (this.selectedApis.length === 0) return;

    this.isSubmitting = true;

    this.apiProductV2Service
      .get(this.data.apiProductId)
      .pipe(
        switchMap(apiProduct => this.buildUpdateRequest(apiProduct)),
        catchError(error => {
          this.snackBarService.error(error.error?.message || 'An error occurred while adding the APIs');
          this.isSubmitting = false;
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(result => {
        if (!result) {
          this.isSubmitting = false;
          return;
        }
        const apiNames = this.selectedApis.map(api => api.name).join(', ');
        this.snackBarService.success(
          this.selectedApis.length === 1
            ? `API "${apiNames}" has been added to the API Product`
            : `${this.selectedApis.length} APIs have been added to the API Product`,
        );
        this.dialogRef.close(true);
      });
  }

  private buildUpdateRequest(apiProduct: { apiIds?: string[] }) {
    const currentApiIds = apiProduct.apiIds || [];
    const { newApiIds, errors } = this.selectedApis.reduce(
      (acc, api) => {
        if (currentApiIds.includes(api.id)) {
          acc.errors.push(`API "${api.name}" is already in this API Product`);
        } else {
          acc.newApiIds.push(api.id);
        }
        return acc;
      },
      { newApiIds: [] as string[], errors: [] as string[] },
    );

    if (errors.length > 0) {
      this.snackBarService.error(errors.join(', '));
      this.isSubmitting = false;
      return of(null);
    }

    const updatedApiIds = [...currentApiIds, ...newApiIds];
    return this.apiProductV2Service.updateApiProductApis(this.data.apiProductId, updatedApiIds);
  }

  removeApi(apiToRemove: Api): void {
    this.selectedApis = this.selectedApis.filter(api => api.id !== apiToRemove.id);
    this.selectedApis$.next(this.selectedApis);
    this.isApiSelected = this.selectedApis.length > 0;
  }
}

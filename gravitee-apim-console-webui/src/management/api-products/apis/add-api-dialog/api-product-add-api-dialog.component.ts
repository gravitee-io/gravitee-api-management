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

import { Component, computed, inject, Injector, OnInit, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { combineLatest, Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, startWith, switchMap } from 'rxjs/operators';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AsyncPipe } from '@angular/common';
import { GioAvatarModule, GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';

import { Api } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { getApiContextPath } from '../../../../shared/utils/api.util';

export type ApiWithContextPath = Api & { contextPath: string | null };

export interface ApiProductAddApiDialogData {
  apiProductId: string;
  existingApiIds?: string[];
}

export type ApiProductAddApiDialogResult = Api[] | undefined;

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
  private readonly injector = inject(Injector);
  private readonly dialogRef = inject(MatDialogRef<ApiProductAddApiDialogComponent>);
  private readonly apiService = inject(ApiV2Service);
  readonly data = inject<ApiProductAddApiDialogData>(MAT_DIALOG_DATA);

  get hasNonEmptySearchTerm(): boolean {
    const v = this.searchApiControl.value;
    return typeof v === 'string' && v.trim().length > 0;
  }

  readonly searchApiControl = new FormControl<string | Api>('');
  filteredOptions$!: Observable<ApiWithContextPath[]>;

  readonly selectedApis = signal<Api[]>([]);
  readonly selectedApisWithContext = computed(() => this.selectedApis().map(api => ({ ...api, contextPath: getApiContextPath(api) })));
  readonly validationError = signal<string | null>(null);

  ngOnInit(): void {
    this.filteredOptions$ = combineLatest([
      this.searchApiControl.valueChanges.pipe(
        filter((v): v is string => typeof v === 'string'),
        debounceTime(300),
        distinctUntilChanged(),
        startWith(''),
      ),
      toObservable(this.selectedApis, { injector: this.injector }),
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
            return apis
              .filter(api => !existingIds.includes(api.id) && !selectedIds.includes(api.id))
              .map(api => ({ ...api, contextPath: getApiContextPath(api) }));
          }),
        );
      }),
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
      if (!this.selectedApis().find(api => api.id === selectedApi.id)) {
        this.selectedApis.update(apis => [...apis, selectedApi]);
      }
      this.searchApiControl.setValue('');
    }
  }

  submit(): void {
    if (this.selectedApis().length === 0) return;

    this.validationError.set(null);

    const existingIds = this.data.existingApiIds || [];
    const errors = this.selectedApis()
      .filter(api => existingIds.includes(api.id))
      .map(api => `API "${api.name}" is already in this API Product`);

    if (errors.length > 0) {
      this.validationError.set(errors.join(', '));
      return;
    }

    this.dialogRef.close(this.selectedApis());
  }

  removeApi(apiToRemove: Api): void {
    this.selectedApis.update(apis => apis.filter(api => api.id !== apiToRemove.id));
  }
}

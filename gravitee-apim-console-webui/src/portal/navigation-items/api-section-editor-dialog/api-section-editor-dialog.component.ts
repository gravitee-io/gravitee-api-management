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
import { AsyncPipe } from '@angular/common';
import { Component, HostListener, inject, OnInit, computed, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { isEqual } from 'lodash';
import { Observable, Subject, of, BehaviorSubject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, startWith, switchMap, tap } from 'rxjs/operators';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api, ApiV2, ApiV4, PortalVisibility, ApisResponse } from '../../../entities/management-api-v2';
import { getApiAccess } from '../../../shared/utils';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

export interface ApiSectionEditorDialogData {
  mode: 'create';
}

export interface ApiSectionEditorDialogResult {
  visibility: PortalVisibility;
  apiId?: string;
  apiIds: string[];
}

type ApiRow = {
  id: string;
  name: string;
  path: string;
  labels?: string;
  isDisabled: boolean;
};

type SelectedApi = {
  id: string;
  name: string;
};

interface ApiSectionFormControls {
  apiIds: FormControl<string[]>;
}

interface ApiSectionFormValues {
  apiIds: string[];
}

type ApiSectionForm = FormGroup<ApiSectionFormControls>;

@Component({
  selector: 'api-section-editor-dialog',
  imports: [
    MatDialogModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatCheckboxModule,
    MatTableModule,
    MatChipsModule,
    MatExpansionModule,
    AsyncPipe,
    GioTableWrapperModule,
  ],
  templateUrl: './api-section-editor-dialog.component.html',
  styleUrls: ['./api-section-editor-dialog.component.scss'],
})
export class ApiSectionEditorDialogComponent implements OnInit {
  private readonly apiService = inject(ApiV2Service);

  form!: ApiSectionForm;
  public initialFormValues: ApiSectionFormValues;

  public title = 'Add APIs';

  displayedColumns = ['select', 'name', 'path', 'labels'];

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  total = 0;
  isLoading = true;

  private readonly filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private readonly refresh$ = new Subject<void>();

  private selectedOrderedApis = signal<SelectedApi[]>([]);
  selectedCount = computed(() => this.selectedOrderedApis().length);
  selectedApis = computed(() => this.selectedOrderedApis());

  rows$: Observable<ApiRow[]>;

  selectedPanelExpanded = signal(true);

  private readonly dialogRef = inject(MatDialogRef<ApiSectionEditorDialogComponent, ApiSectionEditorDialogResult>);

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent) {
    if (!this.formIsUnchanged()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  ngOnInit(): void {
    this.form = new FormGroup<ApiSectionFormControls>({
      apiIds: new FormControl<string[]>([], {
        validators: [Validators.required],
        nonNullable: true,
      }),
    });

    const initialApiIds = this.form.controls.apiIds.value ?? [];
    this.selectedOrderedApis.set(initialApiIds.map(id => ({ id, name: id })));

    this.form.controls.apiIds.valueChanges.pipe(distinctUntilChanged(isEqual)).subscribe(apiIds => {
      const ids = apiIds ?? [];
      const current = this.selectedOrderedApis();
      this.selectedOrderedApis.set(current.filter(a => ids.includes(a.id)));
    });

    const disabledSet = new Set<string>([]);

    this.rows$ = this.filters$.pipe(
      debounceTime(100),
      distinctUntilChanged(isEqual),
      switchMap(filters =>
        this.refresh$.pipe(
          startWith(undefined),
          tap(() => (this.isLoading = true)),
          switchMap(() =>
            this.apiService.search({ query: filters.searchTerm }, undefined, filters.pagination.index, filters.pagination.size, false),
          ),
          catchError((): Observable<ApisResponse> => {
            this.total = 0;
            return of({ data: [], pagination: undefined, links: undefined });
          }),
          tap(() => (this.isLoading = false)),
        ),
      ),
      tap(resp => {
        this.total = resp.pagination?.totalCount ?? 0;
      }),
      map(resp =>
        (resp.data ?? []).map((api: Api) => {
          const isV2OrV4 = this.isApiV2OrV4(api);
          const access = isV2OrV4 ? getApiAccess(api) : null;

          return {
            id: api.id,
            name: api.name,
            path: access?.[0] ?? '',
            labels: isV2OrV4 ? (api.labels ?? []).join(', ') : '',
            isDisabled: disabledSet.has(api.id),
          };
        }),
      ),
    );

    this.initialFormValues = this.form.getRawValue();
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
    this.refresh$.next();
  }

  isChecked(apiId: string): boolean {
    return (this.form.controls.apiIds.value ?? []).includes(apiId);
  }

  toggleApiSelection(apiId: string, checked: boolean, apiName?: string): void {
    if (checked) {
      this.addApiId(apiId, apiName);
    } else {
      this.removeApiId(apiId);
    }
  }

  removeSelected(apiId: string): void {
    this.removeApiId(apiId);
  }

  private addApiId(apiId: string, apiName?: string): void {
    const currentIds = this.form.controls.apiIds.value ?? [];
    if (currentIds.includes(apiId)) {
      return;
    }

    const nextIds = [...currentIds, apiId];
    this.form.controls.apiIds.setValue(nextIds);

    const current = this.selectedOrderedApis();
    this.selectedOrderedApis.set([...current, { id: apiId, name: apiName ?? apiId }]);
  }

  private removeApiId(apiId: string): void {
    const currentIds = this.form.controls.apiIds.value ?? [];
    if (!currentIds.includes(apiId)) {
      return;
    }

    const nextIds = currentIds.filter(id => id !== apiId);
    this.form.controls.apiIds.setValue(nextIds);

    const current = this.selectedOrderedApis();
    this.selectedOrderedApis.set(current.filter(a => a.id !== apiId));
  }

  onSubmit(): void {
    if (this.form.valid) {
      const formValues = this.form.getRawValue();
      const apiIds = formValues.apiIds ?? [];

      this.dialogRef.close({
        visibility: 'PUBLIC',
        apiIds,
        apiId: apiIds[0],
      });
    }
  }

  close(): void {
    this.dialogRef.close();
  }

  formIsUnchanged(): boolean {
    return isEqual(this.form.getRawValue(), this.initialFormValues);
  }

  private isApiV2OrV4(api: Api): api is ApiV2 | ApiV4 {
    return api.definitionVersion === 'V2' || api.definitionVersion === 'V4';
  }
}

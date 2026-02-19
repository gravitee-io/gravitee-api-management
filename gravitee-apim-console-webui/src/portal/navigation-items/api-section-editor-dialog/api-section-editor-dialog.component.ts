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
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { isEqual } from 'lodash';
import { Observable, Subject, of, BehaviorSubject, combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, startWith, switchMap, tap, finalize } from 'rxjs/operators';
import { MatSelectModule } from '@angular/material/select';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api, ApiV2, ApiV4, PortalVisibility, ApisResponse, PortalNavigationApi } from '../../../entities/management-api-v2';
import { getApiAccess } from '../../../shared/utils';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

export interface ApiSectionEditorDialogData {
  mode: 'create' | 'edit';
  existingItem?: PortalNavigationApi;
}

export interface ApiSectionEditorDialogResult {
  visibility: PortalVisibility;
  apiId?: string;
  apiIds?: string[];
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
  visibility: FormControl<PortalVisibility>;
}

interface ApiSectionFormValues {
  apiIds: string[];
  visibility: PortalVisibility;
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
    MatSelectModule,
    AsyncPipe,
    GioTableWrapperModule,
  ],
  templateUrl: './api-section-editor-dialog.component.html',
  styleUrls: ['./api-section-editor-dialog.component.scss'],
})
export class ApiSectionEditorDialogComponent implements OnInit {
  private readonly apiService = inject(ApiV2Service);

  readonly data: ApiSectionEditorDialogData = inject(MAT_DIALOG_DATA);
  readonly isEditMode = computed(() => this.data?.mode === 'edit');

  form!: ApiSectionForm;
  public initialFormValues: ApiSectionFormValues;

  public title = 'Add APIs';
  public buttonTitle = 'Add';

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
    const isEditMode = this.isEditMode();

    if (isEditMode) {
      this.title = 'Edit API';
      this.buttonTitle = 'Save';
    }

    this.form = new FormGroup<ApiSectionFormControls>({
      apiIds: new FormControl<string[]>([], {
        validators: isEditMode ? [] : [Validators.required],
        nonNullable: true,
      }),
      visibility: new FormControl<PortalVisibility>(this.data?.existingItem?.visibility ?? 'PUBLIC', {
        validators: [Validators.required],
        nonNullable: true,
      }),
    });

    if (isEditMode) {
      const id = this.data.existingItem?.apiId;
      if (id) {
        this.form.controls.apiIds.setValue([id]);
        this.selectedOrderedApis.set([{ id, name: id }]);
      }

      this.rows$ = of([]);
      this.initialFormValues = this.form.getRawValue();
      return;
    }

    const initialApiIds = this.form.controls.apiIds.value ?? [];
    this.selectedOrderedApis.set(initialApiIds.map(id => ({ id, name: id })));

    this.form.controls.apiIds.valueChanges.pipe(distinctUntilChanged(isEqual)).subscribe(apiIds => {
      const ids = apiIds ?? [];
      const current = this.selectedOrderedApis();
      this.selectedOrderedApis.set(current.filter(a => ids.includes(a.id)));
    });

    const disabledSet = new Set<string>([]);

    this.rows$ = combineLatest([
      this.filters$.pipe(debounceTime(100), distinctUntilChanged(isEqual)),
      this.refresh$.pipe(startWith(undefined)),
    ]).pipe(
      tap(() => (this.isLoading = true)),
      switchMap(([filters]) => this.searchApis(filters)),
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
      startWith([]),
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
        visibility: formValues.visibility,
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

  private searchApis(filters: GioTableWrapperFilters): Observable<ApisResponse> {
    return new Observable<ApisResponse>(subscriber => {
      const sub = this.apiService
        .search({ query: filters.searchTerm }, undefined, filters.pagination.index, filters.pagination.size, false)
        .subscribe({
          next: resp => subscriber.next(resp),
          error: () => {
            this.total = 0;
            subscriber.next({ data: [], pagination: undefined, links: undefined });
            subscriber.complete();
          },
          complete: () => subscriber.complete(),
        });

      return () => {
        sub.unsubscribe();
        this.isLoading = false;
      };
    }).pipe(finalize(() => (this.isLoading = false)));
  }
}

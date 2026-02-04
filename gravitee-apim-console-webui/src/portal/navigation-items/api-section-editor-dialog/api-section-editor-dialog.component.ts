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
import { Component, DestroyRef, HostListener, inject, OnInit, computed, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEqual } from 'lodash';
import { Observable, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, startWith, switchMap, tap } from 'rxjs/operators';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api, ApiV2, ApiV4, PortalVisibility, ApisResponse } from '../../../entities/management-api-v2';
import { getApiAccess } from '../../../shared/utils';

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
    MatPaginatorModule,
    MatChipsModule,
    AsyncPipe,
  ],
  templateUrl: './api-section-editor-dialog.component.html',
  styleUrls: ['./api-section-editor-dialog.component.scss'],
})
export class ApiSectionEditorDialogComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiService = inject(ApiV2Service);

  form!: ApiSectionForm;
  public initialFormValues: ApiSectionFormValues;

  public title = 'Add APIs';

  searchControl = new FormControl<string>('', { nonNullable: true });

  pageIndex = signal(0);
  pageSize = signal(10);
  total = signal(0);

  private readonly refresh$ = new Subject<void>();

  private selectedOrderedApis = signal<SelectedApi[]>([]);
  selectedCount = computed(() => this.selectedOrderedApis().length);
  selectedApis = computed(() => this.selectedOrderedApis());
  selectedIds = computed(() => new Set(this.selectedOrderedApis().map((a) => a.id)));

  displayedColumns = ['select', 'name', 'path', 'labels'];

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
    this.selectedOrderedApis.set(initialApiIds.map((id) => ({ id, name: id })));

    const disabledSet = new Set<string>([]);

    this.rows$ = this.searchControl.valueChanges.pipe(
      startWith(this.searchControl.value),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((query) =>
        this.refresh$.pipe(
          startWith(undefined),
          switchMap(() => this.apiService.search({ query }, undefined, this.pageIndex() + 1, this.pageSize(), false)),
          catchError((): Observable<ApisResponse> => {
            this.total.set(0);
            return of({ data: [], pagination: undefined, links: undefined });
          }),
        ),
      ),
      tap((resp) => {
        this.total.set(resp.pagination?.totalCount ?? 0);
      }),
      map((resp) =>
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
      takeUntilDestroyed(this.destroyRef),
    );

    this.initialFormValues = this.form.getRawValue();
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.pageIndex.set(0);
    this.refresh$.next();
  }

  isChecked(apiId: string): boolean {
    return this.selectedIds().has(apiId);
  }

  toggle(apiId: string, checked: boolean, apiName?: string): void {
    const current = this.selectedOrderedApis();

    if (checked) {
      if (!current.some((a) => a.id === apiId)) {
        const next = [...current, { id: apiId, name: apiName ?? apiId }];
        this.selectedOrderedApis.set(next);
        this.form.controls.apiIds.setValue(next.map((a) => a.id));
      }
    } else {
      const next = current.filter((a) => a.id !== apiId);
      this.selectedOrderedApis.set(next);
      this.form.controls.apiIds.setValue(next.map((a) => a.id));
    }
  }

  removeSelected(apiId: string): void {
    this.toggle(apiId, false);
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.refresh$.next();
  }

  trackSelectedById(_index: number, api: SelectedApi): string {
    return api.id;
  }

  toggleSelectedPanel(): void {
    this.selectedPanelExpanded.set(!this.selectedPanelExpanded());
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

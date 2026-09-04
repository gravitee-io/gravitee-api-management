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
import { Component, computed, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxChange, MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { isEqual } from 'lodash';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';

import { Api, PortalNavigationItem, PortalVisibility } from '../../../entities/management-api-v2';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { getPublicVisibilityDisabledTooltip, isPublicVisibilityDisabled } from '../visibility-toggle.util';

/** The navigation item types that are picked from a catalog of existing entities rather than authored inline. */
export type SectionEntityKind = 'AGENT' | 'API_PRODUCT';

export interface SectionEntityPickerDialogData {
  kind: SectionEntityKind;
  parentItem: PortalNavigationItem;
  existingIds?: string[];
}

export interface SelectedSectionEntity {
  id: string;
  name: string;
}

export interface SectionEntityPickerDialogResult {
  visibility: PortalVisibility;
  entities: SelectedSectionEntity[];
}

interface SectionEntityRow extends SelectedSectionEntity {
  version: string;
  description: string;
  apiCount: number;
  isDisabled: boolean;
}

interface SectionEntityPage {
  rows: Omit<SectionEntityRow, 'isDisabled'>[];
  totalCount: number;
}

interface SectionEntityKindConfig {
  title: string;
  pluralLabel: string;
  singularLabel: string;
  description: string;
  nameColumnHeader: string;
  showApiCount: boolean;
  search: (filters: GioTableWrapperFilters) => Observable<SectionEntityPage>;
}

interface SectionEntityFormControls {
  entityIds: FormControl<string[]>;
  isPrivate: FormControl<boolean>;
}

interface SectionEntityFormValues {
  entityIds: string[];
  isPrivate: boolean;
}

const EMPTY_PAGE: SectionEntityPage = { rows: [], totalCount: 0 };

@Component({
  selector: 'section-entity-picker-dialog',
  imports: [
    MatDialogModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatIconModule,
    MatCheckboxModule,
    MatTableModule,
    MatChipsModule,
    MatExpansionModule,
    AsyncPipe,
    GioTableWrapperModule,
  ],
  templateUrl: './section-entity-picker-dialog.component.html',
  styleUrls: ['./section-entity-picker-dialog.component.scss'],
})
export class SectionEntityPickerDialogComponent implements OnInit {
  private readonly apiService = inject(ApiV2Service);
  private readonly apiProductService = inject(ApiProductV2Service);
  private readonly dialogData = inject<SectionEntityPickerDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SectionEntityPickerDialogComponent, SectionEntityPickerDialogResult>);

  form!: FormGroup<SectionEntityFormControls>;
  initialFormValues!: SectionEntityFormValues;
  rows$!: Observable<SectionEntityRow[]>;
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  readonly isLoading = signal(true);
  readonly hasLoadError = signal(false);
  readonly selectedPanelExpanded = signal(true);
  readonly total = signal(0);
  readonly selectedEntities = signal<SelectedSectionEntity[]>([]);

  readonly selectedCount = computed(() => this.selectedEntities().length);
  readonly publicDisabled = computed(() => isPublicVisibilityDisabled(this.dialogData.parentItem));
  readonly publicDisabledTooltip = computed(() => getPublicVisibilityDisabledTooltip(this.dialogData.parentItem));

  private readonly filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  readonly config: SectionEntityKindConfig = this.configFor(this.dialogData.kind);
  readonly displayedColumns = this.config.showApiCount
    ? ['select', 'name', 'version', 'description', 'apiCount']
    : ['select', 'name', 'version', 'description'];

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent): void {
    if (!this.formIsUnchanged()) {
      event.preventDefault();
    }
  }

  ngOnInit(): void {
    this.form = new FormGroup<SectionEntityFormControls>({
      entityIds: new FormControl<string[]>([], {
        validators: [Validators.required],
        nonNullable: true,
      }),
      isPrivate: new FormControl<boolean>(false, {
        nonNullable: true,
      }),
    });

    const disabledIds = new Set(this.dialogData.existingIds ?? []);

    this.rows$ = this.filters$.pipe(
      debounceTime(100),
      distinctUntilChanged(isEqual),
      tap(() => {
        this.isLoading.set(true);
        this.hasLoadError.set(false);
      }),
      switchMap(filters =>
        this.config.search(filters).pipe(
          catchError(() => {
            this.hasLoadError.set(true);
            return of(EMPTY_PAGE);
          }),
        ),
      ),
      tap(page => {
        this.isLoading.set(false);
        this.total.set(page.totalCount);
      }),
      map(page => page.rows.map(row => ({ ...row, isDisabled: disabledIds.has(row.id) }))),
    );

    this.syncVisibilityControlState();
    this.initialFormValues = this.form.getRawValue();
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  isChecked(entityId: string): boolean {
    return this.form.controls.entityIds.value.includes(entityId);
  }

  onSelectionChange(entity: SelectedSectionEntity, event: MatCheckboxChange): void {
    if (event.checked) {
      this.addEntity({ id: entity.id, name: entity.name });
      return;
    }

    this.removeEntity(entity.id);
  }

  removeSelected(entityId: string): void {
    this.removeEntity(entityId);
  }

  onSubmit(): void {
    if (!this.form.valid) {
      return;
    }

    const formValues = this.form.getRawValue();
    this.dialogRef.close({
      visibility: formValues.isPrivate ? 'PRIVATE' : 'PUBLIC',
      entities: this.selectedEntities(),
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  formIsUnchanged(): boolean {
    return isEqual(this.form.getRawValue(), this.initialFormValues);
  }

  private addEntity(entity: SelectedSectionEntity): void {
    const currentIds = this.form.controls.entityIds.value;
    if (currentIds.includes(entity.id)) {
      return;
    }

    this.form.controls.entityIds.setValue([...currentIds, entity.id]);
    this.selectedEntities.update(entities => [...entities, entity]);
  }

  private removeEntity(entityId: string): void {
    const currentIds = this.form.controls.entityIds.value;
    if (!currentIds.includes(entityId)) {
      return;
    }

    this.form.controls.entityIds.setValue(currentIds.filter(id => id !== entityId));
    this.selectedEntities.update(entities => entities.filter(entity => entity.id !== entityId));
  }

  private syncVisibilityControlState(): void {
    const isPrivateControl = this.form.controls.isPrivate;

    if (this.publicDisabled()) {
      isPrivateControl.setValue(true, { emitEvent: false });
      isPrivateControl.disable({ emitEvent: false });
      return;
    }

    isPrivateControl.enable({ emitEvent: false });
  }

  private configFor(kind: SectionEntityKind): SectionEntityKindConfig {
    if (kind === 'AGENT') {
      return {
        title: 'Add Agents',
        pluralLabel: 'Agents',
        singularLabel: 'Agent',
        description: 'Pick the A2A proxy agents you want to add to your navigation menu.',
        nameColumnHeader: 'Agent name',
        showApiCount: false,
        search: filters =>
          this.apiService
            .search(
              { query: filters.searchTerm, apiTypes: ['V4_A2A_PROXY'] },
              undefined,
              filters.pagination.index,
              filters.pagination.size,
              false,
            )
            .pipe(
              map(response => ({
                totalCount: response.pagination?.totalCount ?? 0,
                rows: (response.data ?? []).map((api: Api) => ({
                  id: api.id,
                  name: api.name,
                  version: api.apiVersion ?? '',
                  description: api.description ?? '',
                  apiCount: 0,
                })),
              })),
            ),
      };
    }

    return {
      title: 'Add API Products',
      pluralLabel: 'API Products',
      singularLabel: 'API Product',
      description: 'Pick the API Products you want to add to your navigation menu.',
      nameColumnHeader: 'Product name',
      showApiCount: true,
      search: filters =>
        this.apiProductService.search({ query: filters.searchTerm }, undefined, filters.pagination.index, filters.pagination.size).pipe(
          map(response => ({
            totalCount: response.pagination?.totalCount ?? 0,
            rows: (response.data ?? []).map((apiProduct: ApiProduct) => ({
              id: apiProduct.id,
              name: apiProduct.name,
              version: apiProduct.version,
              description: apiProduct.description ?? '',
              apiCount: apiProduct.apiIds?.length ?? 0,
            })),
          })),
        ),
    };
  }
}

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

import { ApiProduct, ApiProductsResponse } from '../../../entities/management-api-v2/api-product';
import { PortalNavigationItem, PortalVisibility } from '../../../entities/management-api-v2';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { getPublicVisibilityDisabledTooltip, isPublicVisibilityDisabled } from '../visibility-toggle.util';

export interface ApiProductSectionEditorDialogData {
  mode: 'create';
  parentItem: PortalNavigationItem;
  existingApiProductIds?: string[];
}

export interface SelectedApiProduct {
  id: string;
  name: string;
}

export interface ApiProductSectionEditorDialogResult {
  visibility: PortalVisibility;
  apiProducts: SelectedApiProduct[];
}

type ApiProductRow = SelectedApiProduct & {
  version: string;
  description: string;
  apiCount: number;
  isDisabled: boolean;
};

interface ApiProductSectionFormControls {
  apiProductIds: FormControl<string[]>;
  isPrivate: FormControl<boolean>;
}

interface ApiProductSectionFormValues {
  apiProductIds: string[];
  isPrivate: boolean;
}

type ApiProductSectionForm = FormGroup<ApiProductSectionFormControls>;

@Component({
  selector: 'api-product-section-editor-dialog',
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
  templateUrl: './api-product-section-editor-dialog.component.html',
  styleUrls: ['./api-product-section-editor-dialog.component.scss'],
})
export class ApiProductSectionEditorDialogComponent implements OnInit {
  private readonly apiProductService = inject(ApiProductV2Service);
  private readonly dialogData = inject<ApiProductSectionEditorDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ApiProductSectionEditorDialogComponent, ApiProductSectionEditorDialogResult>);

  form!: ApiProductSectionForm;
  initialFormValues!: ApiProductSectionFormValues;
  readonly title = 'Add API Products';
  readonly displayedColumns = ['select', 'name', 'version', 'description', 'apiCount'];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  total = 0;
  readonly isLoading = signal(true);
  readonly hasLoadError = signal(false);
  readonly selectedPanelExpanded = signal(true);

  private readonly filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private readonly selectedOrderedApiProducts = signal<SelectedApiProduct[]>([]);

  readonly selectedCount = computed(() => this.selectedOrderedApiProducts().length);
  readonly selectedApiProducts = computed(() => this.selectedOrderedApiProducts());
  readonly publicDisabled = computed(() => isPublicVisibilityDisabled(this.dialogData.parentItem));
  readonly publicDisabledTooltip = computed(() => getPublicVisibilityDisabledTooltip(this.dialogData.parentItem));

  rows$!: Observable<ApiProductRow[]>;

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent): void {
    if (!this.formIsUnchanged()) {
      event.preventDefault();
    }
  }

  ngOnInit(): void {
    this.form = new FormGroup<ApiProductSectionFormControls>({
      apiProductIds: new FormControl<string[]>([], {
        validators: [Validators.required],
        nonNullable: true,
      }),
      isPrivate: new FormControl<boolean>(false, {
        nonNullable: true,
      }),
    });

    const disabledApiProductIds = new Set(this.dialogData.existingApiProductIds ?? []);

    this.rows$ = this.filters$.pipe(
      debounceTime(100),
      distinctUntilChanged(isEqual),
      tap(() => {
        this.isLoading.set(true);
        this.hasLoadError.set(false);
      }),
      switchMap(filters =>
        this.apiProductService.search({ query: filters.searchTerm }, undefined, filters.pagination.index, filters.pagination.size).pipe(
          catchError((): Observable<ApiProductsResponse> => {
            this.hasLoadError.set(true);
            return of({ data: [], pagination: undefined, links: undefined });
          }),
        ),
      ),
      tap(response => {
        this.isLoading.set(false);
        this.total = response.pagination?.totalCount ?? 0;
      }),
      map(response =>
        (response.data ?? []).map((apiProduct: ApiProduct) => ({
          id: apiProduct.id,
          name: apiProduct.name,
          version: apiProduct.version,
          description: apiProduct.description ?? '',
          apiCount: apiProduct.apiIds?.length ?? 0,
          isDisabled: disabledApiProductIds.has(apiProduct.id),
        })),
      ),
    );

    this.syncVisibilityControlState();
    this.initialFormValues = this.form.getRawValue();
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  isChecked(apiProductId: string): boolean {
    return this.form.controls.apiProductIds.value.includes(apiProductId);
  }

  onApiProductSelectionChange(apiProduct: SelectedApiProduct, event: MatCheckboxChange): void {
    if (event.checked) {
      this.addApiProduct({ id: apiProduct.id, name: apiProduct.name });
      return;
    }

    this.removeApiProduct(apiProduct.id);
  }

  removeSelected(apiProductId: string): void {
    this.removeApiProduct(apiProductId);
  }

  onSubmit(): void {
    if (!this.form.valid) {
      return;
    }

    const formValues = this.form.getRawValue();
    this.dialogRef.close({
      visibility: formValues.isPrivate ? 'PRIVATE' : 'PUBLIC',
      apiProducts: this.selectedOrderedApiProducts(),
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  formIsUnchanged(): boolean {
    return isEqual(this.form.getRawValue(), this.initialFormValues);
  }

  private addApiProduct(apiProduct: SelectedApiProduct): void {
    const currentApiProductIds = this.form.controls.apiProductIds.value;
    if (currentApiProductIds.includes(apiProduct.id)) {
      return;
    }

    this.form.controls.apiProductIds.setValue([...currentApiProductIds, apiProduct.id]);
    this.selectedOrderedApiProducts.update(selectedProducts => [...selectedProducts, apiProduct]);
  }

  private removeApiProduct(apiProductId: string): void {
    const currentApiProductIds = this.form.controls.apiProductIds.value;
    if (!currentApiProductIds.includes(apiProductId)) {
      return;
    }

    this.form.controls.apiProductIds.setValue(currentApiProductIds.filter(id => id !== apiProductId));
    this.selectedOrderedApiProducts.update(selectedProducts => selectedProducts.filter(product => product.id !== apiProductId));
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
}

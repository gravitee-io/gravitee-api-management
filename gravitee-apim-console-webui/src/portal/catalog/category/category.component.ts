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
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { BehaviorSubject, EMPTY, forkJoin, Observable, of } from 'rxjs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, filter, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { chunk } from 'lodash';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  GIO_DIALOG_WIDTH,
  GioAvatarModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormFilePickerModule,
  GioFormSlideToggleModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { AsyncPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog } from '@angular/material/dialog';

import {
  AddApiToCategoryDialogComponent,
  AddApiToCategoryDialogData,
  AddApiToCategoryDialogResult,
} from './add-api-to-category-dialog/add-api-to-category-dialog.component';
import {
  AddApiProductToCategoryDialogComponent,
  AddApiProductToCategoryDialogData,
  AddApiProductToCategoryDialogResult,
  ApiProductCategoryCandidate,
} from './add-api-product-to-category-dialog/add-api-product-to-category-dialog.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PortalCategoryService } from '../../../services-ngx/portal-category.service';
import { PortalNavigationItemService } from '../../../services-ngx/portal-navigation-item.service';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import {
  CreatePortalCategory,
  PortalCategory,
  PortalNavigationApi,
  PortalNavigationApiProduct,
  PortalNavigationItemApiSummary,
  PortalNavigationItemsResponse,
  UpdateApiPortalNavigationItem,
  UpdateApiProductPortalNavigationItem,
  UpdatePortalCategory,
} from '../../../entities/management-api-v2';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';

interface ApiVM {
  id: string;
  name: string;
  version: string;
  contextPath: string;
  navigationItem: PortalNavigationApi;
}

interface ApiProductVM extends ApiProductCategoryCandidate {
  id: string;
}

@Component({
  selector: 'category',
  imports: [
    // Angular Modules
    AsyncPipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,

    // Gravitee UI Particles Modules & Components
    GioAvatarModule,
    GioFormFilePickerModule,
    GioFormSlideToggleModule,
    GioGoBackButtonModule,
    GioPermissionModule,
    GioSaveBarModule,

    // Angular Material Modules
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './category.component.html',
  styleUrl: './category.component.scss',
})
export class CategoryCatalogComponent implements OnInit {
  mode: 'new' | 'edit' = 'new';

  categoryDetails: FormGroup<{
    title: FormControl<string>;
    description: FormControl<string>;
    visible: FormControl<boolean>;
  }>;
  categoryDetailsInitialValue: unknown;

  category$: Observable<PortalCategory>;
  apis$: Observable<ApiVM[]>;
  apiProducts$: Observable<ApiProductVM[]>;

  displayedColumns = ['name', 'version', 'contextPath', 'actions'];
  apiProductDisplayedColumns = ['name', 'version', 'actions'];
  readonly isLoadingApiProductCandidates = signal(false);

  private refreshData = new BehaviorSubject(1);
  private refreshNavigationItems = new BehaviorSubject(1);
  private destroyRef = inject(DestroyRef);
  private readonly portalNavigationItemService = inject(PortalNavigationItemService);
  private readonly apiProductService = inject(ApiProductV2Service);
  private readonly matDialog = inject(MatDialog);
  private navigationItemsResponse$: Observable<PortalNavigationItemsResponse>;

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private portalCategoryService: PortalCategoryService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit() {
    this.category$ = this.refreshData.pipe(
      switchMap(_ => this.activatedRoute.params),
      switchMap(({ categoryId }) => {
        if (!!categoryId && categoryId !== 'new') {
          this.mode = 'edit';
          return this.portalCategoryService.list().pipe(
            map(categories => categories.find(category => category.id === categoryId)),
            tap(category => {
              if (!category) {
                this.snackBarService.error('Category not found');
                this.router.navigate(['..', '..'], { relativeTo: this.activatedRoute });
              }
            }),
            filter(category => !!category),
          );
        }
        return of({} as PortalCategory);
      }),
      tap(category => {
        this.categoryDetails = new FormGroup({
          title: new FormControl<string>(category.title, { validators: Validators.required }),
          description: new FormControl<string>(category.description),
          visible: new FormControl<boolean>(category.visible ?? true),
        });
        this.categoryDetailsInitialValue = this.categoryDetails.getRawValue();
        this.handleReadOnly();
      }),
    );

    this.navigationItemsResponse$ = this.refreshNavigationItems.pipe(
      switchMap(() =>
        this.mode === 'edit'
          ? this.portalNavigationItemService.getNavigationItems('TOP_NAVBAR', ['apis']).pipe(
              catchError(({ error }) => {
                this.snackBarService.error(error?.message ?? 'Unable to load navigation items');
                return of({ items: [] } satisfies PortalNavigationItemsResponse);
              }),
            )
          : of({ items: [] } satisfies PortalNavigationItemsResponse),
      ),
      shareReplay({ bufferSize: 1, refCount: true }),
    );

    this.apis$ = this.navigationItemsResponse$.pipe(
      map(response =>
        response.items
          .filter((item): item is PortalNavigationApi => item.type === 'API')
          .filter(navItem => (navItem.categoryIds ?? []).includes(this.categoryId))
          .map(navItem => this.toApiVM(navItem, response.metadata?.apis?.[navItem.id]))
          .filter((apiVM): apiVM is ApiVM => !!apiVM),
      ),
    );

    this.apiProducts$ = this.navigationItemsResponse$.pipe(
      map(response =>
        response.items
          .filter((item): item is PortalNavigationApiProduct => item.type === 'API_PRODUCT')
          .filter(navItem => (navItem.categoryIds ?? []).includes(this.categoryId)),
      ),
      switchMap(navItems =>
        this.toApiProductVMs$(navItems).pipe(
          catchError(({ error }) => {
            this.snackBarService.error(error?.message ?? 'Unable to load API Products');
            return of([]);
          }),
        ),
      ),
    );
  }

  onSubmit() {
    of(this.mode)
      .pipe(
        switchMap(mode => (mode === 'edit' ? this.updateCategory$() : this.createCategory$())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: category => {
          this.snackBarService.success(`Category [${category.title}] successfully ${this.mode === 'new' ? 'created' : 'updated'}.`);
          if (this.mode === 'new') {
            this.router.navigate(['..', category.id], { relativeTo: this.activatedRoute });
          } else {
            this.refreshData.next(1);
          }
        },
        error: ({ error }) => this.snackBarService.error(error.message),
      });
  }

  addApiToCategory(): void {
    this.selectableApiNavigationItems$()
      .pipe(
        switchMap(candidates =>
          this.matDialog
            .open<AddApiToCategoryDialogComponent, AddApiToCategoryDialogData, AddApiToCategoryDialogResult>(
              AddApiToCategoryDialogComponent,
              {
                data: { title: 'Add API to Category', candidates },
                width: GIO_DIALOG_WIDTH.SMALL,
              },
            )
            .afterClosed(),
        ),
        filter(navItem => !!navItem),
        switchMap(navItem =>
          this.portalNavigationItemService.updateNavigationItem(
            navItem.id,
            this.toUpdatePayload(navItem, [...(navItem.categoryIds ?? []), this.categoryId]),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: navItem => {
          this.snackBarService.success(`API [${navItem.title}] has been added to the category.`);
          this.refreshNavigationItems.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ?? 'Error during API addition'),
      });
  }

  removeApiFromCategory(apiVM: ApiVM): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Remove API',
          content: `Are you sure you want to remove API '${apiVM.name}' from this category?`,
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => !!confirmed),
        switchMap(() =>
          this.portalNavigationItemService.updateNavigationItem(
            apiVM.navigationItem.id,
            this.toUpdatePayload(
              apiVM.navigationItem,
              (apiVM.navigationItem.categoryIds ?? []).filter(categoryId => categoryId !== this.categoryId),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success(`'${apiVM.name}' removed successfully`);
          this.refreshNavigationItems.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ?? 'Error during API removal'),
      });
  }

  addApiProductToCategory(): void {
    if (this.isLoadingApiProductCandidates()) {
      return;
    }
    this.isLoadingApiProductCandidates.set(true);

    this.selectableApiProductCandidates$()
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Unable to load API Products');
          return EMPTY;
        }),
        switchMap(candidates =>
          this.matDialog
            .open<AddApiProductToCategoryDialogComponent, AddApiProductToCategoryDialogData, AddApiProductToCategoryDialogResult>(
              AddApiProductToCategoryDialogComponent,
              {
                data: { title: 'Add API Product to Category', candidates },
                width: GIO_DIALOG_WIDTH.SMALL,
              },
            )
            .afterClosed(),
        ),
        filter(candidate => !!candidate),
        switchMap(candidate => {
          const navigationItem = candidate.navigationItem;
          return this.portalNavigationItemService
            .updateNavigationItem(
              navigationItem.id,
              this.toApiProductUpdatePayload(navigationItem, [...(navigationItem.categoryIds ?? []), this.categoryId]),
            )
            .pipe(map(() => candidate.name));
        }),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingApiProductCandidates.set(false)),
      )
      .subscribe({
        next: apiProductName => {
          this.snackBarService.success(`API Product [${apiProductName}] has been added to the category.`);
          this.refreshNavigationItems.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ?? 'Error during API Product addition'),
      });
  }

  removeApiProductFromCategory(apiProductVM: ApiProductVM): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Remove API Product',
          content: `Are you sure you want to remove API Product '${apiProductVM.name}' from this category?`,
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => !!confirmed),
        switchMap(() =>
          this.portalNavigationItemService.updateNavigationItem(
            apiProductVM.navigationItem.id,
            this.toApiProductUpdatePayload(
              apiProductVM.navigationItem,
              (apiProductVM.navigationItem.categoryIds ?? []).filter(categoryId => categoryId !== this.categoryId),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success(`'${apiProductVM.name}' removed successfully`);
          this.refreshNavigationItems.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ?? 'Error during API Product removal'),
      });
  }

  private updateCategory$(): Observable<PortalCategory> {
    const categoryId = this.activatedRoute.snapshot.params.categoryId;
    const newValues = this.categoryDetails.getRawValue();
    const categoryToUpdate: UpdatePortalCategory = {
      title: newValues.title,
      description: newValues.description,
      visible: newValues.visible,
    };
    return this.portalCategoryService.update(categoryId, categoryToUpdate);
  }

  private createCategory$(): Observable<PortalCategory> {
    const newValues = this.categoryDetails.getRawValue();
    const newCategory: CreatePortalCategory = {
      title: newValues.title,
      description: newValues.description,
      visible: newValues.visible,
    };

    return this.portalCategoryService.create(newCategory);
  }

  private handleReadOnly(): void {
    // User cannot change category details
    if (!this.permissionService.hasAnyMatching(['environment-category-u'])) {
      this.categoryDetails.disable();
    }
  }

  private get categoryId(): string {
    return this.activatedRoute.snapshot.params.categoryId;
  }

  private selectableApiNavigationItems$(): Observable<PortalNavigationApi[]> {
    return this.portalNavigationItemService.getNavigationItems('TOP_NAVBAR').pipe(
      map(response => response.items.filter((item): item is PortalNavigationApi => item.type === 'API' && item.published)),
      map(navItems => navItems.filter(navItem => !(navItem.categoryIds ?? []).includes(this.categoryId))),
    );
  }

  private selectableApiProductCandidates$(): Observable<ApiProductCategoryCandidate[]> {
    return this.portalNavigationItemService.getNavigationItems('TOP_NAVBAR').pipe(
      map(response =>
        response.items
          .filter((item): item is PortalNavigationApiProduct => item.type === 'API_PRODUCT' && item.published)
          .filter(navItem => !(navItem.categoryIds ?? []).includes(this.categoryId)),
      ),
      switchMap(navItems => this.toApiProductVMs$(navItems)),
      map(apiProducts =>
        apiProducts.map(({ navigationItem, name, version }) => ({
          navigationItem,
          name,
          version,
        })),
      ),
    );
  }

  private toApiProductVMs$(navItems: PortalNavigationApiProduct[]): Observable<ApiProductVM[]> {
    return this.findApiProductsByIds$(navItems.map(navItem => navItem.apiProductId)).pipe(
      map(apiProductsById =>
        navItems
          .map(navItem => {
            const apiProduct = apiProductsById.get(navItem.apiProductId);
            return apiProduct
              ? {
                  id: apiProduct.id,
                  name: apiProduct.name,
                  version: apiProduct.version,
                  navigationItem: navItem,
                }
              : null;
          })
          .filter((apiProductVM): apiProductVM is ApiProductVM => !!apiProductVM),
      ),
    );
  }

  private findApiProductsByIds$(apiProductIds: string[]): Observable<Map<string, ApiProduct>> {
    const idBatches = chunk([...new Set(apiProductIds)], 100);
    if (idBatches.length === 0) {
      return of(new Map());
    }

    return forkJoin(idBatches.map(ids => this.apiProductService.search({ ids }, undefined, 1, 100))).pipe(
      map(responses => new Map(responses.flatMap(response => response.data ?? []).map(apiProduct => [apiProduct.id, apiProduct] as const))),
    );
  }

  private toApiVM(navItem: PortalNavigationApi, summary: PortalNavigationItemApiSummary | undefined): ApiVM | null {
    if (!summary) {
      return null;
    }
    return {
      id: summary.id,
      name: summary.name,
      version: summary.apiVersion,
      contextPath: summary.contextPath ?? '',
      navigationItem: navItem,
    };
  }

  private toUpdatePayload(navItem: PortalNavigationApi, categoryIds: string[]): UpdateApiPortalNavigationItem {
    return {
      type: 'API',
      title: navItem.title,
      published: navItem.published,
      visibility: navItem.visibility,
      parentId: navItem.parentId,
      order: navItem.order,
      apiId: navItem.apiId,
      categoryIds,
    };
  }

  private toApiProductUpdatePayload(navItem: PortalNavigationApiProduct, categoryIds: string[]): UpdateApiProductPortalNavigationItem {
    return {
      type: 'API_PRODUCT',
      title: navItem.title,
      published: navItem.published,
      visibility: navItem.visibility,
      parentId: navItem.parentId,
      order: navItem.order,
      categoryIds,
    };
  }
}

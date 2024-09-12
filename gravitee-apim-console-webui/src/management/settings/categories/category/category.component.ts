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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, EMPTY, Observable, of, switchMap } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData, NewFile } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { isEmpty } from 'lodash';

import { Page, PageType } from '../../../../entities/page';
import { Category } from '../../../../entities/category/Category';
import { CategoryService } from '../../../../services-ngx/category.service';
import { DocumentationService } from '../../../../services-ngx/documentation.service';
import { NewCategory } from '../../../../entities/category/NewCategory';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UpdateCategory } from '../../../../entities/category/UpdateCategory';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiService } from '../../../../services-ngx/api.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { CategoryV2Service } from '../../../../services-ngx/category-v2.service';
import {
  GioApiSelectDialogComponent,
  GioApiSelectDialogData,
  GioApiSelectDialogResult,
} from '../../../../shared/components/gio-api-select-dialog/gio-api-select-dialog.component';
import { EnvironmentSettingsService } from '../../../../services-ngx/environment-settings.service';

interface ApiVM {
  id: string;
  name: string;
  version: string;
  contextPath: string;
  order: number;
  disableMoveUp: boolean;
  disableMoveDown: boolean;
}

@Component({
  selector: 'app-category',
  templateUrl: './category.component.html',
  styleUrl: './category.component.scss',
})
export class CategoryComponent implements OnInit {
  mode: 'new' | 'edit' = 'new';

  categoryDetails: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
    hidden: FormControl<boolean>;
    page: FormControl<string>;
    picture: FormControl<unknown[]>;
    background: FormControl<unknown[]>;
    highlightApi: FormControl<string>;
  }>;
  highlightApiControl: FormControl<string>;
  categoryDetailsInitialValue: unknown;
  hasPortalNextEnabled: boolean = false;

  category$: Observable<Category>;
  pages$: Observable<Page[]>;
  apis$: Observable<ApiVM[]>;

  displayedColumns = ['name', 'version', 'contextPath', 'actions'];

  private refreshData = new BehaviorSubject(1);
  private category = new BehaviorSubject<Category>(null);
  private destroyRef = inject(DestroyRef);

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private categoryService: CategoryService,
    private categoryV2Service: CategoryV2Service,
    private documentationService: DocumentationService,
    private apiService: ApiService,
    private apiV2Service: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private matDialog: MatDialog,
    private environmentSettingsService: EnvironmentSettingsService,
  ) {}

  ngOnInit() {
    this.category$ = this.refreshData.pipe(
      switchMap((_) => this.activatedRoute.params),
      switchMap(({ categoryId }) => {
        if (!!categoryId && categoryId !== 'new') {
          this.mode = 'edit';
          return this.categoryService.get(categoryId);
        }
        return of({} as Category);
      }),
      tap((category) => {
        this.category.next(category);

        this.highlightApiControl = new FormControl<string>(category.highlightApi);
        this.categoryDetails = new FormGroup({
          name: new FormControl<string>(category.name, { validators: Validators.required }),
          description: new FormControl<string>(category.description),
          hidden: new FormControl<boolean>(category.hidden != null ? category.hidden : false),
          page: new FormControl<string>(category.page),
          picture: new FormControl<unknown[]>(category.picture_url ? [category.picture_url] : []),
          background: new FormControl<unknown[]>(category.background_url ? [category.background_url] : []),
          highlightApi: this.highlightApiControl,
        });
        this.categoryDetailsInitialValue = this.categoryDetails.getRawValue();
        this.handleReadOnly();
      }),
    );

    this.apis$ = this.category.pipe(
      filter((category) => !isEmpty(category)),
      switchMap((category) => this.categoryV2Service.getApis(category.id)),
      map((pagedResult) => {
        return pagedResult.data.map((api) => ({
          id: api.id,
          name: api.name,
          version: api.apiVersion,
          contextPath: api.accessPaths?.join(','),
          order: api.order,
          disableMoveDown: api.order >= pagedResult.pagination.totalCount - 1,
          disableMoveUp: api.order <= 0,
        }));
      }),
    );

    this.pages$ = this.refreshData.pipe(switchMap((_) => this.documentationService.listPortalPages(PageType.MARKDOWN, true)));

    this.hasPortalNextEnabled = this.environmentSettingsService.getSnapshot().portalNext?.access?.enabled === true;
  }

  onSubmit() {
    of(this.mode)
      .pipe(
        switchMap((mode) => (mode === 'edit' ? this.updateCategory$() : this.createCategory$())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (category) => {
          this.snackBarService.success(`Category [${category.name}] successfully ${this.mode === 'new' ? 'created' : 'updated'}.`);
          if (this.mode === 'new') {
            this.router.navigate(['..', category.id], { relativeTo: this.activatedRoute });
          } else {
            this.refreshData.next(1);
          }
        },
        error: ({ error }) => this.snackBarService.error(error.message),
      });
  }

  private updateCategory$(): Observable<Category> {
    return this.categoryService.get(this.category.getValue().id).pipe(
      switchMap((category) => {
        const newValues = this.categoryDetails.getRawValue();
        const categoryToUpdate: UpdateCategory = {
          ...category,
          name: newValues.name,
          description: newValues.description,
          page: newValues.page,
          hidden: newValues.hidden,
          highlightApi: newValues.highlightApi,
        };

        if (newValues.picture.length && newValues.picture[0] instanceof NewFile) {
          categoryToUpdate.picture = (newValues.picture[0] as NewFile).dataUrl;
          categoryToUpdate.picture_url = undefined;
        } else if (newValues.picture.length === 0 && !!category.picture_url) {
          categoryToUpdate.picture_url = undefined;
        }

        if (newValues.background.length && newValues.background[0] instanceof NewFile) {
          categoryToUpdate.background = (newValues.background[0] as NewFile).dataUrl;
          categoryToUpdate.background_url = undefined;
        } else if (newValues.background.length === 0 && !!category.background_url) {
          categoryToUpdate.background_url = undefined;
        }

        return this.categoryService.update(categoryToUpdate);
      }),
    );
  }

  private createCategory$(): Observable<Category> {
    const newValues = this.categoryDetails.getRawValue();
    const newCategory: NewCategory = {
      name: newValues.name,
      description: newValues.description,
      page: newValues.page,
      hidden: newValues.hidden,
    };
    if (newValues.picture.length && newValues.picture[0] instanceof NewFile) {
      newCategory.picture = (newValues.picture[0] as NewFile).dataUrl;
    }

    if (newValues.background.length && newValues.background[0] instanceof NewFile) {
      newCategory.background = (newValues.background[0] as NewFile).dataUrl;
    }

    return this.categoryService.create(newCategory);
  }

  addApiToCategory(category: Category) {
    this.matDialog
      .open<GioApiSelectDialogComponent, GioApiSelectDialogData, GioApiSelectDialogResult>(GioApiSelectDialogComponent, {
        data: {
          title: 'Add API',
        },
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result?.id),
        switchMap(({ id }) => this.apiV2Service.get(id)),
        switchMap((api) => {
          if (api.categories?.includes(category.key)) {
            this.snackBarService.error(`API "${api.name}" is already defined in the category.`);
            return EMPTY;
          }
          const updatedCategories = api.categories ? [...api.categories, category.key] : [category.key];
          return this.apiV2Service.update(api.id, { ...api, categories: updatedCategories });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (api) => {
          this.snackBarService.success(`API [${api.name}] has been added to the category.`);
          this.category.next(category);
        },
        error: ({ error }) => this.snackBarService.error(error.message),
      });
  }

  removeApiFromCategory(api: ApiVM, category: Category) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Remove API',
          content: `Are you sure you want to remove API '${api.name}' from the category '${category.name}'?`,
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap((_) => this.apiV2Service.get(api.id)),
        switchMap((api) => {
          const categories = api.categories.filter((c) => c !== category.key);
          return this.apiV2Service.update(api.id, { ...api, categories });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success(`'${api.name}' removed successfully`);
          this.refreshData.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ?? 'Error during API removal'),
      });
  }

  removeHighlightedApi() {
    this.highlightApiControl.setValue(null);
    this.categoryDetails.markAsDirty();
  }

  addHighlightedApi(apiId: string) {
    this.highlightApiControl.setValue(apiId);
    this.categoryDetails.markAsDirty();
  }
  private handleReadOnly(): void {
    // User cannot change category details
    if (!this.permissionService.hasAnyMatching(['environment-category-u'])) {
      this.categoryDetails.disable();

      // User cannot edit an API either
      if (!this.permissionService.hasAnyMatching(['environment-api-u'])) {
        this.displayedColumns.pop();
      }
    }
  }

  moveCategoryApi(element: ApiVM, newOrder: number) {
    const category = this.category.getValue();
    this.categoryV2Service
      .updateCategoryApi(category.id, element.id, { order: newOrder })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('API order updated successfully.');
          this.category.next(category);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ? error.message : 'Error during API order update!'),
      });
  }
}

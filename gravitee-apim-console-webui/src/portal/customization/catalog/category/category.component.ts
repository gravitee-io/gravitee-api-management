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
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { BehaviorSubject, EMPTY, Observable, of, switchMap } from 'rxjs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { catchError, filter, map, tap } from 'rxjs/operators';
import { isEmpty } from 'lodash';
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
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption } from '@angular/material/autocomplete';
import { MatTooltip } from '@angular/material/tooltip';
import { CdkDrag, CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { MatIcon } from '@angular/material/icon';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import {
  GioApiSelectDialogComponent,
  GioApiSelectDialogData,
  GioApiSelectDialogResult,
} from '../../../../shared/components/gio-api-select-dialog/gio-api-select-dialog.component';
import { NewCategory } from '../../../../entities/category/NewCategory';
import { UpdateCategory } from '../../../../entities/category/UpdateCategory';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { CategoryV2Service } from '../../../../services-ngx/category-v2.service';
import { CategoryService } from '../../../../services-ngx/category.service';
import { Category } from '../../../../entities/category/Category';
import { PortalHeaderComponent } from '../../../components/header/portal-header.component';

interface ApiVM {
  id: string;
  name: string;
  version: string;
  contextPath: string;
  order: number;
}

@Component({
  selector: 'category',
  standalone: true,
  imports: [
    AsyncPipe,
    FormsModule,
    GioAvatarModule,
    GioFormFilePickerModule,
    GioFormSlideToggleModule,
    GioGoBackButtonModule,
    GioPermissionModule,
    GioSaveBarModule,
    MatButton,
    MatCard,
    MatCardContent,
    MatFormField,
    MatTableModule,
    MatInput,
    MatLabel,
    MatOption,
    MatTooltip,
    ReactiveFormsModule,
    RouterLink,
    PortalHeaderComponent,
    CdkDropList,
    CdkDrag,
    MatIcon,
  ],
  templateUrl: './category.component.html',
  styleUrl: './category.component.scss',
})
export class CategoryCatalogComponent implements OnInit {
  mode: 'new' | 'edit' = 'new';

  categoryDetails: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
  }>;
  categoryDetailsInitialValue: unknown;

  category$: Observable<Category>;
  apis$: Observable<ApiVM[]>;

  displayedColumns = ['order', 'name', 'version', 'contextPath', 'actions'];

  private refreshData = new BehaviorSubject(1);
  private category = new BehaviorSubject<Category>(null);
  private destroyRef = inject(DestroyRef);

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private categoryService: CategoryService,
    private categoryV2Service: CategoryV2Service,
    private apiV2Service: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private matDialog: MatDialog,
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

        this.categoryDetails = new FormGroup({
          name: new FormControl<string>(category.name, { validators: Validators.required }),
          description: new FormControl<string>(category.description),
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
        }));
      }),
      catchError((_) => of([])),
      tap((apis) => {
        if (apis.length < 2) {
          this.displayedColumns.shift();
        }
      }),
    );
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
        };
        return this.categoryService.update(categoryToUpdate);
      }),
    );
  }

  private createCategory$(): Observable<Category> {
    const newValues = this.categoryDetails.getRawValue();
    const newCategory: NewCategory = {
      name: newValues.name,
      description: newValues.description,
    };

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

  drop(event: CdkDragDrop<string>) {
    if (event.previousIndex !== event.currentIndex) {
      const category = this.category.getValue();
      this.categoryV2Service
        .updateCategoryApi(category.id, event.item.data.id, { order: event.currentIndex })
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
}

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
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { catchError, filter, map, switchMap, take, tap } from 'rxjs/operators';
import {
  GioAvatarModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormSlideToggleModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDrag, CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { AsyncPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { MatSelectModule } from '@angular/material/select';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { CategoryService } from '../../../services-ngx/category.service';
import { Category } from '../../../entities/category/Category';
import { BothPortalsBadgeComponent } from '../../components/portal-badge/both-portals-badge/both-portals-badge.component';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { PortalSettings } from '../../../entities/portal/portalSettings';

interface CatalogViewModeVM {
  value: string;
  label: string;
}

@Component({
  selector: 'category-list',
  imports: [
    // Angular Common & Forms
    AsyncPipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,

    // Gravitee UI Particles & Custom Components
    GioAvatarModule,
    GioFormSlideToggleModule,
    GioPermissionModule,
    GioSaveBarModule,
    BothPortalsBadgeComponent,

    // Angular Material
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatSelectModule,
    MatDialogModule,

    // Angular CDK
    CdkDropList,
    CdkDrag,
  ],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss',
})
export class CategoryListComponent implements OnInit {
  categoriesDS$: Observable<Category[]> = of([]);
  private categoryList = new BehaviorSubject(1);
  displayedColumns: string[] = ['order', 'name', 'description', 'count', 'actions'];
  portalCatalogViewMode: FormControl<string> = new FormControl();

  form: FormGroup<{ catalogViewMode: FormControl<string> }> = new FormGroup({
    catalogViewMode: this.portalCatalogViewMode,
  });
  initialValues: { catalogViewMode?: string };

  viewModes: CatalogViewModeVM[] = [
    {
      value: 'TABS',
      label: 'Tabs (Default)',
    },
    {
      value: 'CATEGORIES',
      label: 'Tiles',
    },
  ];

  private destroyRef = inject(DestroyRef);

  constructor(
    private categoryService: CategoryService,
    private readonly snackBarService: SnackBarService,
    private matDialog: MatDialog,
    private readonly permissionService: GioPermissionService,
    private readonly portalSettingsService: PortalSettingsService,
  ) {}

  ngOnInit(): void {
    // If read-only, remove actions column
    if (!this.permissionService.hasAnyMatching(['environment-category-u', 'environment-category-d'])) {
      this.displayedColumns.pop();
    }
    // If cannot update settings, disable form
    if (!this.permissionService.hasAnyMatching(['environment-settings-u'])) {
      this.form.disable();
    }

    this.categoriesDS$ = this.categoryList.pipe(
      switchMap(_ => this.categoryService.list(true)),
      map(categories => categories.sort((a, b) => a.order - b.order)),
      catchError(_ => of([])),
      tap(categories => {
        if (categories.length < 2) {
          this.displayedColumns.shift();
        }
      }),
    );
    this.portalSettingsService
      .get()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: settings => {
          this.initializeForm(settings);
        },
      });
  }

  deleteCategory(category: Category) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete Category',
          content: `Are you sure you want to delete the category '${category.name}'?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed),
        switchMap(_ => this.categoryService.delete(category.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: _ => {
          this.snackBarService.success(`'${category.name}' deleted successfully`);
          this.categoryList.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ?? 'Error during deletion'),
      });
  }

  showCategory(category: Category) {
    this.updateCategoryVisibility(false, category);
  }

  hideCategory(category: Category) {
    this.updateCategoryVisibility(true, category);
  }

  private updateCategoryVisibility(isHidden: boolean, category: Category): void {
    const categoryToUpdate: Category = { ...category, hidden: isHidden };
    this.categoryService
      .update(categoryToUpdate)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: _ => {
          this.snackBarService.success(`Category [${category.name}] is now ${isHidden ? 'hidden' : 'shown'}`);
          this.categoryList.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error.message),
      });
  }

  drop(event: CdkDragDrop<string>) {
    if (event.previousIndex !== event.currentIndex) {
      this.categoriesDS$
        .pipe(
          take(1),
          switchMap(categories => {
            const { previousIndex, currentIndex } = event;
            const categoryToMove = categories.splice(previousIndex, 1)[0];
            categories.splice(currentIndex, 0, categoryToMove);

            categories.forEach((category, index) => (category.order = index + 1));

            return this.categoryService.updateList(categories).pipe(
              tap(() => {
                this.categoryList.next(1);
                this.snackBarService.success(`Category order has been changed`);
              }),
            );
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          error: ({ error }) => this.snackBarService.error(error.message),
        });
    }
  }

  reset() {
    this.form.reset(this.initialValues);
  }

  submit() {
    this.portalSettingsService
      .get()
      .pipe(
        switchMap(settings =>
          this.portalSettingsService.save({
            ...settings,
            portalNext: {
              ...settings.portalNext,
              catalog: {
                viewMode: this.form.getRawValue().catalogViewMode,
              },
            },
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: settings => {
          this.initializeForm(settings);
        },
      });
  }

  private initializeForm(settings: PortalSettings): void {
    this.portalCatalogViewMode.setValue(settings.portalNext.catalog?.viewMode ?? 'TABS');
    this.initialValues = this.form.getRawValue();
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }
}

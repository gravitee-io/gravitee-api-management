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
import { catchError, filter, switchMap } from 'rxjs/operators';
import { GioAvatarModule, GioConfirmDialogComponent, GioConfirmDialogData, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { PortalCategoryService } from '../../../services-ngx/portal-category.service';
import { PortalCategory } from '../../../entities/management-api-v2';
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
    GioPermissionModule,
    GioSaveBarModule,

    // Angular Material
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatSelectModule,
    MatDialogModule,
  ],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss',
})
export class CategoryListComponent implements OnInit {
  categoriesDS$: Observable<PortalCategory[]> = of([]);
  private categoryList = new BehaviorSubject(1);
  displayedColumns: string[] = ['name', 'description', 'actions'];
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
    private portalCategoryService: PortalCategoryService,
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
      switchMap(_ => this.portalCategoryService.list()),
      catchError(_ => of([])),
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

  deleteCategory(category: PortalCategory) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete Category',
          content: `Are you sure you want to delete the category '${category.title}'?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed),
        switchMap(_ => this.portalCategoryService.delete(category.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: _ => {
          this.snackBarService.success(`'${category.title}' deleted successfully`);
          this.categoryList.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error?.message ?? 'Error during deletion'),
      });
  }

  showCategory(category: PortalCategory) {
    this.updateCategoryVisibility(true, category);
  }

  hideCategory(category: PortalCategory) {
    this.updateCategoryVisibility(false, category);
  }

  private updateCategoryVisibility(visible: boolean, category: PortalCategory): void {
    this.portalCategoryService
      .update(category.id, { title: category.title, description: category.description, visible })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: _ => {
          this.snackBarService.success(`Category [${category.title}] is now ${visible ? 'shown' : 'hidden'}`);
          this.categoryList.next(1);
        },
        error: ({ error }) => this.snackBarService.error(error.message),
      });
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

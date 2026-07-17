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
import { BehaviorSubject, Observable, of } from 'rxjs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GioAvatarModule, GioFormFilePickerModule, GioFormSlideToggleModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { AsyncPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PortalCategoryService } from '../../../services-ngx/portal-category.service';
import { CreatePortalCategory, PortalCategory, UpdatePortalCategory } from '../../../entities/management-api-v2';

interface ApiVM {
  id: string;
  name: string;
  version: string;
  contextPath: string;
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

  // API-to-category association is not yet supported by the Portal Next category API.
  // It will be reintroduced in a future story - for now the "APIs" table is always empty and read-only.
  readonly apiAssociationEnabled = false;

  categoryDetails: FormGroup<{
    title: FormControl<string>;
    description: FormControl<string>;
    visible: FormControl<boolean>;
  }>;
  categoryDetailsInitialValue: unknown;

  category$: Observable<PortalCategory>;
  apis$: Observable<ApiVM[]> = of([]);

  displayedColumns = ['name', 'version', 'contextPath'];

  private refreshData = new BehaviorSubject(1);
  private destroyRef = inject(DestroyRef);

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
}

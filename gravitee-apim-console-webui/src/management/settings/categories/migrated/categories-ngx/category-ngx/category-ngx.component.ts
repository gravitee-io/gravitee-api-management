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
import { BehaviorSubject, Observable, of, switchMap } from 'rxjs';
import { tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NewFile } from '@gravitee/ui-particles-angular';

import { Page, PageType } from '../../../../../../entities/page';
import { Category } from '../../../../../../entities/category/Category';
import { CategoryService } from '../../../../../../services-ngx/category.service';
import { DocumentationService } from '../../../../../../services-ngx/documentation.service';
import { NewCategory } from '../../../../../../entities/category/NewCategory';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { UpdateCategory } from '../../../../../../entities/category/UpdateCategory';
import { GioPermissionService } from '../../../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'app-category-ngx',
  templateUrl: './category-ngx.component.html',
  styleUrl: './category-ngx.component.scss',
})
export class CategoryNgxComponent implements OnInit {
  categoryId: string;
  mode: 'new' | 'edit' = 'new';

  categoryDetails: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
    hidden: FormControl<boolean>;
    page: FormControl<string>;
    picture: FormControl<unknown[]>;
    background: FormControl<unknown[]>;
  }>;
  categoryDetailsInitialValue: unknown;

  retrieveCategory = new BehaviorSubject(1);
  category$: Observable<Category>;
  pages$: Observable<Page[]>;

  private destroyRef = inject(DestroyRef);

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private categoryService: CategoryService,
    private documentationService: DocumentationService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit() {
    this.category$ = this.retrieveCategory.pipe(
      switchMap((_) => this.activatedRoute.params),
      switchMap(({ categoryId }) => {
        if (!!categoryId && categoryId !== 'new') {
          this.mode = 'edit';
          this.categoryId = categoryId;
          return this.categoryService.get(categoryId);
        }
        this.categoryId = 'new';
        return of({} as Category);
      }),
      tap((category) => {
        this.categoryDetails = new FormGroup({
          name: new FormControl<string>(category.name, { validators: Validators.required }),
          description: new FormControl<string>(category.description),
          hidden: new FormControl<boolean>(category.hidden != null ? category.hidden : false),
          page: new FormControl<string>(category.page),
          picture: new FormControl<unknown[]>(category.picture_url ? [category.picture_url] : []),
          background: new FormControl<unknown[]>(category.background_url ? [category.background_url] : []),
        });
        this.categoryDetailsInitialValue = this.categoryDetails.getRawValue();
        this.handleReadOnly();
      }),
    );

    this.pages$ = this.retrieveCategory.pipe(switchMap((_) => this.documentationService.listPortalPages(PageType.MARKDOWN, true)));
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
            this.retrieveCategory.next(1);
          }
        },
        error: ({ error }) => this.snackBarService.error(error.message),
      });
  }

  private updateCategory$(): Observable<Category> {
    return this.categoryService.get(this.categoryId).pipe(
      switchMap((category) => {
        const newValues = this.categoryDetails.getRawValue();
        const categoryToUpdate: UpdateCategory = {
          ...category,
          name: newValues.name,
          description: newValues.description,
          page: newValues.page,
          hidden: newValues.hidden,
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

  private handleReadOnly(): void {
    // User cannot change category details
    if (!this.permissionService.hasAnyMatching(['environment-category-u'])) {
      this.categoryDetails.disable();
    }
  }
}

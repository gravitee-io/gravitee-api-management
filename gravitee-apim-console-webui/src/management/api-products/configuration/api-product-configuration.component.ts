/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AsyncValidatorFn, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EMPTY, merge, Observable, of, Subject, timer } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormFocusInvalidModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';

import { ApiProductDangerZoneComponent } from './danger-zone/api-product-danger-zone.component';

import { ApiProduct, UpdateApiProduct } from '../../../entities/management-api-v2/api-product';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

interface ConfigForm {
  name: FormControl<string>;
  version: FormControl<string>;
  description: FormControl<string>;
}

@Component({
  selector: 'api-product-configuration',
  templateUrl: './api-product-configuration.component.html',
  styleUrls: ['./api-product-configuration.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    GioFormFocusInvalidModule,
    GioSaveBarModule,
    ApiProductDangerZoneComponent,
  ],
})
export class ApiProductConfigurationComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly permissionService = inject(GioPermissionService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly matDialog = inject(MatDialog);

  protected readonly isReadOnly = !this.permissionService.hasAnyMatching(['environment-api_product-u']);

  readonly form = signal<FormGroup<ConfigForm> | null>(null);
  readonly initialFormValue = signal<Record<string, unknown> | null>(null);
  readonly nameMaxLength = 512;
  readonly versionMaxLength = 64;
  private readonly reload$ = new Subject<void>();
  private readonly updatedProduct$ = new Subject<ApiProduct>();

  readonly apiProduct = toSignal(
    merge(
      merge(
        (this.activatedRoute.parent?.params ?? of({})).pipe(map(p => p['apiProductId'] ?? null)),
        this.reload$.pipe(map(() => this.activatedRoute.parent?.snapshot?.params['apiProductId'] ?? null)),
      ).pipe(
        switchMap(apiProductId => {
          if (apiProductId) {
            return this.apiProductV2Service.get(apiProductId).pipe(
              catchError(error => {
                this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
                return of(null);
              }),
            );
          }
          return of(null);
        }),
      ),
      this.updatedProduct$,
    ),
    { initialValue: null as ApiProduct | null },
  );

  readonly apiIds = computed(() => this.apiProduct()?.apiIds ?? []);

  private readonly _formSync = effect(() => {
    const p = this.apiProduct();
    if (p) {
      const f = this.createForm(p);
      this.form.set(f);
      this.initialFormValue.set(f.getRawValue());
    }
  });

  onReloadDetails(): void {
    this.reload$.next();
  }

  onSubmit(): void {
    const f = this.form();
    const product = this.apiProduct();
    const apiProductId = product?.id;
    if (f?.valid && apiProductId) {
      const formValue = f.getRawValue();
      const updateApiProduct: UpdateApiProduct = {
        name: formValue.name?.trim() || '',
        version: formValue.version?.trim() || '',
        description: formValue.description?.trim() || undefined,
        apiIds: product?.apiIds,
      };

      this.apiProductV2Service
        .update(apiProductId, updateApiProduct)
        .pipe(
          tap(updatedApiProduct => {
            this.updatedProduct$.next(updatedApiProduct);
            this.initialFormValue.set(f.getRawValue());
            f.markAsPristine();
            f.markAsUntouched();
            this.snackBarService.success('Configuration successfully saved!');
          }),
          catchError(error => {
            this.snackBarService.error(error.error?.message || error.message || 'An error occurred while updating the API Product');
            return EMPTY;
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    } else {
      f?.markAllAsTouched();
    }
  }

  onReset(): void {
    const f = this.form();
    const initial = this.initialFormValue();
    if (f && initial) {
      f.reset(initial);
      f.markAsPristine();
      f.markAsUntouched();
    }
  }

  onDeleteApi(apiId: string): void {
    const apiProductId = this.apiProduct()?.id;
    if (!apiProductId) return;

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '31.25rem',
        data: {
          title: 'Remove API',
          content: 'Are you sure you want to remove this API from the API Product?',
          confirmButton: 'Yes, remove it',
        },
        role: 'alertdialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm): confirm is true => confirm === true),
        switchMap(() => this.apiProductV2Service.deleteApiFromApiProduct(apiProductId, apiId)),
        tap(() => {
          this.snackBarService.success('API removed from the product');
          this.onReloadDetails();
        }),
        catchError(error => {
          this.snackBarService.error(error.error?.message || 'An error occurred while removing the API');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private createForm(apiProduct: ApiProduct | null): FormGroup<ConfigForm> {
    const product = apiProduct ?? undefined;
    return new FormGroup<ConfigForm>({
      name: new FormControl(product?.name ?? '', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(this.nameMaxLength), Validators.minLength(1)],
        asyncValidators: [this.nameUniquenessValidator(product)],
        updateOn: 'blur',
      }),
      version: new FormControl(product?.version ?? '', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(this.versionMaxLength), Validators.minLength(1)],
      }),
      description: new FormControl(product?.description ?? '', { nonNullable: true }),
    });
  }

  private nameUniquenessValidator(currentProduct: ApiProduct | undefined): AsyncValidatorFn {
    return (formControl: FormControl<string>): Observable<ValidationErrors | null> => {
      if (!formControl || !formControl.value || formControl.value.trim() === '') {
        return of(null);
      }
      const trimmedName = formControl.value.trim();
      if (currentProduct && trimmedName === currentProduct.name) {
        return of(null);
      }
      return timer(250).pipe(
        switchMap(() => this.apiProductV2Service.verify(trimmedName)),
        map(res => (res.ok ? null : { unique: true })),
      );
    };
  }
}

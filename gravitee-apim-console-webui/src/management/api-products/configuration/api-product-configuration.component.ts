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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AsyncValidatorFn,
  FormControl,
  ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EMPTY, Observable, of, timer } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GioFormFocusInvalidModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';

import { ApiProductDangerZoneComponent } from './danger-zone/api-product-danger-zone.component';
import { ApiProduct, UpdateApiProduct } from '../../../entities/management-api-v2/api-product';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

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
    GioFormFocusInvalidModule,
    GioSaveBarModule,
    ApiProductDangerZoneComponent,
  ],
})
export class ApiProductConfigurationComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly formBuilder = inject(UntypedFormBuilder);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly snackBarService = inject(SnackBarService);

  form: UntypedFormGroup | null = null;
  initialFormValue: Record<string, unknown> | null = null;
  readonly descriptionMaxLength = 250;
  apiProductId = '';
  apiProduct: ApiProduct | null = null;
  isLoading = false;

  ngOnInit(): void {
    // Get apiProductId from route params (check parent route if not in current route)
    const getApiProductId = (route: ActivatedRoute): string | null => {
      if (route.snapshot.params['apiProductId']) {
        return route.snapshot.params['apiProductId'];
      }
      if (route.parent) {
        return getApiProductId(route.parent);
      }
      return null;
    };

    this.apiProductId = getApiProductId(this.activatedRoute) || '';

    if (this.apiProductId) {
      this.isLoading = true;
      this.apiProductV2Service
        .get(this.apiProductId)
        .pipe(
          tap(apiProduct => {
            this.apiProduct = apiProduct;
            this.isLoading = false;
            this.initializeForm();
          }),
          catchError(error => {
            this.isLoading = false;
            this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
            // Initialize form with empty values on error
            this.initializeForm();
            return of(null);
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    } else {
      // Initialize form with empty values if no API Product ID
      this.initializeForm();
    }
  }

  onReloadDetails(): void {
    if (this.apiProductId) {
      this.isLoading = true;
      this.apiProductV2Service
        .get(this.apiProductId)
        .pipe(
          tap(apiProduct => {
            this.apiProduct = apiProduct;
            this.isLoading = false;
            this.initializeForm();
          }),
          catchError(error => {
            this.isLoading = false;
            this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
            return of(null);
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    }
  }

  onSubmit(): void {
    if (this.form?.valid && this.apiProductId) {
      const formValue = this.form!.getRawValue();
      const updateApiProduct: UpdateApiProduct = {
        name: formValue.name,
        version: formValue.version,
        description: formValue.description || undefined,
        // Preserve existing apiIds if they exist
        apiIds: this.apiProduct?.apiIds,
      };

      this.apiProductV2Service
        .update(this.apiProductId, updateApiProduct)
        .pipe(
          tap(updatedApiProduct => {
            this.apiProduct = updatedApiProduct;
            this.initialFormValue = this.form!.getRawValue();
            this.form!.markAsPristine();
            this.form!.markAsUntouched();
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
      this.form?.markAllAsTouched();
    }
  }

  onReset(): void {
    if (this.form && this.initialFormValue) {
      this.form.reset(this.initialFormValue);
      this.form.markAsPristine();
      this.form.markAsUntouched();
    }
  }

  private initializeForm(): void {
    this.form = this.formBuilder.group({
      name: this.formBuilder.control(this.apiProduct?.name || '', {
        validators: [Validators.required],
        asyncValidators: [this.nameUniquenessValidator()],
        updateOn: 'blur', // Only validate on blur, not on every keystroke
      }),
      version: this.formBuilder.control(this.apiProduct?.version || '', [Validators.required]),
      description: this.formBuilder.control(this.apiProduct?.description || '', [Validators.maxLength(this.descriptionMaxLength)]),
    }) as UntypedFormGroup;
    this.initialFormValue = this.form.getRawValue();
  }

  private nameUniquenessValidator(): AsyncValidatorFn {
    return (formControl: FormControl): Observable<ValidationErrors | null> => {
      if (!formControl || !formControl.value || formControl.value.trim() === '') {
        return of(null);
      }
      const trimmedName = formControl.value.trim();
      // Skip validation if the name hasn't changed from the original
      if (this.apiProduct && trimmedName === this.apiProduct.name) {
        return of(null);
      }
      // Debounce API call to avoid too many requests
      return timer(250).pipe(
        switchMap(() => this.apiProductV2Service.verify(trimmedName)),
        map(res => (res.ok ? null : { unique: true })),
      );
    };
  }
}

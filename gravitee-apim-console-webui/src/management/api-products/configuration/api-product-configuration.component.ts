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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { AsyncValidatorFn, FormControl, UntypedFormBuilder, UntypedFormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Observable, of, Subject, timer } from 'rxjs';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';

import { Constants } from '../../../entities/Constants';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApiProduct, UpdateApiProduct } from '../../../entities/management-api-v2/api-product';

@Component({
  selector: 'api-product-configuration',
  templateUrl: './api-product-configuration.component.html',
  styleUrls: ['./api-product-configuration.component.scss'],
  standalone: false,
})
export class ApiProductConfigurationComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public form: UntypedFormGroup;
  public initialFormValue: unknown;
  public descriptionMaxLength = 250;
  public apiProductId: string;
  public apiProduct: ApiProduct | null = null;
  public isLoading = false;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly apiProductV2Service: ApiProductV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

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
          tap((apiProduct) => {
            this.apiProduct = apiProduct;
            this.isLoading = false;
            this.initializeForm();
          }),
          catchError((error) => {
            this.isLoading = false;
            this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
            // Initialize form with empty values on error
            this.initializeForm();
            return of(null);
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    } else {
      // Initialize form with empty values if no API Product ID
      this.initializeForm();
    }
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  onSubmit(): void {
    if (this.form.valid && this.apiProductId) {
      const formValue = this.form.getRawValue();
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
          tap((updatedApiProduct) => {
            this.apiProduct = updatedApiProduct;
            this.initialFormValue = this.form.getRawValue();
            this.snackBarService.success('Configuration successfully saved!');
          }),
          catchError((error) => {
            this.snackBarService.error(error.error?.message || error.message || 'An error occurred while updating the API Product');
            return EMPTY;
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    } else {
      this.form.markAllAsTouched();
    }
  }

  onReset(): void {
    this.form.reset(this.initialFormValue);
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  getDescriptionLength(): number {
    return this.form.get('description')?.value?.length || 0;
  }

  private initializeForm(): void {
    this.form = this.formBuilder.group({
      name: this.formBuilder.control(
        this.apiProduct?.name || '',
        {
          validators: [Validators.required],
          asyncValidators: [this.nameUniquenessValidator()],
          updateOn: 'blur', // Only validate on blur, not on every keystroke
        },
      ),
      version: this.formBuilder.control(this.apiProduct?.version || '', [Validators.required]),
      description: this.formBuilder.control(this.apiProduct?.description || '', [
        Validators.maxLength(this.descriptionMaxLength),
      ]),
    });
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
        map((res) => (res.ok ? null : { unique: true })),
      );
    };
  }
}


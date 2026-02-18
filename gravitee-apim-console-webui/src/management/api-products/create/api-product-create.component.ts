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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { AsyncValidatorFn, FormControl, UntypedFormBuilder, UntypedFormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of, Subject, timer } from 'rxjs';
import { map, switchMap, takeUntil } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { Constants } from '../../../entities/Constants';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { CreateApiProduct } from '../../../entities/management-api-v2/api-product';

@Component({
  selector: 'api-product-create',
  templateUrl: './api-product-create.component.html',
  styleUrls: ['./api-product-create.component.scss'],
  standalone: false,
})
export class ApiProductCreateComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public form: UntypedFormGroup;
  public descriptionMaxLength = 250;
  public nameMaxLength = 255;
  public versionMaxLength = 255;
  public isCreating = false;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly matDialog: MatDialog,
    private readonly apiProductV2Service: ApiProductV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      name: this.formBuilder.control('', {
        validators: [Validators.required, Validators.maxLength(this.nameMaxLength), Validators.minLength(1)],
        asyncValidators: [this.nameUniquenessValidator()],
        updateOn: 'blur', // Only validate on blur, not on every keystroke
      }),
      version: this.formBuilder.control('', [Validators.required, Validators.maxLength(this.versionMaxLength), Validators.minLength(1)]),
      description: this.formBuilder.control('', [Validators.maxLength(this.descriptionMaxLength)]),
    });
  }

  private nameUniquenessValidator(): AsyncValidatorFn {
    return (formControl: FormControl): Observable<ValidationErrors | null> => {
      if (!formControl || !formControl.value || formControl.value.trim() === '') {
        return of(null);
      }
      const trimmedName = formControl.value.trim();
      // Debounce API call to avoid too many requests
      return timer(250).pipe(
        switchMap(() => this.apiProductV2Service.verify(trimmedName)),
        map(res => (res.ok ? null : { unique: true })),
      );
    };
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  onExit(): void {
    if (this.form.dirty) {
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Exit without saving?',
            content: 'You have unsaved changes. Are you sure you want to exit without saving?',
            confirmButton: 'Exit without saving',
            cancelButton: 'Cancel',
          },
        })
        .afterClosed()
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe(confirmed => {
          if (confirmed) {
            this.router.navigate(['..'], { relativeTo: this.activatedRoute });
          }
        });
      return;
    }
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  onBack(): void {
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  onCreate(): void {
    if (this.form.valid && !this.isCreating) {
      this.isCreating = true;
      const formValue = this.form.getRawValue();

      // Sanitize input: trim whitespace and validate non-empty after trim
      const trimmedName = formValue.name?.trim() || '';
      const trimmedVersion = formValue.version?.trim() || '';
      const trimmedDescription = formValue.description?.trim() || '';

      // Validate that trimmed values are not empty
      if (!trimmedName || !trimmedVersion) {
        this.snackBarService.error('Name and version are required and cannot be empty');
        this.isCreating = false;
        this.form.markAllAsTouched();
        return;
      }

      const createApiProduct: CreateApiProduct = {
        name: trimmedName,
        version: trimmedVersion,
        description: trimmedDescription || undefined,
        apiIds: [], // TODO: Add API selection functionality
      };

      this.apiProductV2Service
        .create(createApiProduct)
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe({
          next: apiProduct => {
            this.isCreating = false;
            this.snackBarService.success(`API Product ${apiProduct.id} successfully created`);
            this.router.navigate(['..', apiProduct.id, 'configuration'], { relativeTo: this.activatedRoute });
          },
          error: error => {
            this.isCreating = false;
            const errorMessage =
              error.error?.message || error.error?.errors?.[0]?.message || 'An error occurred while creating the API Product';
            this.snackBarService.error(errorMessage);
          },
        });
    } else {
      this.form.markAllAsTouched();
    }
  }

  getDescriptionLength(): number {
    const description = this.form.get('description')?.value;
    return description ? description.length : 0;
  }
}

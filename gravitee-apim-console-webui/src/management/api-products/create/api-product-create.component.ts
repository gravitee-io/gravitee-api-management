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

import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { CreateApiProduct } from '../../../entities/management-api-v2/api-product';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { apiProductNameUniqueAsyncValidator } from '../validators/api-product-name-unique-async.validator';

interface CreateApiProductForm {
  name: FormControl<string>;
  version: FormControl<string>;
  description: FormControl<string>;
}

@Component({
  selector: 'api-product-create',
  templateUrl: './api-product-create.component.html',
  styleUrls: ['./api-product-create.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
})
export class ApiProductCreateComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly matDialog = inject(MatDialog);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);

  readonly form = new FormGroup<CreateApiProductForm>({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(512), Validators.minLength(1)],
      asyncValidators: [apiProductNameUniqueAsyncValidator(this.apiProductV2Service)],
      updateOn: 'blur',
    }),
    version: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(64), Validators.minLength(1)],
    }),
    description: new FormControl('', { nonNullable: true }),
  });
  readonly nameMaxLength = 512;
  readonly versionMaxLength = 64;
  readonly isCreating = signal(false);

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
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(confirmed => {
          if (confirmed) {
            this.onBack();
          }
        });
      return;
    }
    this.onBack();
  }

  onBack(): void {
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  goToConfiguration(productId: string): void {
    this.router.navigate(['..', productId, 'configuration'], { relativeTo: this.activatedRoute });
  }

  onCreate(): void {
    if (this.form.valid && !this.isCreating()) {
      this.isCreating.set(true);
      const formValue = this.form.getRawValue();

      // Sanitize input: trim whitespace and validate non-empty after trim
      const trimmedName = formValue.name?.trim() || '';
      const trimmedVersion = formValue.version?.trim() || '';
      const trimmedDescription = formValue.description?.trim() || undefined;

      // Validate that name and version are not empty (both required for creation)
      if (!trimmedName || !trimmedVersion) {
        this.snackBarService.error('Name and version are required and cannot be empty');
        this.isCreating.set(false);
        this.form.markAllAsTouched();
        return;
      }

      const createApiProduct: CreateApiProduct = {
        name: trimmedName,
        version: trimmedVersion,
        description: trimmedDescription,
        apiIds: [], // TODO: Add API selection functionality
      };

      this.apiProductV2Service
        .create(createApiProduct)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: apiProduct => {
            this.isCreating.set(false);
            this.snackBarService.success(`${apiProduct.name} - Successfully created`);
            this.goToConfiguration(apiProduct.id);
          },
          error: err => {
            this.isCreating.set(false);
            const errorMessage =
              err.error?.message || err.error?.errors?.[0]?.message || 'An error occurred while creating the API Product';
            this.snackBarService.error(errorMessage);
          },
        });
    } else {
      this.form.markAllAsTouched();
    }
  }
}

/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DATE_FORMATS, MAT_NATIVE_DATE_FORMATS, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { of, switchMap, tap } from 'rxjs';

import { ApplicationCertificateService } from '../../../../../../services/application-certificate.service';
import { ApplicationService } from '../../../../../../services/application.service';
import { fileNameWithoutExtension } from '../../../../../../utils/common.utils';

export type DetectedPemType = 'certificate' | 'csr' | 'unknown';

export interface AddCertificateDialogData {
  applicationId: string;
  hasActiveCertificates: boolean;
  activeCertificateId?: string;
  activeCertificateName?: string;
  activeCertificateExpiration?: string;
}

@Component({
  selector: 'app-add-certificate-dialog',
  imports: [
    DatePipe,
    MatDialogModule,
    MatButtonModule,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatDatepickerModule,
    ReactiveFormsModule,
  ],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS }],
  templateUrl: './add-certificate-dialog.component.html',
  styleUrl: './add-certificate-dialog.component.scss',
})
export class AddCertificateDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AddCertificateDialogComponent>);
  readonly data: AddCertificateDialogData = inject(MAT_DIALOG_DATA);
  private readonly certService = inject(ApplicationCertificateService);
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly stepper = viewChild<MatStepper>('certificateStepper');

  readonly minDate = new Date();
  readonly maxGracePeriodDate: Date | undefined = this.data.activeCertificateExpiration
    ? new Date(this.data.activeCertificateExpiration)
    : undefined;

  detectedPemType = signal<DetectedPemType | null>(null);
  isSubmitting = signal(false);
  submitError = signal<string | null>(null);
  isValidating = signal(false);
  validateError = signal<string | null>(null);

  readonly uploadForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    certificate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly configureForm = new FormGroup({
    endsAt: new FormControl<Date | null>(null),
    gracePeriodEnd: new FormControl<Date | null>(null, this.data.hasActiveCertificates ? [Validators.required] : []),
  });

  continueStep(group: FormGroup): void {
    group.markAllAsTouched();
    if (group.invalid) return;
    this.stepper()?.next();
  }

  goPrevious(): void {
    const stepper = this.stepper();
    if (!stepper) return;
    // CSR skips Configure, so Previous from Confirm returns to Upload.
    if (this.detectedPemType() === 'csr' && stepper.selectedIndex === 2) {
      stepper.selectedIndex = 0;
      return;
    }
    stepper.previous();
  }

  validateAndContinue(): void {
    this.validateError.set(null);
    this.submitError.set(null);

    const pem = this.uploadForm.controls.certificate.value.trim();
    this.uploadForm.controls.certificate.markAsTouched();
    if (!pem) {
      this.uploadForm.controls.certificate.updateValueAndValidity();
      return;
    }

    const detectedType = this.detectPemType(pem);
    this.detectedPemType.set(detectedType);

    if (detectedType === 'csr') {
      const stepper = this.stepper();
      if (stepper) {
        stepper.selectedIndex = 2; // Jump to Confirm, skipping Configure / grace period
      }
      return;
    }

    if (detectedType === 'unknown') {
      this.validateError.set(
        $localize`:@@addCertificateUnrecognizedPem:Please provide a PEM-encoded certificate or CSR.`,
      );
      return;
    }

    this.uploadForm.markAllAsTouched();
    if (this.uploadForm.invalid) return;

    this.isValidating.set(true);

    this.certService
      .validate(this.data.applicationId, this.uploadForm.controls.certificate.value)
      .pipe(
        tap(response => {
          if (response.certificateExpiration) {
            this.configureForm.controls.endsAt.setValue(new Date(response.certificateExpiration));
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.isValidating.set(false);
          this.stepper()?.next();
        },
        error: (err: HttpErrorResponse) => {
          this.isValidating.set(false);
          if (err.status === 400) {
            this.validateError.set(
              $localize`:@@addCertificateValidationError:Validation failed for Certificate uploaded. Please try again`,
            );
          } else {
            this.validateError.set(
              $localize`:@@addCertificateValidateError:An error occurred while validating the certificate. Please try again`,
            );
          }
        },
      });
  }

  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const content = await this.readFileAsText(file);
    this.uploadForm.controls.certificate.setValue(content);
    this.detectedPemType.set(null);
    if (!this.uploadForm.controls.name.value) {
      this.uploadForm.controls.name.setValue(fileNameWithoutExtension(file.name));
    }
    input.value = '';
  }

  onSubmit(): void {
    if (this.detectedPemType() === 'csr') {
      this.submitCsr(this.uploadForm.controls.certificate.value.trim(), false);
      return;
    }

    this.configureForm.markAllAsTouched();
    if (this.configureForm.invalid || this.uploadForm.invalid) return;

    const { name, certificate } = this.uploadForm.value;
    const { endsAt, gracePeriodEnd } = this.configureForm.value;

    this.isSubmitting.set(true);
    this.submitError.set(null);

    this.certService
      .create(this.data.applicationId, {
        name: name!,
        certificate: certificate!,
        ...(endsAt ? { endsAt: endsAt.toISOString() } : {}),
      })
      .pipe(
        switchMap(() => this.updateActiveCertificateGracePeriod(gracePeriodEnd ?? null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.dialogRef.close(true);
        },
        error: (err: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          if (err.status === 400) {
            this.submitError.set($localize`:@@addCertificateValidationError:Validation failed for Certificate uploaded. Please try again`);
          } else {
            this.submitError.set($localize`:@@addCertificateSubmitError:An error occurred. Please try again`);
          }
        },
      });
  }

  detectPemType(content: string): DetectedPemType {
    const normalized = content.toUpperCase();
    if (normalized.includes('BEGIN CERTIFICATE REQUEST') || normalized.includes('BEGIN NEW CERTIFICATE REQUEST')) {
      return 'csr';
    }
    if (normalized.includes('BEGIN CERTIFICATE')) {
      return 'certificate';
    }
    return 'unknown';
  }

  private submitCsr(csr: string, applyGracePeriod = true): void {
    this.isSubmitting.set(true);
    this.validateError.set(null);
    this.submitError.set(null);

    const gracePeriodEnd = applyGracePeriod ? this.configureForm.controls.gracePeriodEnd.value : null;

    this.applicationService
      .get(this.data.applicationId)
      .pipe(
        switchMap(application => this.applicationService.save({ ...application, csr })),
        switchMap(() => this.updateActiveCertificateGracePeriod(gracePeriodEnd)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.dialogRef.close(true);
        },
        error: (err: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          if (err.status === 400) {
            this.submitError.set($localize`:@@addCertificateCsrValidationError:Validation failed for CSR uploaded. Please try again`);
          } else {
            this.submitError.set($localize`:@@addCertificateCsrSubmitError:An error occurred while saving the CSR. Please try again`);
          }
        },
      });
  }

  private updateActiveCertificateGracePeriod(gracePeriodEnd: Date | null) {
    if (gracePeriodEnd && this.data.activeCertificateId) {
      return this.certService.update(this.data.applicationId, this.data.activeCertificateId, {
        ...(this.data.activeCertificateName ? { name: this.data.activeCertificateName } : {}),
        endsAt: gracePeriodEnd.toISOString(),
      });
    }
    return of(null);
  }

  private readFileAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsText(file);
    });
  }
}

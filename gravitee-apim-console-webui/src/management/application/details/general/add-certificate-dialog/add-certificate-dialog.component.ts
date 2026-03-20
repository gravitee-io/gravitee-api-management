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
<<<<<<< HEAD
import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
=======
import { ChangeDetectorRef, Component, DestroyRef, inject, viewChild } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatStepper } from '@angular/material/stepper';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NewFile } from '@gravitee/ui-particles-angular';

import { ApplicationService } from '../../../../../services-ngx/application.service';
import { ValidateCertificateResponse } from '../../../../../entities/application/ClientCertificate';
>>>>>>> 0c2d8a16bb (refactor: use material stepper for mtls cert rotation)

export interface AddCertificateDialogData {
  hasActiveCertificates: boolean;
  activeCertificateId?: string;
}

export interface AddCertificateDialogResult {
  name: string;
  certificate: string;
  endsAt?: string;
  gracePeriodEnd?: string;
  activeCertificateId?: string;
}

@Component({
  selector: 'add-certificate-dialog',
  templateUrl: './add-certificate-dialog.component.html',
  styleUrls: ['./add-certificate-dialog.component.scss'],
  standalone: false,
})
export class AddCertificateDialogComponent {
<<<<<<< HEAD
  form: FormGroup;
  minDate: Date = new Date();

  constructor(
    private readonly dialogRef: MatDialogRef<AddCertificateDialogComponent, AddCertificateDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: AddCertificateDialogData,
  ) {
    this.form = new FormGroup({
      name: new FormControl('', Validators.required),
      certificate: new FormControl('', Validators.required),
      endsAt: new FormControl(null),
      ...(data.hasActiveCertificates ? { gracePeriodEnd: new FormControl(null, Validators.required) } : {}),
    });
=======
  private readonly dialogRef = inject(MatDialogRef<AddCertificateDialogComponent>);
  readonly data: AddCertificateDialogData = inject(MAT_DIALOG_DATA);
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly stepper = viewChild.required<MatStepper>('stepper');
  uploadStepCompleted = false;

  uploadForm: FormGroup<UploadFormControls>;
  configureForm: FormGroup<ConfigureFormControls>;
  filePickerValue: unknown[] = [];
  minDate = new Date();
  maxEndsAt: Date | undefined;
  maxGracePeriod: Date | undefined;
  isValidating = false;
  validationError: string | undefined;
  validationResponse: ValidateCertificateResponse | undefined;

  constructor() {
    const gracePeriodDefault = this.data.activeCertificateExpiration ? new Date(this.data.activeCertificateExpiration) : null;
    if (gracePeriodDefault) {
      this.maxGracePeriod = gracePeriodDefault;
    }

    this.uploadForm = new FormGroup<UploadFormControls>({
      name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      certificate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    });

    this.configureForm = new FormGroup<ConfigureFormControls>({
      endsAt: new FormControl<Date | null>(null),
      ...(this.data.hasActiveCertificates ? { gracePeriodEnd: new FormControl<Date | null>(gracePeriodDefault, Validators.required) } : {}),
    });
  }

  async onFileSelected(event: (NewFile | string)[] | undefined): Promise<void> {
    if (!event?.length) return;
    const file = event[0];
    if (!(file instanceof NewFile)) return;
    const content = await this.readFileAsText(file.file);
    this.uploadForm.controls.certificate.setValue(content);
    this.filePickerValue = [];
  }

  onValidate(): void {
    this.uploadForm.markAllAsTouched();
    if (this.uploadForm.invalid) {
      return;
    }

    const certificate = this.uploadForm.controls.certificate.value;
    this.isValidating = true;
    this.validationError = undefined;

    this.applicationService
      .validateCertificate(this.data.applicationId, certificate)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.isValidating = false;
          this.validationResponse = response;
          const expiration = new Date(response.certificateExpiration);
          this.maxEndsAt = expiration;
          this.configureForm.controls.endsAt.setValue(expiration);
          this.uploadStepCompleted = true;
          this.cdr.detectChanges();
          this.stepper().next();
        },
        error: () => {
          this.isValidating = false;
          this.validationError = 'Invalid certificate format';
        },
      });
  }

  onContinueToConfirm(): void {
    this.configureForm.markAllAsTouched();
    if (this.configureForm.invalid) {
      return;
    }
    this.stepper().next();
>>>>>>> 0c2d8a16bb (refactor: use material stepper for mtls cert rotation)
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    const { name, certificate, endsAt, gracePeriodEnd } = this.form.getRawValue();

    this.dialogRef.close({
      name,
      certificate,
      endsAt: endsAt ? new Date(endsAt).toISOString() : undefined,
      gracePeriodEnd: gracePeriodEnd ? new Date(gracePeriodEnd).toISOString() : undefined,
      activeCertificateId: this.data.activeCertificateId,
    });
  }
}

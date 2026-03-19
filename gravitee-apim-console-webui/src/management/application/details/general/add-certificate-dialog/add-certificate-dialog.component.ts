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
import { Component, DestroyRef, inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

import { ApplicationService } from '../../../../../services-ngx/application.service';

export interface AddCertificateDialogData {
  applicationId: string;
  hasActiveCertificates: boolean;
  activeCertificateId?: string;
  activeCertificateExpiration?: string;
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
  private readonly dialogRef = inject(MatDialogRef<AddCertificateDialogComponent>);
  readonly data: AddCertificateDialogData = inject(MAT_DIALOG_DATA);
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);

  form: FormGroup;
  minDate = new Date();
  maxEndsAt: Date | undefined;
  maxGracePeriod: Date | undefined;
  isValidating = false;
  validationError: string | undefined;

  constructor() {
    const gracePeriodDefault = this.data.activeCertificateExpiration ? new Date(this.data.activeCertificateExpiration) : null;
    if (gracePeriodDefault) {
      this.maxGracePeriod = gracePeriodDefault;
    }

    this.form = new FormGroup({
      name: new FormControl('', Validators.required),
      certificate: new FormControl('', Validators.required),
      endsAt: new FormControl(null),
      ...(this.data.hasActiveCertificates ? { gracePeriodEnd: new FormControl(gracePeriodDefault, Validators.required) } : {}),
    });

    this.setupCertificateValidation();
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

  private setupCertificateValidation(): void {
    this.form.controls['certificate'].valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged(),
        switchMap((certificate: string) => {
          if (!certificate?.trim()) {
            this.maxEndsAt = undefined;
            this.isValidating = false;
            this.validationError = undefined;
            return EMPTY;
          }
          this.isValidating = true;
          this.validationError = undefined;
          return this.applicationService.validateCertificate(this.data.applicationId, certificate).pipe(
            catchError(() => {
              this.maxEndsAt = undefined;
              this.isValidating = false;
              this.validationError = 'Invalid certificate format';
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(response => {
        this.isValidating = false;
        this.validationError = undefined;
        const expiration = new Date(response.certificateExpiration);
        this.maxEndsAt = expiration;
        this.form.controls['endsAt'].setValue(expiration);
      });
  }
}

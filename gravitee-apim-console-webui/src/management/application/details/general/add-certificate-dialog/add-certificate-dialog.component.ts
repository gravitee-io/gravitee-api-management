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
import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

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
  form: FormGroup;

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

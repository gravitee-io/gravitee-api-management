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
import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DATE_FORMATS, MatNativeDateModule, MAT_NATIVE_DATE_FORMATS, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { CreateClientCertificateInput } from '../../../../../../entities/application/client-certificate';

export interface AddCertificateDialogData {
  hasActiveCertificates: boolean;
}

@Component({
  selector: 'app-add-certificate-dialog',
  imports: [
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatNativeDateModule,
    ReactiveFormsModule,
  ],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS }],
  templateUrl: './add-certificate-dialog.component.html',
  styleUrl: './add-certificate-dialog.component.scss',
})
export class AddCertificateDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AddCertificateDialogComponent, CreateClientCertificateInput>);
  readonly data: AddCertificateDialogData = inject(MAT_DIALOG_DATA, { optional: true }) ?? { hasActiveCertificates: false };

  readonly minDate = new Date();

  form = new FormGroup({
    name: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    certificate: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    endsAt: new FormControl<Date | null>(null),
  });

  onCancel(): void {
    this.dialogRef.close();
  }

  onAdd(): void {
    if (this.form.valid) {
      const { name, certificate, endsAt } = this.form.getRawValue();
      this.dialogRef.close({
        name,
        certificate,
        endsAt: endsAt ? new Date(endsAt).toISOString() : undefined,
      });
    }
  }
}

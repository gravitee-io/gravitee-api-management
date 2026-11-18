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
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { Constants } from '../../../../../entities/Constants';

export interface ApplicationSharedApiKeyRenewDialogResult {
  customApiKey?: string;
}

/** Same length and characters as the backend `@CustomApiKey` constraint. A blank value means generate. */
const CUSTOM_API_KEY_PATTERN = /^[^#%@/;=?|^~, \\]*$/;

function customApiKeyValidator(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value.trim();
  if (value.length === 0) {
    return null;
  }
  if (value.length < 8 || value.length > 128) {
    return { length: true };
  }
  if (!CUSTOM_API_KEY_PATTERN.test(value)) {
    return { characters: true };
  }
  return null;
}

@Component({
  selector: 'application-shared-api-key-renew-dialog',
  templateUrl: './application-shared-api-key-renew-dialog.component.html',
  styleUrl: './application-shared-api-key-renew-dialog.component.scss',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationSharedApiKeyRenewDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<ApplicationSharedApiKeyRenewDialogComponent, ApplicationSharedApiKeyRenewDialogResult>);
  private readonly constants = inject(Constants);

  readonly canUseCustomApiKey = this.constants.env?.settings?.plan?.security?.customApiKey?.enabled === true;

  readonly form = new FormGroup({
    apiKey: new FormControl('', { nonNullable: true, validators: [customApiKeyValidator] }),
  });

  renew(): void {
    if (this.form.invalid) {
      return;
    }
    const value = this.canUseCustomApiKey ? this.form.controls.apiKey.value.trim() : '';
    this.dialogRef.close(value.length > 0 ? { customApiKey: value } : {});
  }
}

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
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { ApiImportV4FormComponent } from '../../import-v4/api-import-v4-form/api-import-v4-form.component';

export interface ApiImportV4FormDialogData {
  apiId: string;
  apiName: string;
}

@Component({
  selector: 'api-import-v4-form-dialog',
  standalone: true,
  imports: [MatDialogModule, ApiImportV4FormComponent],
  templateUrl: './api-import-v4-form-dialog.component.html',
  styleUrl: './api-import-v4-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiImportV4FormDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<ApiImportV4FormDialogComponent, string | undefined>);
  protected readonly data = inject<ApiImportV4FormDialogData>(MAT_DIALOG_DATA);

  protected onImportCompleted(apiId: string): void {
    this.dialogRef.close(apiId);
  }

  protected onDismissed(): void {
    this.dialogRef.close();
  }
}

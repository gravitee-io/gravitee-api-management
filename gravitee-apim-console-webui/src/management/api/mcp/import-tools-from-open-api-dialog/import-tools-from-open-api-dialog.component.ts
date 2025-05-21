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
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface ImportToolsFromOpenApiDialogData {}

export interface ImportToolsFromOpenApiDialogResult {
  tools: string;
}

@Component({
  selector: 'import-tools-from-open-api-dialog',
  templateUrl: './import-tools-from-open-api-dialog.component.html',
  styleUrls: ['./import-tools-from-open-api-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule],
})
export class ImportToolsFromOpenApiDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: ImportToolsFromOpenApiDialogData,
    public dialogRef: MatDialogRef<ImportToolsFromOpenApiDialogComponent, ImportToolsFromOpenApiDialogResult>,
  ) {}
}

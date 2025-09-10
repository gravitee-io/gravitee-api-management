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
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { OpenApiToMcpToolsComponent } from '../open-api-to-mcp-tools/open-api-to-mcp-tools.component';
import { MCPTool } from '../../../../../entities/entrypoint/mcp';

export interface ImportMcpToolsDialogData {
  hasPreviousTools: boolean;
}

export type ImportMcpToolsDialogResult =
  | {
      tools: MCPTool[];
    }
  | undefined;

@Component({
  selector: 'import-mcp-tools-dialog',
  templateUrl: './import-mcp-tools-dialog.component.html',
  styleUrls: ['./import-mcp-tools-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, OpenApiToMcpToolsComponent, ReactiveFormsModule, GioBannerModule],
})
export class ImportMcpToolsDialogComponent {
  formControl = new FormControl<MCPTool[]>([]);

  canImportMcpTools = toSignal(
    this.formControl.valueChanges.pipe(
      map((value: MCPTool[]) => value.length === 0 || this.formControl.invalid || this.formControl.disabled),
    ),
  );

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: ImportMcpToolsDialogData,
    public dialogRef: MatDialogRef<ImportMcpToolsDialogComponent, ImportMcpToolsDialogResult>,
  ) {}

  importMcpTools(): void {
    this.dialogRef.close({ tools: this.formControl.value || [] });
  }
}

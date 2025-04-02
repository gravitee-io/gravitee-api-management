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
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { DatePipe } from '@angular/common';
import { GioClipboardModule, GioIconsModule, GioMonacoEditorModule, MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ReactiveFormsModule, UntypedFormControl } from '@angular/forms';
import { MatCardActions } from '@angular/material/card';
import { MatTooltip } from '@angular/material/tooltip';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioDiffModule } from '../../../../shared/components/gio-diff/gio-diff.module';

export interface ApiHistoryV4DeploymentInfoDialogData {
  version: string;
  eventId?: string;
  createdAt?: Date;
  label?: string;
  user?: string;
  hideRollback?: boolean;
  apiDefinition: string;
}

export type ApiHistoryV4DeploymentInfoDialogResult = null | { rollbackTo: string };

@Component({
  selector: 'app-deployment-info-dialog',
  templateUrl: './api-history-v4-deployment-info-dialog.component.html',
  styleUrls: ['./api-history-v4-deployment-info-dialog.component.scss'],
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,

    GioIconsModule,
    GioDiffModule,
    GioPermissionModule,
    GioMonacoEditorModule,
    GioClipboardModule,
    DatePipe,
    MatCardActions,
    MatTooltip,
  ],
})
export class ApiHistoryV4DeploymentInfoDialogComponent {
  public languageConfig: MonacoEditorLanguageConfig = { language: 'json', schemas: [] };
  public control: UntypedFormControl;

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: ApiHistoryV4DeploymentInfoDialogData,
    public dialogRef: MatDialogRef<ApiHistoryV4DeploymentInfoDialogComponent, ApiHistoryV4DeploymentInfoDialogResult>,
  ) {
    this.control = new UntypedFormControl({
      value: data.apiDefinition,
      disabled: true,
    });
  }
}

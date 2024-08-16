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
import { GioMonacoEditorModule, MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';
import { FormsModule } from '@angular/forms';

import { SharedPolicyGroup } from '../../../../../../entities/management-api-v2';

export interface HistoryJsonDialogData {
  sharedPolicyGroup: SharedPolicyGroup;
}

export type HistoryJsonDialogResult = boolean;

@Component({
  selector: 'history-json-dialog',
  templateUrl: './history-json-dialog.component.html',
  styleUrls: ['./history-json-dialog.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, GioMonacoEditorModule, FormsModule],
})
export class HistoryJsonDialogComponent {
  protected languageConfig: MonacoEditorLanguageConfig = {
    language: 'json',
  };

  protected sharedPolicyGroup = JSON.stringify(this.data.sharedPolicyGroup, null, 2);

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: HistoryJsonDialogData,
    public dialogRef: MatDialogRef<HistoryJsonDialogComponent, HistoryJsonDialogResult>,
  ) {}
}

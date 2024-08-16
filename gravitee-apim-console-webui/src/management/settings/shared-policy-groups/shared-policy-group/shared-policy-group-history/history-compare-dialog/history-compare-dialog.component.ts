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

import { GioDiffModule } from '../../../../../../shared/components/gio-diff/gio-diff.module';
import { SharedPolicyGroup } from '../../../../../../entities/management-api-v2';

export interface HistoryCompareDialogData {
  left: SharedPolicyGroup;
  right: SharedPolicyGroup;
  rightIsPending?: boolean;
}

export type HistoryCompareDialogResult = boolean;

@Component({
  selector: 'history-compare-dialog',
  templateUrl: './history-compare-dialog.component.html',
  styleUrls: ['./history-compare-dialog.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, GioDiffModule],
})
export class HistoryCompareDialogComponent {
  protected readonly left = JSON.stringify(this.data.left, null, 2);
  protected readonly right = JSON.stringify(this.data.right, null, 2);
  protected readonly rightIsPending = this.data.rightIsPending;

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: HistoryCompareDialogData,
    public dialogRef: MatDialogRef<HistoryCompareDialogComponent, HistoryCompareDialogResult>,
  ) {}
}

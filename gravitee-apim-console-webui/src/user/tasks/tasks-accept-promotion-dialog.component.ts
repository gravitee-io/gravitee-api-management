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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface TasksAcceptPromotionDialogData {
  isApiUpdate: boolean;
  apiName: string;
  targetEnvironmentName: string;
}
export interface TasksAcceptPromotionDialogResult {
  accepted: boolean;
}

@Component({
  selector: 'tasks-accept-promotion-dialog',
  templateUrl: './tasks-accept-promotion-dialog.component.html',
  styleUrls: ['./tasks-accept-promotion-dialog.component.scss'],
  standalone: false,
})
export class TasksAcceptPromotionDialogComponent {
  data: TasksAcceptPromotionDialogData;

  constructor(
    private readonly dialogRef: MatDialogRef<TasksAcceptPromotionDialogComponent, TasksAcceptPromotionDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: TasksAcceptPromotionDialogData,
  ) {
    this.data = dialogData;
  }
}

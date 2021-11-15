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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

export type GioConfirmDialogData = {
  title?: string;
  content?: string;
  confirmButton?: string;
  cancelButton?: string;
};

@Component({
  selector: 'gio-confirm-dialog',
  template: require('./gio-confirm-dialog.component.html'),
})
export class GioConfirmDialogComponent {
  title: string;
  content?: string;
  confirmButton: string;
  cancelButton: string;

  constructor(public dialogRef: MatDialogRef<GioConfirmDialogComponent>, @Inject(MAT_DIALOG_DATA) confirmDialogData: GioConfirmDialogData) {
    this.title = confirmDialogData?.title ?? 'Are you sure ?';
    this.content = confirmDialogData?.content;
    this.confirmButton = confirmDialogData?.confirmButton ?? 'Yes';
    this.cancelButton = confirmDialogData?.cancelButton ?? 'Cancel';
  }
}

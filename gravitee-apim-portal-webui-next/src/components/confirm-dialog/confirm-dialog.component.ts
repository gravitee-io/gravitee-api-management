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
import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

/**
 * Confirm dialog data.
 *
 * Dialog component does not handle localization, so all translation must be handled calling $localize.
 *
 * @example
 * {
 *    title: $localize`:@@confirmDialogTitle:Confirm action?`,
 *    content: $localize`:@@confirmDialogContent:Are you sure you want to confirm the action?`,
 *    confirmLabel: $localize`:@@confirmDialogConfirmLabel:Confirm`,
 *    cancelLabel: $localize`:@@confirmDialogCancelLabel:Cancel`,
 * }
 */
export interface ConfirmDialogData {
  title: string;
  content: string;
  confirmLabel: string;
  cancelLabel: string;
}

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.component.html',
  imports: [MatDialogModule, MatButtonModule],
  standalone: true,
})
export class ConfirmDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData) {}
}

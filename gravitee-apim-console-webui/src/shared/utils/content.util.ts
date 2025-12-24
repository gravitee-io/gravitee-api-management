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

import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

export function normalizeContent(content: string | null | undefined): string {
  return (content || '').replace(/\r\n/g, '\n').trim();
}

// Centralized discard changes confirmation utility
export function confirmDiscardChanges(matDialog: MatDialog): Observable<boolean> {
  const data: GioConfirmDialogData = {
    title: 'Discard changes',
    content: 'There are unsaved changes on this page. Would you like to discard changes or continue editing?',
    confirmButton: 'Discard Changes',
    cancelButton: 'Continue Editing',
  };

  return matDialog
    .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
      width: GIO_DIALOG_WIDTH.SMALL,
      data,
      role: 'alertdialog',
      id: 'discardChangesConfirmDialog',
    })
    .afterClosed();
}

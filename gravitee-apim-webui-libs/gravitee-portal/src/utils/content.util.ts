import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

export function normalizeContent(content: string | null | undefined): string {
  return (content || '').replace(/\r\n/g, '\n').trim();
}

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
    .afterClosed() as Observable<boolean>;
}

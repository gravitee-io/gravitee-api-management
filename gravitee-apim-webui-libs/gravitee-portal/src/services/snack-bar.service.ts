import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root',
})
export class SnackBarService {
  private readonly defaultSnackBarOptions: MatSnackBarConfig = {
    duration: 3000,
    horizontalPosition: 'end',
  };

  constructor(private readonly matSnackBar: MatSnackBar) {}

  success(message: string, undoAction?: string) {
    return this.matSnackBar.open(message, undoAction, {
      ...this.defaultSnackBarOptions,
      panelClass: 'gio-snack-bar-success',
    });
  }

  error(message: string) {
    return this.matSnackBar.open(message, 'Close', {
      ...this.defaultSnackBarOptions,
      duration: undefined,
      panelClass: 'gio-snack-bar-error',
    });
  }
}

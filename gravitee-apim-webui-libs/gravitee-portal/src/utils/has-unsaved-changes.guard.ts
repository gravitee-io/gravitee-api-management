import { CanDeactivateFn } from '@angular/router';
import { Observable } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef, inject } from '@angular/core';

import { confirmDiscardChanges } from './content.util';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean | Observable<boolean>;
}

export const HasUnsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component: HasUnsavedChanges) => {
  if (component.hasUnsavedChanges()) {
    return confirmDiscardChanges(inject(MatDialog)).pipe(
      map(x => !!x),
      takeUntilDestroyed(inject(DestroyRef)),
    );
  }
  return true;
};

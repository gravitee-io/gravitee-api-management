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
import { CanDeactivateFn } from '@angular/router';
import { Observable } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef, inject } from '@angular/core';

import { confirmDiscardChanges } from '../utils/content.util';

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

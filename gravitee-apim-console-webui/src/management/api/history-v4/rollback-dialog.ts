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
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { filter, switchMap } from 'rxjs/operators';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';

export const openRollbackDialog = (
  matDialog: MatDialog,
  snackBarService: SnackBarService,
  apiService: ApiV2Service,
  apiId: string,
  eventId: string,
) => {
  matDialog
    .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
      data: {
        title: 'Rollback API',
        content: 'Are you sure you want to rollback this API? This change will update the API with this deployment version.',
      },
      width: GIO_DIALOG_WIDTH.MEDIUM,
    })
    .afterClosed()
    .pipe(
      filter((result) => !!result),
      switchMap(() => apiService.rollback(apiId, eventId)),
    )
    .subscribe({
      next: () => {
        snackBarService.success('API deployment has been rolled back successfully');
      },
      error: (error) => {
        snackBarService.error(error?.error?.message ?? 'An error occurred while rolling back the API deployment!');
      },
    });
};

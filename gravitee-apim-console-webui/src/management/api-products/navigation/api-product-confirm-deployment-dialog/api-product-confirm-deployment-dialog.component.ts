/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { catchError, EMPTY, tap } from 'rxjs';

import { ApiProductV2Service } from '../../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

export interface ApiProductConfirmDeploymentDialogData {
  apiProductId: string;
}

export type ApiProductConfirmDeploymentDialogResult = void;

@Component({
  selector: 'api-product-confirm-deployment-dialog',
  templateUrl: './api-product-confirm-deployment-dialog.component.html',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule],
})
export class ApiProductConfirmDeploymentDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<ApiProductConfirmDeploymentDialogComponent, ApiProductConfirmDeploymentDialogResult>>(MatDialogRef);
  private readonly dialogData = inject<ApiProductConfirmDeploymentDialogData>(MAT_DIALOG_DATA);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);

  onDeploy(): void {
    this.apiProductV2Service
      .deploy(this.dialogData.apiProductId)
      .pipe(
        tap(() => {
          this.dialogRef.close();
          this.snackBarService.success('API Product successfully deployed.');
        }),
        catchError(err => {
          this.snackBarService.error(`An error occurred while deploying the API Product.\n${err.error?.message ?? err.message}.`);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}

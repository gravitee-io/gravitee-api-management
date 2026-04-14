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
import { ChangeDetectionStrategy, Component, DestroyRef, Inject, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { EMPTY, Observable } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { ApiImportV4StepperComponent } from './api-import-v4-stepper.component';
import { ApiImportV4DialogData, ApiImportV4WizardPayload } from './api-import-v4-wizard.model';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PolicyV2Service } from '../../../services-ngx/policy-v2.service';
import { ApiV4 } from '../../../entities/management-api-v2';


@Component({
  selector: 'api-import-v4-dialog',
  imports: [ApiImportV4StepperComponent, MatDialogModule],
  templateUrl: './api-import-v4-dialog.component.html',
  styleUrl: './api-import-v4-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiImportV4DialogComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly policyV2Service = inject(PolicyV2Service);

  protected readonly hasOasValidationPolicy = toSignal(
    this.policyV2Service.list().pipe(map(policies => policies.some(policy => policy.id === 'oas-validation'))),
    { initialValue: false },
  );

  constructor(
    private readonly dialogRef: MatDialogRef<ApiImportV4DialogComponent, string | undefined>,
    @Inject(MAT_DIALOG_DATA) readonly data: ApiImportV4DialogData,
  ) {}

  protected onImportRequested(payload: ApiImportV4WizardPayload): void {
    const request$ = this.buildImportRequest(payload);
    if (!request$) {
      return;
    }
    request$
      .pipe(
        tap(createdApi => {
          this.snackBarService.success('API imported successfully');
          this.dialogRef.close(createdApi.id);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'An error occurred while importing the API');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected onCancelled(): void {
    this.dialogRef.close(undefined);
  }

  private buildImportRequest(payload: ApiImportV4WizardPayload): Observable<ApiV4> | null {
    if (payload.fileSourceType === 'remote') {
      this.snackBarService.error('Remote import is not yet implemented');
      return null;
    }
    if (payload.apiFormat === 'wsdl') {
      this.snackBarService.success('WSDL import is not yet implemented');
      return null;
    }
    if (
      payload.apiFormat === 'gravitee' &&
      payload.detectedImportType === 'MAPI_V2' &&
      payload.fileContent != null
    ) {
      return this.apiV2Service.import(payload.fileContent);
    }
    if (
      payload.apiFormat === 'openapi' &&
      payload.detectedImportType === 'SWAGGER' &&
      payload.fileContent != null
    ) {
      return this.apiV2Service.importSwaggerApi({
        payload: payload.fileContent,
        withDocumentation: payload.createDocPage,
        withOASValidationPolicy: payload.addSpecValidation,
      });
    }
    this.snackBarService.error('Unsupported type for V4 API import');
    return null;
  }
}

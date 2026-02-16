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
import { Component, Inject, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormControl } from '@angular/forms';
import { Subject } from 'rxjs';
import { map, switchMap, takeUntil } from 'rxjs/operators';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { FlowService } from '../../../../services-ngx/flow.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

export interface ApiConfirmDeploymentDialogData {
  apiId: string;
}
export type ApiConfirmDeploymentDialogResult = void;
@Component({
  selector: 'api-confirm-deployment-dialog',
  templateUrl: './api-confirm-deployment-dialog.component.html',
  styleUrls: ['./api-confirm-deployment-dialog.component.scss'],
  standalone: false,
})
export class ApiConfirmDeploymentDialogComponent implements OnDestroy {
  private unsubscribe$ = new Subject<void>();

  public deploymentLabel = new UntypedFormControl();

  public hasPlatformPolicies$ = this.flowService.getConfiguration().pipe(map(configuration => configuration.has_policies));

  constructor(
    private readonly dialogRef: MatDialogRef<ApiConfirmDeploymentDialogComponent, ApiConfirmDeploymentDialogResult>,
    @Inject(MAT_DIALOG_DATA) private readonly dialogData: ApiConfirmDeploymentDialogData,
    private readonly snackBarService: SnackBarService,
    private readonly apiV2Service: ApiV2Service,
    private readonly flowService: FlowService,
  ) {}

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onDeploy() {
    this.apiV2Service
      .deploy(this.dialogData.apiId, this.deploymentLabel.value)
      .pipe(
        switchMap(() => this.apiV2Service.get(this.dialogData.apiId)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        () => {
          this.dialogRef.close();
          this.snackBarService.success('API successfully deployed.');
        },
        err => {
          this.snackBarService.error(`An error occurred while deploying the API.\n${err.error?.message}.`);
        },
      );
  }
}

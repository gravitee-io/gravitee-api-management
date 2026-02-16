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
import { Component, Inject, OnInit } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { GioBannerModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import { map, Observable, shareReplay } from 'rxjs';
import { MatIcon } from '@angular/material/icon';
import { AsyncPipe } from '@angular/common';
import { MatCheckbox } from '@angular/material/checkbox';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { MigrateToV4Issue, MigrateToV4Response } from '../../../../entities/management-api-v2/api/v2/migrateToV4Response';
import { MigrateDialogResult } from '../api-general-info.component';

@Component({
  selector: 'api-general-info-migrate-to-v4-dialog',
  imports: [
    GioLoaderModule,
    MatDialogTitle,
    MatDialogContent,
    MatButton,
    MatDialogActions,
    MatDialogClose,
    AsyncPipe,
    MatIcon,
    GioBannerModule,
    MatCheckbox,
  ],
  templateUrl: './api-general-info-migrate-to-v4-dialog.component.html',
  styleUrl: './api-general-info-migrate-to-v4-dialog.component.scss',
})
export class ApiGeneralInfoMigrateToV4DialogComponent implements OnInit {
  public migrationStep: 'INITIAL_CHECK' | 'CONFIRMATION' = 'INITIAL_CHECK';
  public isConfirmed = false;

  migrationResponse$: Observable<MigrateToV4Response>;
  impossibleIssues$: Observable<MigrateToV4Issue[]>;
  forcibleIssues$: Observable<MigrateToV4Issue[]>;

  constructor(
    public dialogRef: MatDialogRef<ApiGeneralInfoMigrateToV4DialogComponent, MigrateDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: { apiId: string },
    private readonly apiService: ApiV2Service,
  ) {}

  ngOnInit(): void {
    this.migrationResponse$ = this.apiService.migrateToV4(this.data.apiId, 'DRY_RUN').pipe(shareReplay(1));
    this.impossibleIssues$ = this.migrationResponse$.pipe(
      map(response => response.issues?.filter(issue => issue.state === 'IMPOSSIBLE') ?? []),
    );
    this.forcibleIssues$ = this.migrationResponse$.pipe(
      map(response => response.issues?.filter(issue => issue.state === 'CAN_BE_FORCED') ?? []),
    );
  }

  onContinue(): void {
    this.migrationStep = 'CONFIRMATION';
  }

  onBack(): void {
    this.migrationStep = 'INITIAL_CHECK';
    this.isConfirmed = false;
  }

  onStartMigration(migrationResponse: MigrateToV4Response): void {
    this.dialogRef.close({
      confirmed: true,
      state: migrationResponse.state,
    });
  }
}

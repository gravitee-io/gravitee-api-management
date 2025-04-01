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

import { Component, Inject } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';
import { remove } from 'lodash';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { isUnique } from '../../../../../shared/utils';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiV1, ApiV2 } from '../../../../../entities/management-api-v2';
import { onlyApiV2Filter } from '../../../../../util/apiFilter.operator';

export interface ApiPathMappingsEditDialogData {
  api: ApiV1 | ApiV2;
  path: string;
}

@Component({
  selector: 'api-path-mappings-edit-dialog',
  templateUrl: './api-path-mappings-edit-dialog.component.html',
  styleUrls: ['./api-path-mappings-edit-dialog.component.scss'],
  standalone: false,
})
export class ApiPathMappingsEditDialogComponent {
  private api: ApiV1 | ApiV2;
  private readonly pathToUpdate: string;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public pathFormGroup: UntypedFormGroup;
  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPathMappingsEditDialogData,
    private readonly dialogRef: MatDialogRef<ApiPathMappingsEditDialogData>,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {
    this.api = dialogData.api;
    this.pathToUpdate = dialogData.path;
    this.pathFormGroup = new UntypedFormGroup({
      path: new UntypedFormControl(this.pathToUpdate, [Validators.required, isUnique(this.api.pathMappings)]),
    });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public save(): void {
    this.apiService
      .get(this.api.id)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => {
          remove(api.pathMappings, (p) => p === this.pathToUpdate);
          api.pathMappings.push(this.pathFormGroup.getRawValue().path);
          return this.apiService.update(api.id, api);
        }),
        catchError(() => {
          this.snackBarService.error(`An error occurred while trying to update the path mapping ${this.pathToUpdate}.`);
          return EMPTY;
        }),
        tap(() => {
          this.snackBarService.success(`The path mapping has been successfully updated!`);
          this.dialogRef.close();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}

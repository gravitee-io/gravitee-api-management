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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';
import { remove } from 'lodash';

import { Api } from '../../../../../entities/api';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { isUnique } from '../../../../../shared/utils';

export interface ApiPathMappingsEditDialogData {
  api: Api;
  path: string;
}

@Component({
  selector: 'api-path-mappings-edit-dialog',
  template: require('./api-path-mappings-edit-dialog.component.html'),
  styles: [require('./api-path-mappings-edit-dialog.component.scss')],
})
export class ApiPathMappingsEditDialogComponent {
  private api: Api;
  private readonly pathToUpdate: string;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public pathFormGroup: FormGroup;
  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPathMappingsEditDialogData,
    private readonly dialogRef: MatDialogRef<ApiPathMappingsEditDialogData>,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.api = dialogData.api;
    this.pathToUpdate = dialogData.path;
    this.pathFormGroup = new FormGroup({
      path: new FormControl(this.pathToUpdate, [Validators.required, isUnique(this.api.path_mappings)]),
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
        takeUntil(this.unsubscribe$),
        switchMap((api) => {
          remove(api.path_mappings, (p) => p === this.pathToUpdate);
          api.path_mappings.push(this.pathFormGroup.getRawValue().path);
          return this.apiService.update(api);
        }),
        catchError(() => {
          this.snackBarService.error(`An error occurred while trying to update the path mapping ${this.pathToUpdate}.`);
          return EMPTY;
        }),
        tap(() => {
          this.snackBarService.success(`The path mapping has been successfully updated!`);
          this.dialogRef.close();
        }),
      )
      .subscribe();
  }
}

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
import { MatTabChangeEvent } from '@angular/material/tabs';

import { Api } from '../../../../../entities/api';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { isUnique } from '../../../../../shared/utils';
import { Page } from '../../../../../entities/page';

export interface ApiPathMappingsAddDialogData {
  api: Api;
  swaggerDocs: Page[];
}

@Component({
  selector: 'api-path-mappings-add-dialog',
  template: require('./api-path-mappings-add-dialog.component.html'),
  styles: [require('./api-path-mappings-add-dialog.component.scss')],
})
export class ApiPathMappingsAddDialogComponent {
  tabLabels = {
    AddPathMapping: 'Path',
    SwaggerDocument: 'Swagger Document',
  };

  private api: Api;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public pathFormGroup: FormGroup;
  public swaggerDocs: Page[];

  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPathMappingsAddDialogData,
    private readonly dialogRef: MatDialogRef<ApiPathMappingsAddDialogData>,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.api = dialogData.api;
    this.swaggerDocs = dialogData.swaggerDocs;
    this.pathFormGroup = this.initFormGroup();
  }

  private initFormGroup() {
    return new FormGroup({
      path: new FormControl('', [isUnique(this.api.path_mappings), Validators.pattern('^/[a-zA-Z0-9:/]+')]),
    });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSelectedTab(event: MatTabChangeEvent) {
    this.pathFormGroup = this.initFormGroup();
    switch (event.tab.textLabel) {
      case this.tabLabels.AddPathMapping:
        this.pathFormGroup.addValidators(Validators.required);
        break;
    }
  }

  onAddPath() {
    this.apiService
      .get(this.api.id)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) => {
          api.path_mappings.push(this.pathFormGroup.getRawValue().path);
          return this.apiService.update(api);
        }),
        catchError(() => {
          this.snackBarService.error(`An error occurred while trying to add the path mapping ${this.pathFormGroup.getRawValue().path}.`);
          return EMPTY;
        }),
        tap(() => {
          this.snackBarService.success(`The path mapping has been successfully added!`);
          this.dialogRef.close();
        }),
      )
      .subscribe();
  }
}

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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';
import { MatTabChangeEvent } from '@angular/material/tabs';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { isUnique } from '../../../../../shared/utils';
import { Page } from '../../../../../entities/page';
import { ApiV1, ApiV2 } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { onlyApiV2Filter } from '../../../../../util/apiFilter.operator';
import { ApiService } from '../../../../../services-ngx/api.service';

export interface ApiPathMappingsAddDialogData {
  api: ApiV1 | ApiV2;
  swaggerDocs: Page[];
}

@Component({
  selector: 'api-path-mappings-add-dialog',
  templateUrl: './api-path-mappings-add-dialog.component.html',
  styleUrls: ['./api-path-mappings-add-dialog.component.scss'],
  standalone: false,
})
export class ApiPathMappingsAddDialogComponent implements OnInit {
  public tabLabels = {
    AddPathMapping: 'Path',
    SwaggerDocument: 'Swagger Document',
  };
  public pathFormGroup: UntypedFormGroup;
  public swaggerDocs: Page[];
  public selectedSwaggerDoc: string;
  private api: ApiV1 | ApiV2;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPathMappingsAddDialogData,
    private readonly dialogRef: MatDialogRef<ApiPathMappingsAddDialogData>,
    private readonly apiService: ApiService,
    private readonly apiV2Service: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {
    this.api = dialogData.api;
    this.swaggerDocs = dialogData.swaggerDocs;
  }

  public ngOnInit(): void {
    this.pathFormGroup = this.initFormGroup();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public onAddPath(): void {
    this.apiV2Service
      .get(this.api.id)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => {
          if (this.selectedSwaggerDoc) {
            return this.apiService.importPathMappings(api.id, this.selectedSwaggerDoc, this.api.definitionVersion);
          } else {
            api.pathMappings.push(this.pathFormGroup.getRawValue().path);
            return this.apiV2Service.update(api.id, api);
          }
        }),
        catchError(() => {
          this.snackBarService.error(`An error occurred while trying to add the path mapping ${this.pathFormGroup.getRawValue().path}.`);
          return EMPTY;
        }),
        tap(() => {
          this.snackBarService.success(`The path mapping has been successfully added!`);
          this.dialogRef.close();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public onSelectedTab(event: MatTabChangeEvent) {
    this.selectedSwaggerDoc = null;
    this.pathFormGroup.reset();
    if (event.tab.textLabel === this.tabLabels.AddPathMapping) {
      this.pathFormGroup = this.initFormGroup();
    }
  }

  private initFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup({
      path: new UntypedFormControl(null, [Validators.required, isUnique(this.api.pathMappings), Validators.pattern('^/[a-zA-Z0-9:/._-]+')]),
    });
  }
}

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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { Api } from '../../../../../entities/api';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

export type ApiPortalDetailsDuplicateDialogData = {
  api: Api;
};

@Component({
  selector: 'api-portal-details-duplicate-dialog',
  template: require('./api-portal-details-duplicate-dialog.component.html'),
  styles: [require('./api-portal-details-duplicate-dialog.component.scss')],
})
export class ApiPortalDetailsDuplicateDialogComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public optionsCheckbox = [
    { id: 'groups', label: 'Groups', checked: true },
    { id: 'members', label: 'Members', checked: true },
    { id: 'pages', label: 'Pages', checked: true },
    { id: 'plans', label: 'Plans', checked: true },
  ];

  public duplicateApiForm: FormGroup;

  public apiId: string;
  public contextPathPlaceholder: string;
  public versionPlaceholder: string;

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalDetailsDuplicateDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalDetailsDuplicateDialogData,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.apiId = dialogData.api.id;
    this.contextPathPlaceholder = dialogData.api.proxy.virtual_hosts[0].path;
    this.versionPlaceholder = dialogData.api.version;

    this.duplicateApiForm = new FormGroup({
      contextPath: new FormControl('', [Validators.required], [this.apiService.contextPathValidator()]),
      version: new FormControl('', [Validators.required, this.apiService.versionValidator()]),
      options: new FormGroup(
        this.optionsCheckbox.reduce((acc, option) => {
          acc[option.id] = new FormControl(option.checked);
          return acc;
        }, {}),
      ),
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  async onDuplicate() {
    const configsFormValue = this.duplicateApiForm.value;

    this.apiService
      .duplicate(this.apiId, {
        context_path: configsFormValue.contextPath,
        version: configsFormValue.version,
        filtered_fields: this.optionsCheckbox.filter((option) => !configsFormValue.options[option.id]).map((option) => option.id),
      })
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('API duplicated successfully.')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message ?? 'An error occurred while duplicate the API.');
          return EMPTY;
        }),
      )
      .subscribe((apiDuplicated) => {
        this.dialogRef.close(apiDuplicated);
      });
  }
}

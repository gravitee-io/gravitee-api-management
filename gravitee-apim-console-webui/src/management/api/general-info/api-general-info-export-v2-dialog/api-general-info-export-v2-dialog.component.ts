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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';

import { Api } from '../../../../entities/management-api-v2';
import { ApiService } from '../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

export type ApiPortalDetailsExportV2DialogData = {
  api: Api;
};

@Component({
  selector: 'api-general-info-export-v2-dialog',
  templateUrl: './api-general-info-export-v2-dialog.component.html',
  styleUrls: ['./api-general-info-export-v2-dialog.component.scss'],
  standalone: false,
})
export class ApiGeneralInfoExportV2DialogComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public jsonOptions = [{ id: 'default', label: 'Current version', isDeprecated: false }];

  public jsonOptionsCheckbox = [
    { id: 'groups', label: 'Groups', checked: true },
    { id: 'members', label: 'Members', checked: true },
    { id: 'pages', label: 'Pages', checked: true },
    { id: 'plans', label: 'Plans', checked: true },
    { id: 'metadata', label: 'Metadata', checked: true },
  ];

  public exportJsonFrom: UntypedFormGroup;

  public apiId: string;
  public fileName: string;

  public selectedTabIndex = 0; // 0: Json ; 1: CRD

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalDetailsExportV2DialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalDetailsExportV2DialogData,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.apiId = dialogData.api.id;

    this.fileName = buildFileName(dialogData.api);

    this.exportJsonFrom = new UntypedFormGroup({
      exportVersion: new UntypedFormControl(this.jsonOptions[0].id),
      options: new UntypedFormGroup(
        this.jsonOptionsCheckbox.reduce((acc, option) => {
          acc[option.id] = new UntypedFormControl(option.checked);
          return acc;
        }, {}),
      ),
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  async onExport() {
    let export$: Observable<{ blob: Blob; fileName: string }>;
    const isJsonExport = this.selectedTabIndex === 0;

    if (isJsonExport) {
      const exportJsonFromValue = this.exportJsonFrom.value;

      const excludes = Object.entries(exportJsonFromValue.options)
        .filter(([_, value]) => !value)
        .map(([key]) => key);

      export$ = this.apiService
        .export(this.apiId, excludes, exportJsonFromValue.exportVersion)
        .pipe(map((blob) => ({ blob, fileName: `${this.fileName}.json` })));
    } else {
      export$ = this.apiService.exportCrd(this.apiId).pipe(map((blob) => ({ blob, fileName: `${this.fileName}-crd.yml` })));
    }

    export$
      .pipe(
        tap(({ blob, fileName }) => {
          const anchor = document.createElement('a');
          anchor.download = fileName;
          anchor.href = (window.webkitURL || window.URL).createObjectURL(blob);
          anchor.click();
        }),
        catchError(() => {
          this.snackBarService.error('An error occurred while export the API.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.dialogRef.close();
      });
  }

  public getJsonOptions(value: string) {
    return this.jsonOptions.find((option) => option.id === value);
  }
}

export const buildFileName = (api: Api) => {
  return `${api.name}-${api.apiVersion}`.replace(/[\s]/gi, '-').replace(/[^\w]/gi, '-');
};

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

import { ChangeDetectionStrategy, Component, Inject, OnDestroy } from '@angular/core';
import { FormControl, FormGroup, UntypedFormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';

import { Api } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

export type ApiGeneralDetailsExportV4DialogData = {
  api: Api;
};

export type ApiGeneralDetailsExportV4DialogResult = void;

type ExportFrom = {
  options: FormGroup<Record<string, FormControl<boolean>>>;
};

@Component({
  selector: 'api-general-info-export-v4-dialog',
  templateUrl: './api-general-info-export-v4-dialog.component.html',
  styleUrls: ['./api-general-info-export-v4-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ApiGeneralInfoExportV4DialogComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public optionsCheckbox = [
    { id: 'groups', label: 'Groups', checked: true },
    { id: 'members', label: 'Members', checked: true },
    { id: 'pages', label: 'Pages', checked: true },
    { id: 'plans', label: 'Plans', checked: true },
    { id: 'metadata', label: 'Metadata', checked: true },
  ];

  public exportFrom: FormGroup<ExportFrom>;

  public apiId: string;
  public fileName: string;

  public selectedTabIndex = 0; // 0: Json ; 1: CRD

  constructor(
    private readonly dialogRef: MatDialogRef<ApiGeneralDetailsExportV4DialogData, ApiGeneralDetailsExportV4DialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiGeneralDetailsExportV4DialogData,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {
    this.apiId = dialogData.api.id;

    this.fileName = buildFileName(dialogData.api);

    this.exportFrom = new FormGroup({
      options: new FormGroup(
        this.optionsCheckbox.reduce((acc, option) => {
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
      const exportFromValue = this.exportFrom.value;

      const excludeAdditionalData = Object.entries(exportFromValue.options)
        .filter(([_, value]) => !value)
        .map(([key, _]) => key);

      export$ = this.apiService
        .export(this.apiId, {
          excludeAdditionalData,
        })
        .pipe(map(blob => ({ blob, fileName: `${this.fileName}.json` })));
    } else {
      export$ = this.apiService.exportCRD(this.apiId).pipe(map(blob => ({ blob, fileName: `${this.fileName}-crd.yml` })));
    }

    export$
      .pipe(
        tap(({ blob, fileName }) => {
          const anchor = document.createElement('a');
          anchor.download = fileName;
          anchor.href = ((<any>window).webkitURL || (<any>window).URL).createObjectURL(blob);
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
}

export const buildFileName = (api: Api) => {
  return `${api.name}-${api.apiVersion}`.replace(/[\s]/gi, '-').replace(/[^\w]/gi, '-');
};

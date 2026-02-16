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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { GioLicenseService, License } from '@gravitee/ui-particles-angular';
import { takeUntil, tap } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';

import { ApimFeature, UTMTags } from '../../../../shared/components/gio-license/gio-license-data';
import { ConnectorVM } from '../../../../entities/management-api-v2';

export type ApiEntrypointsV4AddDialogComponentData = {
  entrypoints: ConnectorVM[];
  hasHttpListener: boolean;
};
@Component({
  selector: 'api-entrypoints-v4-add-dialog',
  templateUrl: './api-entrypoints-v4-add-dialog.component.html',
  styleUrls: ['./api-entrypoints-v4-add-dialog.component.scss'],
  standalone: false,
})
export class ApiEntrypointsV4AddDialogComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public apiHasHttpListener: boolean;
  public entrypoints: ConnectorVM[];
  public formGroup: UntypedFormGroup;
  public showContextPathForm = false;
  public contextPathFormGroup: UntypedFormGroup;
  public requiresUpgrade = false;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;

  constructor(
    public dialogRef: MatDialogRef<ApiEntrypointsV4AddDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: ApiEntrypointsV4AddDialogComponentData,
    private formBuilder: UntypedFormBuilder,
    private licenseService: GioLicenseService,
  ) {
    this.entrypoints = data.entrypoints;
    this.apiHasHttpListener = data.hasHttpListener;
  }

  ngOnInit(): void {
    this.formGroup = this.formBuilder.group({
      selectedEntrypointsIds: this.formBuilder.control([], [Validators.required]),
    });

    this.formGroup
      .get('selectedEntrypointsIds')
      .valueChanges.pipe(
        tap(selectedEntrypointsIds => {
          this.requiresUpgrade = selectedEntrypointsIds
            .map(id => this.entrypoints.find(entrypoint => entrypoint.id === id))
            .some(entrypoint => !entrypoint.deployed);
          this.license$ = this.licenseService.getLicense$();
          this.isOEM$ = this.licenseService.isOEM$();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  save() {
    const selectedEntrypointsIds: string[] = this.formGroup.getRawValue().selectedEntrypointsIds;
    const isHttpEntrypointSelected = this.entrypoints
      .filter(e => selectedEntrypointsIds.includes(e.id))
      .some(e => e.supportedListenerType === 'HTTP');

    if (!this.apiHasHttpListener && isHttpEntrypointSelected) {
      this.contextPathFormGroup = this.formBuilder.group({
        contextPath: this.formBuilder.control([], [Validators.required]),
      });
      this.showContextPathForm = true;
    } else {
      this.dialogRef.close({ selectedEntrypoints: this.formGroup.getRawValue().selectedEntrypointsIds });
    }
  }

  saveWithContextPath() {
    this.dialogRef.close({
      selectedEntrypoints: this.formGroup.getRawValue().selectedEntrypointsIds,
      paths: this.contextPathFormGroup.getRawValue().contextPath,
    });
  }
  cancel() {
    this.dialogRef.close([]);
  }

  onRequestUpgrade() {
    this.licenseService.openDialog({ feature: ApimFeature.APIM_EN_MESSAGE_REACTOR, context: UTMTags.GENERAL_ENTRYPOINT_CONFIG });
  }
  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}

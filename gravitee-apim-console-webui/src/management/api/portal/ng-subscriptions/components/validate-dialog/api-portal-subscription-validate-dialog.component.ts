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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup } from '@angular/forms';

export interface ApiPortalSubscriptionAcceptDialogData {
  apiId: string;
  applicationId: string;
  canUseCustomApiKey: boolean;
  sharedApiKeyMode: boolean;
}
export interface ApiPortalSubscriptionAcceptDialogResult {
  start: Date;
  end: Date;
  message: string;
  customApiKey: string;
}
@Component({
  selector: 'api-portal-subscription-validate-dialog',
  template: require('./api-portal-subscription-validate-dialog.component.html'),
  styles: [require('./api-portal-subscription-validate-dialog.component.scss')],
})
export class ApiPortalSubscriptionValidateDialogComponent implements OnInit {
  data: ApiPortalSubscriptionAcceptDialogData;
  form: FormGroup = new FormGroup({});
  minDate: Date;

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalSubscriptionValidateDialogComponent, ApiPortalSubscriptionAcceptDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionAcceptDialogData,
  ) {
    this.data = dialogData;
  }

  ngOnInit(): void {
    this.minDate = new Date();

    this.form = new FormGroup({
      range: new FormGroup({
        start: new FormControl(undefined),
        end: new FormControl(undefined),
      }),
      message: new FormControl(''),
      apiKey: new FormControl(''),
    });
  }

  onClose() {
    this.dialogRef.close({
      start: this.form.getRawValue().range.start,
      end: this.form.getRawValue().range.end,
      message: this.form.getRawValue().message,
      customApiKey: this.form.getRawValue().apiKey,
    });
  }
}

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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface ApiPortalSubscriptionRenewDialogData {
  canUseCustomApiKey: boolean;
  applicationId: string;
  apiId: string;
}

export interface ApiPortalSubscriptionRenewDialogResult {
  customApiKey: string;
}
@Component({
  selector: 'api-portal-subscription-renew-dialog',
  templateUrl: './api-portal-subscription-renew-dialog.component.html',
  styleUrls: ['./api-portal-subscription-renew-dialog.component.scss'],
  standalone: false,
})
export class ApiPortalSubscriptionRenewDialogComponent implements OnInit {
  data: ApiPortalSubscriptionRenewDialogData;
  form: UntypedFormGroup = new UntypedFormGroup({});

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalSubscriptionRenewDialogComponent, ApiPortalSubscriptionRenewDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionRenewDialogData,
  ) {
    this.data = dialogData;
  }

  ngOnInit() {
    this.form = new UntypedFormGroup({
      apiKey: new UntypedFormControl(''),
    });
  }

  onClose() {
    this.dialogRef.close({ customApiKey: this.form.getRawValue().apiKey });
  }
}

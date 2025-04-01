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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';

export interface ApiPortalSubscriptionExpireApiKeyDialogData {
  expirationDate: Date;
}

export interface ApiPortalSubscriptionExpireApiKeyDialogResult {
  expirationDate: Date;
}
@Component({
  selector: 'api-portal-subscription-expire-api-key-dialog',
  templateUrl: './api-portal-subscription-expire-api-key-dialog.component.html',
  styleUrls: ['./api-portal-subscription-expire-api-key-dialog.component.scss'],
  standalone: false,
})
export class ApiPortalSubscriptionExpireApiKeyDialogComponent implements OnInit {
  expirationDate: Date;
  form: UntypedFormGroup;
  minDate: Date;
  constructor(
    private readonly dialogRef: MatDialogRef<
      ApiPortalSubscriptionExpireApiKeyDialogComponent,
      ApiPortalSubscriptionExpireApiKeyDialogResult
    >,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionExpireApiKeyDialogData,
  ) {
    this.expirationDate = dialogData.expirationDate;
  }

  ngOnInit() {
    this.minDate = new Date();
    this.form = new UntypedFormGroup({
      expirationDate: new UntypedFormControl(this.expirationDate),
    });
  }

  onClose() {
    this.dialogRef.close({ expirationDate: this.form.getRawValue().expirationDate });
  }
}

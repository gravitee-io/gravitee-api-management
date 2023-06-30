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
import { AfterViewInit, ChangeDetectorRef, Component, Inject, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApiKeyValidationComponent } from '../api-key-validation/api-key-validation.component';

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
  template: require('./api-portal-subscription-renew-dialog.component.html'),
  styles: [require('./api-portal-subscription-renew-dialog.component.scss')],
})
export class ApiPortalSubscriptionRenewDialogComponent implements AfterViewInit {
  data: ApiPortalSubscriptionRenewDialogData;
  form: FormGroup = new FormGroup({});
  @ViewChild('ApiKeyInput') apiKeyValidationComponent: ApiKeyValidationComponent;

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalSubscriptionRenewDialogComponent, ApiPortalSubscriptionRenewDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionRenewDialogData,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {
    this.data = dialogData;
  }

  ngAfterViewInit() {
    if (this.data.canUseCustomApiKey) {
      this.form.addControl('apiKey', this.apiKeyValidationComponent.apiKey);
      this.apiKeyValidationComponent.apiKey.setParent(this.form);
    }
    this.changeDetectorRef.detectChanges();
  }

  onClose() {
    this.dialogRef.close({ customApiKey: this.form.getRawValue().apiKey?.input });
  }
}

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

import { PlanSecurityType } from '../../../../../../entities/management-api-v2';

export interface ApiPortalSubscriptionChangeEndDateDialogData {
  applicationName: string;
  securityType: PlanSecurityType;
  currentEndDate: Date;
}
export interface ApiPortalSubscriptionChangeEndDateDialogResult {
  endDate: Date;
}
@Component({
  selector: 'api-portal-subscription-change-end-date-dialog',
  templateUrl: './api-portal-subscription-change-end-date-dialog.component.html',
  styleUrls: ['./api-portal-subscription-change-end-date-dialog.component.scss'],
  standalone: false,
})
export class ApiPortalSubscriptionChangeEndDateDialogComponent implements OnInit {
  form: UntypedFormGroup;
  data: ApiPortalSubscriptionChangeEndDateDialogData;
  minDate: Date = new Date();

  constructor(
    private readonly dialogRef: MatDialogRef<
      ApiPortalSubscriptionChangeEndDateDialogComponent,
      ApiPortalSubscriptionChangeEndDateDialogResult
    >,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionChangeEndDateDialogData,
  ) {
    this.data = dialogData;
  }

  ngOnInit(): void {
    this.form = new UntypedFormGroup({
      endDate: new UntypedFormControl(this.data.currentEndDate),
    });
  }

  onClose() {
    this.dialogRef.close({ endDate: this.form.getRawValue().endDate });
  }
}

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
import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';

export interface ApiPortalSubscriptionRejectDialogResult {
  reason?: string;
}
@Component({
  selector: 'api-portal-subscription-reject-dialog',
  templateUrl: './api-portal-subscription-reject-dialog.component.html',
  styleUrls: ['./api-portal-subscription-reject-dialog.component.scss'],
  standalone: false,
})
export class ApiPortalSubscriptionRejectDialogComponent implements OnInit {
  form: UntypedFormGroup;
  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalSubscriptionRejectDialogComponent, ApiPortalSubscriptionRejectDialogResult>,
  ) {}

  ngOnInit() {
    this.form = new UntypedFormGroup({
      reason: new UntypedFormControl(''),
    });
  }

  onClose() {
    this.dialogRef.close({ reason: this.form.getRawValue().reason });
  }
}

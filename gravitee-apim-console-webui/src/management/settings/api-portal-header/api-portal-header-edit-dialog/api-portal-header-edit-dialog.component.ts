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

import { ApiPortalHeader } from '../../../../entities/apiPortalHeader';

export interface ApiPortalHeaderEditDialogResult {
  name: string;
  value: string;
}
export type ApiPortalHeaderDialogData = ApiPortalHeaderEditDialogResult;

@Component({
  selector: 'api-portal-header-edit-dialog',
  templateUrl: './api-portal-header-edit-dialog.component.html',
  styleUrls: ['./api-portal-header-edit-dialog.component.scss'],
  standalone: false,
})
export class ApiPortalHeaderEditDialogComponent implements OnInit {
  public isUpdate = false;

  constructor(
    public dialogRef: MatDialogRef<ApiPortalHeaderEditDialogComponent, ApiPortalHeaderEditDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: ApiPortalHeader,
  ) {}

  ngOnInit() {
    this.isUpdate = !!this.data.value && !!this.data.name;
  }

  onCancelClick(): void {
    this.dialogRef.close();
  }

  isDisabled() {
    return !(!!this.data.name && !!this.data.value);
  }
}

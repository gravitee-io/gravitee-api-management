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
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface ApiHistoryV4DeploymentCompareDialogData {
  left: { eventId: string; apiDefinition: string; version: string; hideRollback: boolean };
  right: { eventId: string; apiDefinition: string; version: string; hideRollback: boolean };
}

export type ApiHistoryV4DeploymentCompareDialogResult = null | { rollbackTo: string };

@Component({
  selector: 'app-deployment-compare-dialog',
  templateUrl: './api-history-v4-deployment-compare-dialog.component.html',
  styleUrls: ['./api-history-v4-deployment-compare-dialog.component.scss'],
  standalone: false,
})
export class ApiHistoryV4DeploymentCompareDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: ApiHistoryV4DeploymentCompareDialogData,
  ) {}
}

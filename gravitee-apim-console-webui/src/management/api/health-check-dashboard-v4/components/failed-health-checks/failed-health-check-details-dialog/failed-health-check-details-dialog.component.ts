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
import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { LowerCasePipe } from '@angular/common';

import { MaskSensitiveHeaderPipe } from './mask-sensitive-header.pipe';

import { HealthCheckLog, HealthCheckStep } from '../../../../../../entities/management-api-v2/api/v4/healthCheck';
import { BodyAccordionModule } from '../../../../api-traffic-v4/runtime-logs-details/components/components/api-runtime-logs-connection-log-details/components/response-body-accordion';

export type FailedHealthCheckDetailsDialogData = HealthCheckLog;

interface HeaderEntry {
  name: string;
  value: string;
}

interface StepViewModel {
  step: HealthCheckStep;
  requestHeaders: HeaderEntry[];
  responseHeaders: HeaderEntry[];
}

function extractHeaderEntries(headers: Record<string, string> | undefined): HeaderEntry[] {
  return Object.entries(headers ?? {}).map(([name, value]) => ({ name, value }));
}

@Component({
  selector: 'failed-health-check-details-dialog',
  imports: [MatDialogModule, MatButtonModule, MatExpansionModule, LowerCasePipe, BodyAccordionModule, MaskSensitiveHeaderPipe],
  templateUrl: './failed-health-check-details-dialog.component.html',
  styleUrl: './failed-health-check-details-dialog.component.scss',
})
export class FailedHealthCheckDetailsDialogComponent {
  protected readonly log: FailedHealthCheckDetailsDialogData = inject(MAT_DIALOG_DATA);

  protected readonly steps: StepViewModel[] = (this.log?.steps ?? []).map(step => ({
    step,
    requestHeaders: extractHeaderEntries(step.request?.headers),
    responseHeaders: extractHeaderEntries(step.response?.headers),
  }));
}

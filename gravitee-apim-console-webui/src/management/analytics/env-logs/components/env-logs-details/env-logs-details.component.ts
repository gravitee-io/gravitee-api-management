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
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { editor } from 'monaco-editor';
import { GioBannerModule, GioClipboardModule, GioMonacoEditorModule } from '@gravitee/ui-particles-angular';

import { fakeEnvLogs } from '../../models/env-log.fixture';
import { EnvLog } from '../../models/env-log.model';
import { EnvLogsDetailsRowComponent } from '../env-logs-details-row/env-logs-details-row.component';

@Component({
  selector: 'env-logs-details',
  templateUrl: './env-logs-details.component.html',
  styleUrl: './env-logs-details.component.scss',
  imports: [
    FormsModule,
    RouterModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatButtonModule,
    GioBannerModule,
    GioClipboardModule,
    GioMonacoEditorModule,
    EnvLogsDetailsRowComponent,
  ],
  standalone: true,
})
export class EnvLogsDetailsComponent {
  private readonly activatedRoute = inject(ActivatedRoute);

  log = signal<EnvLog | undefined>(undefined);

  requestHeaders = computed(() => this.formatHeaders(this.log()?.entrypointRequest?.headers));
  requestBody = computed(() => this.log()?.entrypointRequest?.body ?? '');
  gatewayRequestHeaders = computed(() => this.formatHeaders(this.log()?.endpointRequest?.headers));
  gatewayRequestBody = computed(() => this.log()?.endpointRequest?.body ?? '');
  responseHeaders = computed(() => this.formatHeaders(this.log()?.entrypointResponse?.headers));
  responseBody = computed(() => this.log()?.entrypointResponse?.body ?? '');
  gatewayResponseHeaders = computed(() => this.formatHeaders(this.log()?.endpointResponse?.headers));
  gatewayResponseBody = computed(() => this.log()?.endpointResponse?.body ?? '');

  readonly monacoEditorOptions: editor.IStandaloneEditorConstructionOptions = {
    renderLineHighlight: 'none',
    hideCursorInOverviewRuler: true,
    overviewRulerBorder: false,
    occurrencesHighlight: 'off',
    selectionHighlight: false,
    readOnly: true,
    scrollbar: {
      vertical: 'hidden',
      horizontal: 'hidden',
      useShadows: false,
    },
  };

  constructor() {
    const logId = this.activatedRoute.snapshot.params.logId;
    if (!logId) {
      return;
    }

    // TODO: Replace with a real API call to fetch the log by ID from the backend.
    const foundLog = fakeEnvLogs().find((l) => l.id === logId);
    if (foundLog) {
      this.log.set(foundLog);
    }
  }

  private formatHeaders(headers?: Record<string, string[]>): { key: string; value: string }[] {
    if (!headers) {
      return [];
    }

    return Object.entries(headers)
      .map(([key, values]) => ({
        key,
        value: values.join(', '),
      }))
      .sort((a, b) => a.key.localeCompare(b.key));
  }
}

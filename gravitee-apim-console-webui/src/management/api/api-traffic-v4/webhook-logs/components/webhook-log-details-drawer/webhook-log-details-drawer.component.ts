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

import { Component, computed, input, output, signal } from '@angular/core';
import { CommonModule, DatePipe, KeyValuePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { CdkCopyToClipboard } from '@angular/cdk/clipboard';

import { WebhookLog } from '../../models';

@Component({
  selector: 'webhook-log-details-drawer',
  templateUrl: './webhook-log-details-drawer.component.html',
  styleUrls: ['./webhook-log-details-drawer.component.scss'],
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTableModule, DatePipe, KeyValuePipe, CdkCopyToClipboard],
})
export class WebhookLogDetailsDrawerComponent {
  log = input<WebhookLog>();

  closeDrawer = output<void>();

  isExpanded = signal(true);
  isAttemptsExpanded = signal(true);
  copied = signal(false);

  requestHeaders = computed(() => {
    const headers = this.log()?.additionalMetrics?.string_webhook_request_headers;
    return this.parseJSON(headers, {});
  });

  responseHeaders = computed(() => {
    const headers = this.log()?.additionalMetrics?.string_webhook_response_headers;
    return this.parseJSON(headers, {});
  });

  requestBody = computed(() => this.log()?.additionalMetrics?.string_webhook_request_body || '');

  responseBody = computed(() => this.log()?.additionalMetrics?.string_webhook_response_body || '');

  retryTimeline = computed(() => {
    const timeline = this.log()?.additionalMetrics?.string_webhook_retry_timeline;
    const parsed = this.parseJSON<Array<{ attempt: number; timestamp: number; status: number }>>(timeline, []);
    return parsed.reverse();
  });

  formatDuration(ms: number): string {
    if (ms < 1000) {
      return `${ms} ms`;
    }
    return `${(ms / 1000).toFixed(1)} s`;
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes}B`;
    } else if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)}KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  }

  onCopied(): void {
    this.copied.set(true);
    setTimeout(() => {
      this.copied.set(false);
    }, 2000);
  }

  toggleExpand(): void {
    this.isExpanded.update((v) => !v);
  }

  toggleAttemptsExpanded(): void {
    this.isAttemptsExpanded.update((v) => !v);
  }

  close(): void {
    this.isExpanded.set(false);
    this.closeDrawer.emit();
  }

  private parseJSON<T>(jsonString: string | null | undefined, defaultValue: T): T {
    if (!jsonString) {
      return defaultValue;
    }
    try {
      return JSON.parse(jsonString) as T;
    } catch {
      return defaultValue;
    }
  }
}

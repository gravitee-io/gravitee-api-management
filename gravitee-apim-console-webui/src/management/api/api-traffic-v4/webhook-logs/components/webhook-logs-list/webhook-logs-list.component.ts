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
import { Component, computed, input, TemplateRef, viewChild, output } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { DatePipe } from '@angular/common';

import { LogsListBaseComponent, LogsListColumnDef } from '../../../components/logs-list-base/logs-list-base.component';
import { GioTableWrapperPagination } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../../../../entities/management-api-v2';
import { WebhookLog } from '../../models/webhook-logs.models';

@Component({
  selector: 'webhook-logs-list',
  standalone: true,
  templateUrl: './webhook-logs-list.component.html',
  styleUrls: ['./webhook-logs-list.component.scss'],
  imports: [LogsListBaseComponent, MatIcon, MatTooltipModule, MatButtonModule, DatePipe],
})
export class WebhookLogsListComponent {
  logs = input.required<WebhookLog[]>();
  pagination = input.required<Pagination>();
  hasDlqConfigured = input<boolean | undefined>();

  paginationUpdated = output<GioTableWrapperPagination>();
  logDetailsClicked = output<WebhookLog>();

  readonly columns = computed<LogsListColumnDef[]>(() => {
    const templates = {
      timestamp: this.timestampTemplate(),
      status: this.statusTemplate(),
      callbackUrl: this.callbackUrlTemplate(),
      application: this.applicationTemplate(),
      sentToDlq: this.sentToDlqTemplate(),
      actions: this.actionsTemplate(),
    };

    if (Object.values(templates).some(tpl => !tpl)) {
      return [];
    }

    const columns: LogsListColumnDef[] = [
      { id: 'timestamp', label: 'Timestamp', template: templates.timestamp! },
      { id: 'status', label: 'Status', template: templates.status! },
      { id: 'callbackUrl', label: 'Callback URL', template: templates.callbackUrl! },
      { id: 'application', label: 'Application', template: templates.application! },
    ];

    if (this.hasDlqConfigured() ?? false) {
      columns.push({ id: 'sentToDlq', label: 'Sent to DLQ', template: templates.sentToDlq! });
    }

    columns.push({ id: 'actions', label: '', template: templates.actions! });

    return columns;
  });

  timestampTemplate = viewChild<TemplateRef<WebhookLog>>('timestampTemplate');
  statusTemplate = viewChild<TemplateRef<WebhookLog>>('statusTemplate');
  callbackUrlTemplate = viewChild<TemplateRef<WebhookLog>>('callbackUrlTemplate');
  applicationTemplate = viewChild<TemplateRef<WebhookLog>>('applicationTemplate');
  sentToDlqTemplate = viewChild<TemplateRef<WebhookLog>>('sentToDlqTemplate');
  actionsTemplate = viewChild<TemplateRef<WebhookLog>>('actionsTemplate');
}

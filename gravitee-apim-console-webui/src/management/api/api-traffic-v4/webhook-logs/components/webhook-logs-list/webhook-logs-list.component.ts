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
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

import { LogsListBaseComponent, LogsListColumnDef } from '../../../components/logs-list-base/logs-list-base.component';
import { GioTableWrapperPagination } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../../../../entities/management-api-v2';
import { WebhookLog } from '../../models/webhook-logs.models';

// TODO: Backend Integration Required
// - Verify that the WebhookLog interface matches the backend API response structure
// - Ensure all column data (timestamp, status, callbackUrl, application, sentToDlq) maps correctly from backend
// - Update if backend returns different field names or structure
// - Consider adding error handling for malformed log data
// - Verify pagination component works correctly with backend pagination metadata

@Component({
  selector: 'webhook-logs-list',
  standalone: true,
  templateUrl: './webhook-logs-list.component.html',
  styleUrls: ['./webhook-logs-list.component.scss'],
  imports: [LogsListBaseComponent, MatIcon, MatTooltipModule, MatButtonModule, RouterLink, DatePipe],
})
export class WebhookLogsListComponent {
  logs = input.required<WebhookLog[]>();
  pagination = input.required<Pagination>();

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

    if (Object.values(templates).some((tpl) => !tpl)) {
      return [];
    }

    return [
      { id: 'timestamp', label: 'Timestamp', template: templates.timestamp! },
      { id: 'status', label: 'Status', template: templates.status! },
      { id: 'callbackUrl', label: 'Callback URL', template: templates.callbackUrl! },
      { id: 'application', label: 'Application', template: templates.application! },
      { id: 'sentToDlq', label: 'Sent to DLQ', template: templates.sentToDlq! },
      { id: 'actions', label: '', template: templates.actions! },
    ];
  });

  timestampTemplate = viewChild<TemplateRef<WebhookLog>>('timestampTemplate');
  statusTemplate = viewChild<TemplateRef<WebhookLog>>('statusTemplate');
  callbackUrlTemplate = viewChild<TemplateRef<WebhookLog>>('callbackUrlTemplate');
  applicationTemplate = viewChild<TemplateRef<WebhookLog>>('applicationTemplate');
  sentToDlqTemplate = viewChild<TemplateRef<WebhookLog>>('sentToDlqTemplate');
  actionsTemplate = viewChild<TemplateRef<WebhookLog>>('actionsTemplate');
}

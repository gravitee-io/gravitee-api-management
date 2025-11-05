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

import { Component, input, output, TemplateRef, ViewChild } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

import { Pagination } from '../../../../../../entities/management-api-v2';
import { GioTableWrapperPagination } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTooltipOnEllipsisModule } from '../../../../../../shared/components/gio-tooltip-on-ellipsis/gio-tooltip-on-ellipsis.module';
import { WebhookLog } from '../../models';
import { LogsListBaseComponent, LogsListColumnDef } from '../../../components/logs-list-base';

@Component({
  selector: 'webhook-logs-list',
  templateUrl: './webhook-logs-list.component.html',
  styleUrls: ['./webhook-logs-list.component.scss'],
  standalone: true,
  imports: [LogsListBaseComponent, MatIcon, MatButtonModule, MatTooltipModule, RouterLink, DatePipe, GioTooltipOnEllipsisModule],
})
export class WebhookLogsListComponent {
  logs = input.required<WebhookLog[]>();
  pagination = input.required<Pagination>();

  logDetailsClicked = output<WebhookLog>();
  paginationUpdated = output<GioTableWrapperPagination>();

  @ViewChild('timestampTpl', { static: true }) timestampTpl!: TemplateRef<unknown>;
  @ViewChild('statusTpl', { static: true }) statusTpl!: TemplateRef<unknown>;
  @ViewChild('callbackUrlTpl', { static: true }) callbackUrlTpl!: TemplateRef<unknown>;
  @ViewChild('applicationTpl', { static: true }) applicationTpl!: TemplateRef<unknown>;
  @ViewChild('durationTpl', { static: true }) durationTpl!: TemplateRef<unknown>;
  @ViewChild('actionsTpl', { static: true }) actionsTpl!: TemplateRef<unknown>;

  get columns(): LogsListColumnDef[] {
    return [
      { id: 'timestamp', label: 'Timestamp', template: this.timestampTpl },
      { id: 'status', label: 'Status', template: this.statusTpl },
      { id: 'callbackUrl', label: 'Callback URL', template: this.callbackUrlTpl },
      { id: 'application', label: 'Application', template: this.applicationTpl },
      { id: 'duration', label: 'Duration', template: this.durationTpl },
      { id: 'actions', label: '', template: this.actionsTpl },
    ];
  }
}

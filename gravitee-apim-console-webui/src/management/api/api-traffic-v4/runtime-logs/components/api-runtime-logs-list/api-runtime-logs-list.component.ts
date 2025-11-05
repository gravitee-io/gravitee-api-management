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

import { Component, computed, input, output, TemplateRef, ViewChild } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

import { ConnectionLog, Pagination } from '../../../../../../entities/management-api-v2';
import { GioTableWrapperPagination } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { LogsListBaseComponent, LogsListColumnDef } from '../../../components/logs-list-base';

@Component({
  selector: 'api-runtime-logs-list',
  templateUrl: './api-runtime-logs-list.component.html',
  styleUrls: ['./api-runtime-logs-list.component.scss'],
  standalone: true,
  imports: [LogsListBaseComponent, MatIcon, MatButtonModule, MatTooltipModule, RouterLink, DatePipe],
})
export class ApiRuntimeLogsListComponent {
  logs = input.required<ConnectionLog[]>();
  pagination = input.required<Pagination>();
  isMessageApi = input.required<boolean>();

  paginationUpdated = output<GioTableWrapperPagination>();

  @ViewChild('timestampTpl', { static: true }) timestampTpl!: TemplateRef<unknown>;
  @ViewChild('methodTpl', { static: true }) methodTpl!: TemplateRef<unknown>;
  @ViewChild('statusTpl', { static: true }) statusTpl!: TemplateRef<unknown>;
  @ViewChild('uriTpl', { static: true }) uriTpl!: TemplateRef<unknown>;
  @ViewChild('applicationTpl', { static: true }) applicationTpl!: TemplateRef<unknown>;
  @ViewChild('planTpl', { static: true }) planTpl!: TemplateRef<unknown>;
  @ViewChild('responseTimeTpl', { static: true }) responseTimeTpl!: TemplateRef<unknown>;
  @ViewChild('endpointTpl', { static: true }) endpointTpl!: TemplateRef<unknown>;
  @ViewChild('issuesTpl', { static: true }) issuesTpl!: TemplateRef<unknown>;
  @ViewChild('actionsTpl', { static: true }) actionsTpl!: TemplateRef<unknown>;

  columns = computed<LogsListColumnDef[]>(() => {
    const baseColumns: LogsListColumnDef[] = [
      { id: 'timestamp', label: 'Timestamp', template: this.timestampTpl },
      { id: 'method', label: 'Method', template: this.methodTpl },
      { id: 'status', label: 'Status', template: this.statusTpl },
      { id: 'URI', label: 'URI', template: this.uriTpl },
      { id: 'application', label: 'Application', template: this.applicationTpl },
      { id: 'plan', label: 'Plan', template: this.planTpl },
      { id: 'responseTime', label: 'Response time', template: this.responseTimeTpl },
    ];

    if (!this.isMessageApi()) {
      baseColumns.push({ id: 'endpoint', label: 'Endpoint reached', template: this.endpointTpl });
    }

    baseColumns.push({ id: 'issues', label: 'Issues', template: this.issuesTpl }, { id: 'actions', label: '', template: this.actionsTpl });

    return baseColumns;
  });
}

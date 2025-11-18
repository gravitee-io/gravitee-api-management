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
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

import { ConnectionLog, Pagination } from '../../../../../../entities/management-api-v2';
import { GioTableWrapperPagination } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTooltipOnEllipsisModule } from '../../../../../../shared/components/gio-tooltip-on-ellipsis/gio-tooltip-on-ellipsis.module';
import { LogsListBaseComponent, LogsListColumnDef } from '../../../../api-traffic-v4/components/logs-list-base/logs-list-base.component';

@Component({
  selector: 'api-runtime-logs-list',
  templateUrl: './api-runtime-logs-list.component.html',
  styleUrls: ['./api-runtime-logs-list.component.scss'],
  standalone: true,
  imports: [MatIcon, MatTooltipModule, RouterLink, MatButtonModule, DatePipe, GioTooltipOnEllipsisModule, LogsListBaseComponent],
})
export class ApiRuntimeLogsListComponent {
  logs = input.required<ConnectionLog[]>();
  pagination = input.required<Pagination>();
  isMessageApi = input.required<boolean>();
  paginationUpdated = output<GioTableWrapperPagination>();

  readonly columns = computed<LogsListColumnDef[]>(() => {
    const requiredTemplates = [
      this.timestampTemplate(),
      this.methodTemplate(),
      this.statusTemplate(),
      this.uriTemplate(),
      this.applicationTemplate(),
      this.planTemplate(),
      this.responseTimeTemplate(),
      this.issuesTemplate(),
      this.actionsTemplate(),
    ];

    if (requiredTemplates.some((template) => !template)) {
      return [];
    }

    const baseColumns: LogsListColumnDef[] = [
      { id: 'timestamp', label: 'Timestamp', template: this.timestampTemplate()! },
      { id: 'method', label: 'Method', template: this.methodTemplate()! },
      { id: 'status', label: 'Status', template: this.statusTemplate()! },
      { id: 'URI', label: 'URI', template: this.uriTemplate()! },
      { id: 'application', label: 'Application', template: this.applicationTemplate()! },
      { id: 'plan', label: 'Plan', template: this.planTemplate()! },
      { id: 'responseTime', label: 'Response time', template: this.responseTimeTemplate()! },
    ];

    if (!this.isMessageApi()) {
      const endpointTemplate = this.endpointTemplate();
      if (!endpointTemplate) {
        return [];
      }
      baseColumns.push({ id: 'endpoint', label: 'Endpoint reached', template: endpointTemplate });
    }

    baseColumns.push(
      { id: 'issues', label: 'Issues', template: this.issuesTemplate()! },
      { id: 'actions', label: '', template: this.actionsTemplate()! },
    );

    return baseColumns;
  });

  timestampTemplate = viewChild<TemplateRef<ConnectionLog>>('timestampTemplate');
  methodTemplate = viewChild<TemplateRef<ConnectionLog>>('methodTemplate');
  statusTemplate = viewChild<TemplateRef<ConnectionLog>>('statusTemplate');
  uriTemplate = viewChild<TemplateRef<ConnectionLog>>('uriTemplate');
  applicationTemplate = viewChild<TemplateRef<ConnectionLog>>('applicationTemplate');
  planTemplate = viewChild<TemplateRef<ConnectionLog>>('planTemplate');
  responseTimeTemplate = viewChild<TemplateRef<ConnectionLog>>('responseTimeTemplate');
  endpointTemplate = viewChild<TemplateRef<ConnectionLog>>('endpointTemplate');
  issuesTemplate = viewChild<TemplateRef<ConnectionLog>>('issuesTemplate');
  actionsTemplate = viewChild<TemplateRef<ConnectionLog>>('actionsTemplate');
}

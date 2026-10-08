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

import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Audit, AuditProperty, Pagination } from '../../../../../entities/management-api-v2';
import { isAuditTarget } from '../../../../../entities/audit/auditTargets';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

type AuditRow = Audit & { targets: AuditProperty[] };

@Component({
  selector: 'api-audits-table',
  templateUrl: './api-audits-table.component.html',
  styleUrls: ['./api-audits-table.component.scss'],
  standalone: false,
})
export class ApiAuditsTableComponent {
  protected displayedColumns = ['date', 'user', 'event', 'targets', 'patch'];
  protected readonly JSON = JSON;
  protected tableWrapperFilters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  protected total = 0;
  protected auditRows: AuditRow[];

  @Input()
  public set audits(audits: Audit[]) {
    this.auditRows = audits?.map(audit => ({ ...audit, targets: extractTargets(audit) }));
  }

  @Input()
  public isLoading: boolean;

  @Output()
  public paginationChange = new EventEmitter<Pagination>();

  protected tableWrapperFilterChange(event: GioTableWrapperFilters) {
    this.paginationChange.emit({
      page: event.pagination.index,
      perPage: event.pagination.size,
    });
  }

  @Input()
  public set pagination(pagination: Pagination) {
    this.total = pagination.totalCount;
    this.tableWrapperFilters = {
      ...this.tableWrapperFilters,
      pagination: {
        index: pagination.page,
        size: pagination.perPage,
      },
    };
  }
}

function extractTargets(audit: Audit): AuditProperty[] {
  return (audit.properties ?? []).filter(property => isAuditTarget(property.key));
}

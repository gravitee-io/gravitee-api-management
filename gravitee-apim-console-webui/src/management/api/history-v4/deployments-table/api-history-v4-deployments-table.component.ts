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

import { Pagination, Event } from '../../../../entities/management-api-v2';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

@Component({
  selector: 'api-deployments-table',
  templateUrl: './api-history-v4-deployments-table.component.html',
  styleUrls: ['./api-history-v4-deployments-table.component.scss'],
})
export class ApiHistoryV4DeploymentsTableComponent {
  DEPLOYMENT_NUMBER_PROPERTY = 'DEPLOYMENT_NUMBER';
  LABEL_PROPERTY = 'DEPLOYMENT_LABEL';
  displayedColumns: string[] = ['version', 'createdAt', 'user', 'label', 'action'];
  protected tableWrapperPagination: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  protected total = 0;

  @Input()
  public deployments: Event[];

  @Output()
  public paginationChange = new EventEmitter<Pagination>();

  @Output()
  public rollback = new EventEmitter<string>();

  protected tableWrapperPaginationChange(event: GioTableWrapperFilters) {
    this.paginationChange.emit({
      page: event.pagination.index,
      perPage: event.pagination.size,
    });
  }

  @Input()
  public set pagination(pagination: Pagination) {
    this.total = pagination.totalCount;
    this.tableWrapperPagination = {
      ...this.tableWrapperPagination,
      pagination: {
        index: pagination.page,
        size: pagination.perPage,
      },
    };
  }
}

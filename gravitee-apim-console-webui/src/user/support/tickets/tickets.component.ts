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
import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { TicketService } from '../../../services-ngx/ticket.service';
import { GioTableWrapperFilters, Sort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { TicketsParam } from '../../../entities/ticket/ticketsParam';

type TableData = {
  id: string;
  createdAt: string;
  api: string;
  application: string;
  subject: string;
};

@Component({
  selector: 'tickets',
  templateUrl: './tickets.component.html',
  styleUrls: ['./tickets.component.scss'],
  standalone: false,
})
export class TicketsComponent implements OnInit {
  displayedColumns = ['createdAt', 'api', 'application', 'subject'];
  filteredTableData: TableData[] = [];
  nbTotalInstances = 0;

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private readonly unsubscribe$ = new Subject<void>();

  constructor(private readonly ticketService: TicketService) {}

  ngOnInit(): void {
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap((filters: GioTableWrapperFilters) => {
          const params: TicketsParam = {
            page: filters.pagination.index,
            size: filters.pagination.size,
            order: this.buildOrderParam(filters.sort),
          };
          return this.ticketService.search(params);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((searchResult) => {
        this.nbTotalInstances = searchResult.totalElements;
        this.filteredTableData = searchResult.content.map((ticket) => ({
          id: ticket.id,
          createdAt: ticket.created_at,
          api: ticket.api,
          application: ticket.application,
          subject: ticket.subject,
        }));
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  private buildOrderParam(sort: Sort): string {
    if (!sort) {
      return '-created_at';
    }

    let sortField = sort.active;
    if (sortField === 'createdAt') {
      sortField = 'created_at';
    }

    return sort.direction === 'asc' ? sortField : '-' + sortField;
  }
}

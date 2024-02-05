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
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { isEqual } from 'lodash';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, switchMap } from 'rxjs/operators';

import { ApiAuditFilter } from './components/api-audits-filter-form/api-audits-filter-form.component';

import { ApiAuditsV2Service } from '../../../../services-ngx/api-audits-v2.service';
import { Pagination } from '../../../../entities/management-api-v2';
import { ApiEventsV2Service } from '../../../../services-ngx/api-events-v2.service';

interface SearchQuery {
  query: ApiAuditFilter;
  pagination: Pagination;
}

const INITIAL_PAGINATION: Pagination = {
  page: 1,
  perPage: 10,
};

const INITIAL_SEARCH_QUERY: SearchQuery = {
  query: {
    events: [],
    from: null,
    to: null,
  },
  pagination: {
    ...INITIAL_PAGINATION,
  },
};

@Component({
  selector: 'app-audit-logs',
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.scss'],
})
export class AuditLogsComponent {
  private apiId = this.activatedRoute.snapshot.params.apiId;

  protected filters = new BehaviorSubject<SearchQuery>(INITIAL_SEARCH_QUERY);

  protected apiAudits$ = this.filters.pipe(
    distinctUntilChanged(isEqual),
    switchMap(({ query, pagination }) => {
      return this.apiAuditsV2Service.searchApiAudit(this.apiId, {
        events: query.events?.join(','),
        from: query.from,
        to: query.to,
        ...pagination,
      });
    }),
  );
  protected auditEvents$ = this.apiAuditsV2Service.listAllApiAuditEvents(this.apiId);
  protected apiEvents$ = this.apiEventsV2Service.searchApiEvents(this.apiId, { types: 'START_API,STOP_API,PUBLISH_API' });

  constructor(
    private readonly apiAuditsV2Service: ApiAuditsV2Service,
    private readonly apiEventsV2Service: ApiEventsV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  protected queryChange(event: ApiAuditFilter) {
    this.filters.next({ ...this.filters.getValue(), query: event, pagination: INITIAL_PAGINATION });
  }

  protected paginationChange(event: Pagination) {
    this.filters.next({ ...this.filters.getValue(), pagination: event });
  }
}

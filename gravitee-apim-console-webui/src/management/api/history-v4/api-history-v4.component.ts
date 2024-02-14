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
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, switchMap } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { SearchApiEventParam } from '../../../entities/management-api-v2';
import { ApiEventsV2Service } from '../../../services-ngx/api-events-v2.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';

const INITIAL_SEARCH_PARAM: SearchApiEventParam = {
  page: 1,
  perPage: 10,
  types: 'PUBLISH_API',
};

@Component({
  selector: 'app-api-history-v4',
  templateUrl: './api-history-v4.component.html',
  styleUrls: ['./api-history-v4.component.scss'],
})
export class ApiHistoryV4Component {
  private apiId = this.activatedRoute.snapshot.params.apiId;

  protected filter$ = new BehaviorSubject<SearchApiEventParam>(INITIAL_SEARCH_PARAM);

  protected apiEvents$ = this.filter$.pipe(
    distinctUntilChanged(isEqual),
    switchMap(({ page, perPage }) => {
      return this.eventsService.searchApiEvents(this.apiId, { page: page, perPage: perPage, types: 'PUBLISH_API' });
    }),
  );

  protected currentAPI$ = this.apiService.getLastApiFetch(this.apiId);

  constructor(
    private readonly eventsService: ApiEventsV2Service,
    private readonly apiService: ApiV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  protected paginationChange(searchParam: SearchApiEventParam) {
    this.filter$.next({ ...this.filter$.getValue(), page: searchParam.page, perPage: searchParam.perPage });
  }
}

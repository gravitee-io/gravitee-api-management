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
import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';

import { UIRouterState } from '../../../ajs-upgraded-providers';
import { Api } from '../../../entities/api';
import { PagedResult } from '../../../entities/pagedResult';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiService } from '../../../services-ngx/api.service';

type ApisTableDS = { id: string; name: string }[];

@Component({
  selector: 'api-list',
  template: require('./api-list.component.html'),
  styles: [require('./api-list.component.scss')],
})
export class ApiListComponent implements OnInit {
  displayedColumns = ['name'];

  apisTableDSUnpaginatedLength = 0;
  apisTableDS: ApisTableDS = [];
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  constructor(@Inject(UIRouterState) private readonly $state: StateService, private readonly apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService
      .list()
      .pipe(
        tap((apisPage) => {
          this.apisTableDS = this.toApisTableDS(apisPage);
          this.apisTableDSUnpaginatedLength = apisPage.page.total_elements;
        }),
        catchError(() => of(new PagedResult<Api>())),
      )
      .subscribe();
  }

  onEditActionClicked(api: ApisTableDS[number]) {
    this.$state.go('management.apis.detail.portal.general', { apiId: api.id });
  }

  onAddApiClick() {
    this.$state.go('management.apis.new');
  }

  private toApisTableDS(api: PagedResult<Api>): ApisTableDS {
    return api.page.total_elements > 0
      ? api.data.map((api) => ({
          id: api.id,
          name: api.name,
        }))
      : [];
  }
}

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
import { Component, HostListener, OnInit } from '@angular/core';
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-row';
import { Pagination } from '@gravitee/ui-components/wc/gv-pagination';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Api, ApiService, ApisResponse } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { SearchQueryParam, SearchRequestParams } from '../../../utils/search-query-param.enum';
import { ConfigurationService } from '../../../services/configuration.service';
import { createPromiseList } from '../../../utils/utils';

@Component({
  selector: 'app-search',
  templateUrl: './catalog-search.component.html',
  styleUrls: ['./catalog-search.component.css'],
})
export class CatalogSearchComponent implements OnInit {
  searchForm: FormGroup;
  pageSize: number;
  pageSizes: Array<number>;
  paginationData: Pagination;
  apiResults: Promise<any>[];
  totalElements: number;
  currentPage;

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private config: ConfigurationService,
  ) {
    this.totalElements = 0;
    this.searchForm = this.formBuilder.group({ query: '' });
    this.pageSizes = config.get('pagination.size.values', [5, 10, 25, 50, 100]);
  }

  ngOnInit() {
    this.activatedRoute.queryParamMap.subscribe(params => {
      if (params.has(SearchQueryParam.QUERY)) {
        const query = params.get(SearchQueryParam.QUERY);
        this.searchForm.value.query = query;
      }

      this.pageSize = params.has(SearchQueryParam.SIZE)
        ? parseInt(params.get(SearchQueryParam.SIZE), 10)
        : this.config.get('pagination.size.default', 10);

      this.searchForm.reset({ query: this.searchForm.value.query });
      this.currentPage = params.get(SearchQueryParam.PAGE) || 1;
      if (this.searchForm.value.query.trim() !== '') {
        this._loadData();
      }
    });
  }

  _loadData() {
    const size = this.pageSize;
    const params = new SearchRequestParams(this.searchForm.value.query || '*', size, this.currentPage);
    const { list, deferredList } = createPromiseList(size);
    this.apiResults = list;
    this.apiService
      .searchApis(params)
      .toPromise()
      .then((apisResponse: ApisResponse) => {
        const pagination = apisResponse.metadata.pagination as unknown as Pagination;
        if (pagination) {
          this.paginationData = {
            size: this.pageSize,
            sizes: this.pageSizes,
            ...this.paginationData,
            ...pagination,
          };
          this.totalElements = this.paginationData.total;
        } else {
          this.paginationData = null;
          this.totalElements = 0;
        }

        apisResponse.data.forEach(api => {
          deferredList.shift().resolve(api);
        });
      })
      .catch(err => {
        if (err && err.interceptorFuture) {
          // avoid a duplicated notification with the same error
          err.interceptorFuture.cancel();
        }
        this.totalElements = 0;
        return [];
      })
      .finally(() => {
        deferredList.forEach(row => row.resolve(undefined));
      });
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page, size }) {
    const queryParams = new SearchRequestParams(this.activatedRoute.snapshot.queryParamMap.get(SearchQueryParam.QUERY), size, page);
    this.router.navigate([], { queryParams });
  }

  onSubmitSearch() {
    if (this.searchForm.valid) {
      const query = this.searchForm.value.query || this.activatedRoute.snapshot.queryParamMap.get(SearchQueryParam.QUERY);
      const queryParams = new SearchRequestParams(query, this.searchForm.value.size);
      this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
    }
  }

  goToApi(api: Promise<Api>) {
    api.then(_api => {
      this.router.navigate(['/catalog/api/' + _api.id], { queryParamsHandling: 'merge' });
    });
  }
}

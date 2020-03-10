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
import { Component, OnInit, HostListener } from '@angular/core';

import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-row';
import '@gravitee/ui-components/wc/gv-pagination';

import { FormGroup, FormBuilder } from '@angular/forms';
import { ApiService, ApisResponse, Api } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { SearchQueryParam, SearchRequestParams } from '../../../utils/search-query-param.enum';
import { ConfigurationService } from '../../../services/configuration.service';
import { delay } from '../../../utils/utils';
import { TimeTooLongError } from '../../../exceptions/TimeTooLongError';

@Component({
  selector: 'app-search',
  templateUrl: './catalog-search.component.html',
  styleUrls: ['./catalog-search.component.css']
})
export class CatalogSearchComponent implements OnInit {

  searchForm: FormGroup;
  pageSizes: Array<any>;
  paginationData: any;
  apiResults: Array<Promise<Api>>;
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
    this.searchForm = this.formBuilder.group({ query: '', size: '' });
    this.searchForm.value.size =
      this.pageSizes = config.get('pagination.size.values');
  }

  ngOnInit() {
    this.activatedRoute.queryParamMap.subscribe(params => {
      if (params.has(SearchQueryParam.QUERY)) {
        const query = params.get(SearchQueryParam.QUERY);
        this.searchForm.value.query = query;
      }

      const size = params.has(SearchQueryParam.SIZE) ?
        parseInt(params.get(SearchQueryParam.SIZE), 10) :
        this.config.get('pagination.size.default');

      const closestPageSize = this.pageSizes.reduce((prev, curr) => {
        return (Math.abs(curr - size) < Math.abs(prev - size) ? curr : prev);
      });
      this.searchForm.value.size = closestPageSize;

      this.searchForm.reset({ query: this.searchForm.value.query, size: this.searchForm.value.size });
      this.currentPage = params.get(SearchQueryParam.PAGE) || 1;
      if (this.searchForm.value.query.trim() !== '') {
        Promise.race([this._loadData(), delay(500)]).catch((err) => {
          if (err instanceof TimeTooLongError) {
            this.apiResults = new Array<Promise<Api>>(this.searchForm.value.size);
          }
        });
      }
    });
  }

  _loadData() {
    const params = new SearchRequestParams(this.searchForm.value.query || '*', this.searchForm.value.size, this.currentPage);
    return this.apiService.searchApis(params)
      .toPromise()
      .then((apisResponse: ApisResponse) => {
        if (apisResponse.data.length) {
          this.apiResults = apisResponse.data.map((a) => {
            return Promise.resolve(a);
          });
        } else {
          // @ts-ignore
          this.apiResults = [];
        }
        this.paginationData = apisResponse.metadata.pagination;
        this.totalElements = (this.paginationData ? this.paginationData.total : 0);
      })
      .catch((err) => {
        // @ts-ignore
        this.apiResults = this.apiResults.map(() => Promise.reject(err));
        this.totalElements = 0;
      });
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    if (this.paginationData.current_page !== page) {
      const queryParams = new SearchRequestParams(
        this.activatedRoute.snapshot.queryParamMap.get(SearchQueryParam.QUERY),
        this.searchForm.value.size, page);
      this.router.navigate([], { queryParams });
    }
  }

  onSubmitSearch() {
    if (this.searchForm.valid) {
      const query = this.searchForm.value.query || this.activatedRoute.snapshot.queryParamMap.get(SearchQueryParam.QUERY);
      const queryParams = new SearchRequestParams(query, this.searchForm.value.size);
      this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
    }
  }

  goToApi(api: Promise<Api>) {
    api.then((_api) => {
      this.router.navigate(['/catalog/api/' + _api.id]);
    });
  }
}

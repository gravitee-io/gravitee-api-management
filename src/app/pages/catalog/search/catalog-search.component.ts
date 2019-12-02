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
import '@gravitee/ui-components/wc/gv-row-api';
import '@gravitee/ui-components/wc/gv-pagination';

import { FormGroup, FormBuilder } from '@angular/forms';
import { ApiService, ApisResponse, Api } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { SearchQueryParam, SearchRequestParams } from '../../../utils/search-query-param.enum';
import { ConfigurationService } from '../../../services/configuration.service';
import { LoaderService } from '../../../services/loader.service';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';

@Component({
  selector: 'app-search',
  templateUrl: './catalog-search.component.html',
  styleUrls: ['./catalog-search.component.css']
})
export class CatalogSearchComponent implements OnInit {

  searchForm: FormGroup;
  pageSizes: [];
  paginationData: any;
  apiResults: Array<Promise<Api>>;
  totalElements: string;
  currentPage;
  paginationSize: number;

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private config: ConfigurationService,
    private apiLabelsPipe: ApiLabelsPipe,
    public loaderService: LoaderService,
  ) {
    this.searchForm = this.formBuilder.group({ query: '' });
    this.paginationSize = config.get('pagination.size.default');
    this.pageSizes = config.get('pagination.size.values');
  }

  ngOnInit() {
    this.activatedRoute.queryParamMap.subscribe(params => {
      if (params.has(SearchQueryParam.QUERY)) {
        const query = params.get(SearchQueryParam.QUERY);
        this.searchForm.reset({ query });
      }
      if (params.has(SearchQueryParam.SIZE)) {
        const size = parseInt(params.get(SearchQueryParam.SIZE), 10);
        const closestPageSize = this.pageSizes.reduce((prev, curr) => {
          return (Math.abs(parseInt(curr, 10) - size) < Math.abs(parseInt(prev, 10) - size) ? curr : prev);
        });
        this.paginationSize = closestPageSize;

      }
      this.currentPage = params.get(SearchQueryParam.PAGE) || 1;
      this._loadData();
    });
  }

  _loadData() {
    this.apiResults = new Array(this.paginationSize);
    this.apiService.searchApis(new SearchRequestParams(this.searchForm.value.query, this.paginationSize, this.currentPage))
      .subscribe({
        next: (apisResponse: ApisResponse) => {
          this.apiResults = apisResponse.data.map((a) => {
            a.labels = this.apiLabelsPipe.transform(a);
            return Promise.resolve(a);
          });
          this.paginationData = apisResponse.metadata.pagination;
          this.totalElements = (this.paginationData ? this.paginationData.total : '0');
        },
        error: (err) => {
          // @ts-ignore
          this.apiResults.fill(() => Promise.reject(err));
        }
      });
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    if (this.paginationData.current_page !== page) {
      const queryParams = new SearchRequestParams(this.searchForm.value.query, this.paginationSize, page);
      this.router.navigate([], { queryParams });
    }
  }

  onChangePageSize(event) {
    this.paginationSize = event.target.value;
    this.onSubmitSearch();
  }

  onSubmitSearch() {
    if (this.searchForm.valid && this.searchForm.value.query !== '') {
      const queryParams = new SearchRequestParams(this.searchForm.value.query, this.paginationSize);
      this.router.navigate([], { queryParams });
    }
  }
}

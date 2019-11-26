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
import '@gravitee/ui-components/wc/gv-tag';
import '@gravitee/ui-components/wc/gv-pagination';

import { FormGroup, FormBuilder } from '@angular/forms';
import { ApiService, ApisResponse, Api } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from 'src/environments/environment';
import { SearchQueryParam, SearchRequestParams } from '../../../utils/search-query-param.enum';

@Component({
  selector: 'app-search',
  templateUrl: './catalog-search.component.html',
  styleUrls: ['./catalog-search.component.css']
})
export class CatalogSearchComponent implements OnInit {

  searchForm: FormGroup;
  pageSizes = environment.pagination.sizes;
  paginationData: any;
  paginationLinks: any;
  apiList: Api[];
  totalElements: string;
  currentPage;
  paginationSize: string;

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {
    this.searchForm = this.formBuilder.group({ query: '' });
    this.paginationSize = environment.pagination.default;
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
      this.currentPage = params.get(SearchQueryParam.PAGE);

      this.refreshList();
    });
  }

  refreshList() {
    this.apiService.searchApis(new SearchRequestParams(this.searchForm.value.query, this.paginationSize, this.currentPage))
      .subscribe((apisResponse: ApisResponse) => {
          this.apiList = apisResponse.data;
          this.paginationData = apisResponse.metadata.pagination;
          this.totalElements = (this.paginationData ? this.paginationData.total : '0');

          if (apisResponse.links) {
            this.paginationLinks = {
              first: this._getLink(apisResponse, 'first'),
              prev: this._getLink(apisResponse, 'prev'),
              next: this._getLink(apisResponse, 'next'),
              last: this._getLink(apisResponse, 'last')
            };
          }
        }
      );
  }

  private _getLink(apisResponse: ApisResponse, linkName: string) {
    return apisResponse.links && apisResponse.links[linkName] ? apisResponse.links[linkName].match(/page=(\w+)/)[1] : null;
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ link }) {
    const queryParams = new SearchRequestParams(this.searchForm.value.query, this.paginationSize, link);
    this.router.navigate([], { queryParams });
  }

  @HostListener(':gv-pagination:size', ['$event.detail'])
  _onChangePageSize({ size }) {
    this.paginationSize = size;
    this.onSubmitSearch();
  }

  onSubmitSearch() {
    if (this.searchForm.valid && this.searchForm.value.query !== '') {
      const queryParams = new SearchRequestParams(this.searchForm.value.query, this.paginationSize);
      this.router.navigate([], { queryParams });
    }
  }
}

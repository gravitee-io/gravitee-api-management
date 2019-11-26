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

import { FormGroup, FormBuilder } from '@angular/forms';
import { ApiService, ApisResponse, Api } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-search',
  templateUrl: './catalog-search.component.html',
  styleUrls: ['./catalog-search.component.css']
})
export class CatalogSearchComponent implements OnInit {

  searchForm: FormGroup;
  paginationForm: FormGroup;
  pageSizes = environment.pagination.sizes;
  paginationData: any;
  paginationLinks: any;
  apiList: Api[];
  totalElements: string;
  currentPage;

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.searchForm = this.formBuilder.group({
      query: ''
    });
    this.paginationForm = this.formBuilder.group({
      paginationSize: [ environment.pagination.default ]
    });
  }

  ngOnInit() {
    this.route.queryParams
      .subscribe(params => {
        if (params.q) {
          this.searchForm.reset({ query: params.q });
        }
        if (params.size) {
          const closestPageSize = this.pageSizes.reduce((prev, curr) => {
            return (Math.abs(parseInt(curr, 10) - params.size) < Math.abs(parseInt(prev, 10) - params.size) ? curr : prev);
          });
          this.paginationForm.reset({ paginationSize: closestPageSize });
        }
        if (params.page) {
          this.currentPage = params.page;
        }

        this.refreshList();
      });
  }

  refreshList() {
    this.apiService.searchApis(
      {
        q: this.searchForm.value.query,
        page: this.currentPage,
        size: this.paginationForm.value.paginationSize
      }
    ).subscribe(
      (apisResponse: ApisResponse) =>  {
        this.apiList = apisResponse.data;
        this.totalElements = (apisResponse.metadata.pagination ? apisResponse.metadata.pagination.total : '0');
        this.paginationData = apisResponse.metadata.pagination;
        if (apisResponse.links) {
          this.paginationLinks = {
            first: (apisResponse.links.first ? apisResponse.links.first.match(/page=(\w+)/)[1] : undefined),
            prev: (apisResponse.links.prev ? apisResponse.links.prev.match(/page=(\w+)/)[1] : undefined),
            next: (apisResponse.links.next ? apisResponse.links.next.match(/page=(\w+)/)[1] : undefined),
            last: (apisResponse.links.last ? apisResponse.links.last.match(/page=(\w+)/)[1] : undefined)
          };
        }
      }
    );
  }

  changePage(newPage) {
    this.router.navigate(['catalog/search'], {
      queryParams: {
        q: this.searchForm.value.query,
        size: this.paginationForm.value.paginationSize,
        page: newPage
      }
    });
  }

  search() {
    if (this.searchForm.valid && this.searchForm.value.query !== '') {
      this.router.navigate(['catalog/search'], {
        queryParams: {
          q: this.searchForm.value.query,
          size: this.paginationForm.value.paginationSize
        }
      });
    }
  }
}

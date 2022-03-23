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
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-autocomplete';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit, ViewChild } from '@angular/core';

import { SearchQueryParam, SearchRequestParams } from '../../utils/search-query-param.enum';
import { ApiService, ApisResponse } from '../../../../projects/portal-webclient-sdk/src/lib';

@Component({
  selector: 'app-gv-search-api',
  templateUrl: './gv-search-api.component.html',
})
export class GvSearchApiComponent implements OnInit {
  @ViewChild('searchAutocomplete', { static: true }) searchAutocomplete;
  query: string;
  options: any;
  private _selected: boolean;

  constructor(public router: Router, public activatedRoute: ActivatedRoute, private apiService: ApiService) {
    this._selected = false;
  }

  ngOnInit() {
    this.options = [];
    this.activatedRoute.queryParamMap.subscribe(queryParamMap => {
      if (queryParamMap.has(SearchQueryParam.QUERY)) {
        this.query = queryParamMap.get(SearchQueryParam.QUERY);
      } else {
        this.query = '';
      }
    });
  }

  onSearch({ detail }) {
    return this.apiService
      .searchApis(new SearchRequestParams(detail, 5))
      .toPromise()
      .then((apisResponse: ApisResponse) => {
        if (apisResponse.data.length) {
          this.options = apisResponse.data.map(a => {
            const row = document.createElement('gv-row');
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            row.small = true;
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            row.item = a;
            return { value: a.name, element: row, id: a.id };
          });
        } else {
          this.options = [];
        }
      })
      .catch(err => {
        if (err && err.interceptorFuture) {
          // avoid a duplicated notification with the same error
          err.interceptorFuture.cancel();
        }
      });
  }

  onSelect({ detail }) {
    this._selected = true;
    setTimeout(() => {
      this.router.navigate(['/catalog/api/' + detail.id]).then(() => {
        this._selected = false;
        this.searchAutocomplete.nativeElement.reset();
      });
    }, 300);
  }

  onSubmit({ detail }) {
    if (this._selected === false && detail.trim() !== '') {
      const queryParams = {};
      queryParams[SearchQueryParam.QUERY] = detail;
      this.router.navigate(['/catalog/search'], { queryParams });
    }
  }
}

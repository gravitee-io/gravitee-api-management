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
import { ApiService, Api, CategoryApiQuery, PortalService } from '@gravitee/ng-portal-webclient';
import '@gravitee/ui-components/wc/gv-promote-api';
import '@gravitee/ui-components/wc/gv-card-api-full';
import '@gravitee/ui-components/wc/gv-card-api';
import '@gravitee/ui-components/wc/gv-select';
import '@gravitee/ui-components/wc/gv-option';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ActivatedRoute, Router } from '@angular/router';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';

@Component({
  selector: 'app-all',
  templateUrl: './filtered-catalog.component.html',
  styleUrls: ['./filtered-catalog.component.css']
})
export class FilteredCatalogComponent implements OnInit {

  static readonly RANDOM_MAX_SIZE = 4;
  static readonly DEFAULT_VIEW = 'all';
  static readonly DEFAULT_DISPLAY = 'cards';

  private page: number;
  private size: number;

  allApis: Array<Promise<Api>>;
  randomList: Promise<any>[];
  promotedApi: Promise<{} | Api>;
  categoryApiQuery: CategoryApiQuery;
  views: Array<string>;
  currentView: string;
  paginationData: any;
  options: any[];
  currentDisplay: string;

  constructor(private apiService: ApiService,
              private portalService: PortalService,
              private translateService: TranslateService,
              private activatedRoute: ActivatedRoute,
              private router: Router,
              private apiStates: ApiStatesPipe,
              private apiLabels: ApiLabelsPipe) {
    // @ts-ignore
    this.allApis = [];
    this.randomList = [];
  }

  async ngOnInit() {
    this.promotedApi = new Promise(null);
    this.randomList = new Array(4);
    this.categoryApiQuery = this.activatedRoute.snapshot.data.categoryApiQuery;
    this.currentDisplay = this.activatedRoute.snapshot.queryParamMap.get('display') || FilteredCatalogComponent.DEFAULT_DISPLAY;

    this.options = [
      {
        id: FilteredCatalogComponent.DEFAULT_DISPLAY,
        icon: 'layout:layout-4-blocks',
        active: this.inDefaultDisplay(),
        title: i18n('catalog.display.cards'),
      },
      { id: 'list', icon: 'layout:layout-horizontal', active: !this.inDefaultDisplay(), title: i18n('catalog.display.list') }
    ].map((option) => {
      this.translateService.get(option.title).subscribe((title) => option.title = title);
      // @ts-ignore
      option.title = '';
      return option;
    });

    this.activatedRoute.queryParamMap.subscribe(params => {
      const view = params.get('view') || FilteredCatalogComponent.DEFAULT_VIEW;
      const page = parseInt(params.get(SearchQueryParam.PAGE), 10) || 1;
      const size = parseInt(params.get(SearchQueryParam.SIZE), 10) || 6;
      if (this.currentView !== view || this.page !== page || this.size !== size) {
        this.currentView = view;
        this.page = page;
        this.size = size;
        this.allApis = new Array(this.size);
        this._load();
      }
    });
  }

  getTitle() {
    return this.activatedRoute.snapshot.data.title;
  }

  async _load() {

    this.promotedApi = this.apiService.getApis({ size: 1, cat: this.categoryApiQuery })
      .toPromise()
      .then((response) => response.data[0] || {})
      .catch((err) => Promise.reject(err));

    this.apiService.getApis({ size: FilteredCatalogComponent.RANDOM_MAX_SIZE, _cat: this.categoryApiQuery })
      .subscribe({
        next: (apiResponse) => {
          this.randomList = apiResponse.data.map((a) => {
            // @ts-ignore
            a.states = this.apiStates.transform(a);
            return Promise.resolve(a);
          });
        },
        error: (err) => {
          // @ts-ignore
          this.randomList = this.randomList.fill(() => Promise.reject(err));
        }
      });

    this._loadCards();
  }

  async _loadCards() {
    const withPromotedApi = this.page === 1;
    const size = this.size + (withPromotedApi ? 1 : 0);

    forkJoin([
      this.apiService.getApis({ page: this.page, size, cat: this.categoryApiQuery, view: this.currentView }),
      this.translateService.get(i18n('catalog.defaultView')),
    ])
      .pipe(map(([allPage, label]) => [allPage.data, label, allPage.metadata]))
      .subscribe({
        next: ([allList, label, metadata]) => {
          const defaultViews = [{ value: FilteredCatalogComponent.DEFAULT_VIEW, label }];
          if (!this.inDefaultView()) {
            this.apiService.getApis({ page: this.page, size, cat: this.categoryApiQuery })
              .subscribe((apisResponse) => {
                // @ts-ignore
                this.views = defaultViews.concat(this._getViews(apisResponse.data.slice(withPromotedApi)));
              });
            this.allApis = allList.map((a) => {
              a.states = this.apiStates.transform(a);
              a.labels = this.apiLabels.transform(a);
              return Promise.resolve(a);
            });
          } else {
            // @ts-ignore
            this.views = defaultViews.concat(this._getViews(allList));
            this.allApis = allList.slice(withPromotedApi).map((a) => {
              a.states = this.apiStates.transform(a);
              a.labels = this.apiLabels.transform(a);
              return Promise.resolve(a);
            });
          }
          // @ts-ignore
          this.paginationData = metadata.pagination;
        },
        error: (err) => {
          // @ts-ignore
          this.allApis.fill(() => Promise.reject(err));
        }
      });
  }

  private _getViews(allPage) {
    return [].concat(...new Set([].concat(...allPage.map((api) => api.views))).values());
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    if (this.paginationData.current_page !== page) {
      const queryParams = {};
      queryParams[SearchQueryParam.PAGE] = page;
      this.router.navigate([], { relativeTo: this.activatedRoute, queryParams, queryParamsHandling: 'merge' });
    }
  }

  @HostListener(':gv-option:select', ['$event.detail'])
  _onChangeDisplay({ id }) {
    this.currentView = id;
    this.router.navigate([], { relativeTo: this.activatedRoute, queryParams: { display: id }, queryParamsHandling: 'merge' });
  }

  inDefaultDisplay() {
    return this.currentDisplay === FilteredCatalogComponent.DEFAULT_DISPLAY;
  }

  inDefaultView() {
    return this.currentView === FilteredCatalogComponent.DEFAULT_VIEW;
  }

  onSelectView({ target }) {
    const queryParams = { view: target.value };
    queryParams[SearchQueryParam.PAGE] = 1;
    this.router.navigate([], { relativeTo: this.activatedRoute, queryParams, queryParamsHandling: 'merge' });
  }

}

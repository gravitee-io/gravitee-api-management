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
import { Api, ApiService, CategoryApiQuery, PortalService, View, ApiMetrics } from '@gravitee/ng-portal-webclient';
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
import { ConfigurationService } from '../../../services/configuration.service';
import { FeatureEnum } from '../../../model/feature.enum';

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
  allApisMetrics: Array<Promise<ApiMetrics>>;
  randomList: Promise<any>[];
  promotedApi: Promise<any>;
  promotedMetrics: ApiMetrics;
  categoryApiQuery: CategoryApiQuery;
  views: Array<string>;
  currentView: string;
  paginationData: any;
  options: any[];
  currentDisplay: string;
  category: View;
  title: any;
  total: any;

  constructor(private apiService: ApiService,
              private portalService: PortalService,
              private translateService: TranslateService,
              private activatedRoute: ActivatedRoute,
              private router: Router,
              private apiStates: ApiStatesPipe,
              private apiLabels: ApiLabelsPipe,
              private config: ConfigurationService) {
    this.allApis = [];
    this.allApisMetrics = [];
    this.randomList = [];
  }

  async ngOnInit() {
    this.promotedApi = new Promise(null);
    this.randomList = new Array(4);
    this.currentDisplay = this.activatedRoute.snapshot.queryParamMap.get('display') || FilteredCatalogComponent.DEFAULT_DISPLAY;
    this._initDisplayOptions();

    this.categoryApiQuery = this.activatedRoute.snapshot.data.categoryApiQuery;

    this.activatedRoute.queryParamMap.subscribe(params => {
      const page = parseInt(params.get(SearchQueryParam.PAGE), 10) || 1;
      const size = parseInt(params.get(SearchQueryParam.SIZE), 10) || 6;
      const categoryPath = this._getCategoryPath();

      if (categoryPath) {
        this.currentView = categoryPath;
        if (this.page !== page || this.size !== size) {
          this.page = page;
          this.size = size;
          this.allApis = new Array(this.size);
          this._loadCategory();
        }
      } else {
        const view = params.get('view') || FilteredCatalogComponent.DEFAULT_VIEW;
        if (this.currentView !== view || this.page !== page || this.size !== size) {
          this.currentView = view;
          this.page = page;
          this.size = size;
          this.allApis = new Array(this.size);
          this._load();
        }
      }
    });
  }


  private _initDisplayOptions() {
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
  }

  async _load() {

    this.promotedApi = this._loadPromotedApi({ size: 1, cat: this.categoryApiQuery });

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
    this._loadCards(this.page === 1);
  }

  _loadPromotedApi(requestParams) {
    return this.apiService.getApis(requestParams)
      .toPromise()
      .then(async (response) => {
        const promoted = response.data[0];
        if (promoted) {
          this.promotedMetrics = await this.apiService.getApiMetricsByApiId({ apiId: promoted.id }).toPromise();
        }
        return promoted || {};
      })
      .catch((err) => Promise.reject(err));
  }

  async _loadCategory() {
    this.title = '';
    this.promotedApi = this._loadPromotedApi({ size: 1, view: this.currentView });
    this._loadCards(true);
    this.category = await this.portalService.getViewByViewId({ viewId: this.currentView }).toPromise();
    const _meta = await this.apiService.getApis({ page: this.page, size: 0, view: this.currentView }).toPromise();
    this.total = _meta.metadata.data.total;
    this.title = this.category.name;
  }

  async _loadCards(withPromotedApi: boolean) {
    const size = this.size + (withPromotedApi ? 1 : 0);

    forkJoin([
      this.apiService.getApis({ page: this.page, size, cat: this.categoryApiQuery, view: this.currentView }),
      this.translateService.get(i18n('catalog.defaultView')),
    ])
      .pipe(map(([allPage, label]) => [allPage.data, label, allPage.metadata]))
      .subscribe({
        next: async ([allList, label, metadata]) => {

          this.paginationData = metadata.pagination;

          if (this.hasViewMode() && this.views == null) {
            this.apiService.getApis({ size: -1, cat: this.categoryApiQuery })
              .subscribe((apisResponse) => {
                // @ts-ignore
                const views = this._getViews(apisResponse.data.slice(1));
                if (views.length > 0) {
                  // @ts-ignore
                  this.views = [{ value: FilteredCatalogComponent.DEFAULT_VIEW, label }].concat(views);
                } else {
                  this.views = [];
                }
              });
          }

          if (withPromotedApi) {
            // @ts-ignore
            allList = await this.promotedApi.then((promoted) => allList.filter((api) => promoted.id && api.id !== promoted.id));
          }

          this.allApis = allList.map((a) => {
            this.allApisMetrics.push(this.apiService.getApiMetricsByApiId({ apiId: a.id }).toPromise());
            a.states = this.apiStates.transform(a);
            a.labels = this.apiLabels.transform(a);
            return Promise.resolve(a);
          });


        },
        error: (err) => {
          // @ts-ignore
          this.allApis.fill(() => Promise.reject(err));
        }
      });
  }

  private _getViews(allPage) {
    return [].concat(...new Set([].concat(...allPage.map((api) => {
      return api.views;
    }))).values());
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
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { display: id },
      queryParamsHandling: 'merge'
    }).then(() => {
      this.currentDisplay = id;
    });
  }

  onSelectView({ target }) {
    const queryParams = { view: target.value };
    queryParams[SearchQueryParam.PAGE] = 1;
    this.router.navigate([], { relativeTo: this.activatedRoute, queryParams, queryParamsHandling: 'merge' });
  }

  inDefaultDisplay() {
    return this.currentDisplay === FilteredCatalogComponent.DEFAULT_DISPLAY;
  }

  hasViewMode() {
    return this.config.hasFeature(FeatureEnum.viewMode);
  }

  _getCategoryPath() {
    return this.activatedRoute.snapshot.params.categoryId;
  }

  inCategory() {
    return this._getCategoryPath() != null;
  }

  canFilter() {
    return !this.inCategory() && this.hasViewMode() && this.views && this.views.length > 0;
  }

  goToApi(api: Promise<Api>) {
    api.then((_api) => {
      this.router.navigate(['/catalog/api/' + _api.id]);
    });
  }

  getRouteTitle() {
    return this.activatedRoute.snapshot.data.title;
  }

}

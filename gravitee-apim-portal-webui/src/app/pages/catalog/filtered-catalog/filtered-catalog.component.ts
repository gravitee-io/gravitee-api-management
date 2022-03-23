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
import '@gravitee/ui-components/wc/gv-promote';
import '@gravitee/ui-components/wc/gv-card-list';
import '@gravitee/ui-components/wc/gv-card-full';
import '@gravitee/ui-components/wc/gv-card';
import '@gravitee/ui-components/wc/gv-select';
import '@gravitee/ui-components/wc/gv-option';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import {
  Api,
  ApiMetrics,
  ApiService,
  Category,
  FilterApiQuery,
  Page,
  PortalService,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';
import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';
import { ConfigurationService } from '../../../services/configuration.service';
import { FeatureEnum } from '../../../model/feature.enum';
import { createPromiseList } from '../../../utils/utils';

@Component({
  selector: 'app-all',
  templateUrl: './filtered-catalog.component.html',
  styleUrls: ['./filtered-catalog.component.css'],
})
export class FilteredCatalogComponent implements OnInit {
  static readonly RANDOM_MAX_SIZE = 4;
  static readonly DEFAULT_DISPLAY = 'cards';

  private page: number;
  private size: number;

  allApis: any[];
  randomList: any[];
  promotedApi: Promise<any>;
  promotedApiPath: string;
  promotedMetrics: ApiMetrics;
  filterApiQuery: FilterApiQuery;
  categories: Array<string>;
  currentCategory: string;
  currentCategoryDocumentationPage: Page;
  paginationData: any;
  options: any[];
  currentDisplay: string;
  empty: boolean;
  category: Category;
  fragments: any = {
    pagination: 'pagination',
    filter: 'filter',
  };
  private defaultCategory: { value; label };
  emptyIcon: string;
  emptyMessage: any;
  isDocHidden = true;

  constructor(
    private apiService: ApiService,
    private translateService: TranslateService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private apiStates: ApiStatesPipe,
    private apiLabels: ApiLabelsPipe,
    private config: ConfigurationService,
    private portalService: PortalService,
  ) {
    this.allApis = [];
  }

  ngOnInit() {
    this.emptyIcon = this.activatedRoute.snapshot.data.icon || 'general:sad';
    this.translateService
      .get(i18n('catalog.defaultCategory'))
      .toPromise()
      .then(
        label =>
          (this.defaultCategory = {
            value: '',
            label,
          }),
      );
    this.currentDisplay =
      this.activatedRoute.snapshot.queryParamMap.get('display') ||
      localStorage.getItem('user-display-mode') ||
      FilteredCatalogComponent.DEFAULT_DISPLAY;
    this._initDisplayOptions();

    this.filterApiQuery = this.activatedRoute.snapshot.data.filterApiQuery;

    this.activatedRoute.queryParamMap.subscribe(params => {
      const page = parseInt(params.get(SearchQueryParam.PAGE), 10) || 1;
      const size = parseInt(params.get(SearchQueryParam.SIZE), 10) || 6;
      const categoryPath = this._getCategoryPath();

      if (categoryPath) {
        this.currentCategory = categoryPath;
        if (this.page !== page || this.size !== size) {
          this.page = page;
          this.size = size;
          this._loadCategory();
        }
      } else {
        const category = params.get('category');
        if (this.currentCategory !== category || this.page !== page || this.size !== size) {
          this.currentCategory = category;
          this.page = page;
          this.size = size;
          this._load();
        }
      }

      if (this.category && this.category.page) {
        this.portalService
          .getPageByPageId({ pageId: this.category.page })
          .toPromise()
          .then(docPage => (this.currentCategoryDocumentationPage = docPage));
      }
    });
  }

  private _initDisplayOptions() {
    this.options = [
      {
        id: FilteredCatalogComponent.DEFAULT_DISPLAY,
        icon: 'layout:layout-4-blocks',
        title: i18n('catalog.display.cards'),
      },
      { id: 'list', icon: 'layout:layout-horizontal', title: i18n('catalog.display.list') },
    ].map(option => {
      this.translateService.get(option.title).subscribe(title => (option.title = title));
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      option.title = '';
      return option;
    });
  }

  _load() {
    if (this.page === 1 && this.hasPromotedApiMode()) {
      this.promotedApi = this._loadPromotedApi({ size: 1, filter: this.filterApiQuery, promoted: true });
    }
    return Promise.all([this._loadRandomList(), this._loadCards()]);
  }

  _loadRandomList() {
    const { list, deferredList } = createPromiseList(FilteredCatalogComponent.RANDOM_MAX_SIZE);
    this.randomList = list;

    return this.apiService
      .getApis({ size: FilteredCatalogComponent.RANDOM_MAX_SIZE, filter2: this.filterApiQuery })
      .toPromise()
      .then(apiResponse => {
        apiResponse.data.forEach(a => {
          // eslint-disable-next-line @typescript-eslint/ban-ts-comment
          // @ts-ignore
          a.states = this.apiStates.transform(a);
          deferredList.shift().resolve(a);
        });
        deferredList.forEach(d => d.resolve(undefined));
      })
      .catch(err => {
        deferredList.forEach(d => d.reject(err));
      });
  }

  async _loadPromotedApi(requestParams) {
    return this.apiService
      .getApis(requestParams)
      .toPromise()
      .then(async response => {
        const promoted = response.data[0];
        if (promoted) {
          this.promotedMetrics = await this.apiService.getApiMetricsByApiId({ apiId: promoted.id }).toPromise();
          this.promotedApiPath = `/catalog/api/${promoted.id}`;
          this.empty = false;
        }
        return promoted;
      });
  }

  _loadCategory() {
    this.category = this.activatedRoute.snapshot.data.category;
    if (this.hasPromotedApiMode()) {
      this.promotedApi = this._loadPromotedApi({ size: 1, category: this.currentCategory, promoted: true });
    }
    return this._loadCards();
  }

  async _loadCards() {
    const fetchPromoted = this.hasPromotedApiMode() ? false : undefined;

    return this.apiService
      .getApis({
        page: this.page,
        size: this.size,
        filter: this.filterApiQuery,
        category: this.currentCategory,
        promoted: fetchPromoted,
      })
      .toPromise()
      .then(async ({ data, metadata }) => {
        this.paginationData = metadata.pagination;

        this.allApis = data.map(api => {
          const metrics = this.apiService.getApiMetricsByApiId({ apiId: api.id }).toPromise();

          const item = metrics.then(() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            api.states = this.apiStates.transform(api);
            api.labels = this.apiLabels.transform(api);
            return api;
          });

          return { item, metrics };
        });

        if (this.hasCategoryMode() && this.categories == null) {
          this.apiService.listCategories({ filter: this.filterApiQuery }).subscribe(categoriesResponse => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            const categories = categoriesResponse.data.map(category => ({ value: category.id, label: category.name }));
            if (categories.length > 0) {
              // eslint-disable-next-line @typescript-eslint/ban-ts-comment
              // @ts-ignore
              this.categories = [this.defaultCategory].concat(categories);
            } else {
              this.categories = [];
            }
          });
        }
        return this.allApis;
      })
      .catch(() => {
        this.allApis = [];
      })
      .finally(() => {
        this.updateEmptyState(this.allApis);
      });
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    const queryParams = {};
    queryParams[SearchQueryParam.PAGE] = page;
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.fragments.pagination,
    });
  }

  @HostListener(':gv-option:select', ['$event.detail'])
  _onChangeDisplay({ id }) {
    this.router
      .navigate([], {
        relativeTo: this.activatedRoute,
        queryParams: { display: id },
        queryParamsHandling: 'merge',
        fragment: this.fragments.filter,
      })
      .then(() => {
        this.currentDisplay = id;
        localStorage.setItem('user-display-mode', id);
      });
  }

  onSelectCategory({ target }) {
    if (target.value !== '') {
      const queryParams = { category: target.value };
      queryParams[SearchQueryParam.PAGE] = 1;
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        queryParams,
        queryParamsHandling: 'merge',
        fragment: this.fragments.filter,
      });
    } else {
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        fragment: this.fragments.filter,
      });
    }
  }

  get layoutClassName() {
    const className = 'catalog__section';
    const viewMode = this.hasRandomCardsAside() ? 'random-aside' : 'all';
    const promotedMode = this.hasPromotedApiMode() ? 'promoted' : 'no-promoted';
    return `${className}__${viewMode}__${promotedMode}`;
  }

  hasRandomCardsAside() {
    return this.hasPromotedApiMode() && !this.empty && !this.inCategory() && !this.inCategoryAll();
  }

  get showCards() {
    return this.currentDisplay === FilteredCatalogComponent.DEFAULT_DISPLAY;
  }

  hasPromotedApiMode() {
    return this.config.hasFeature(FeatureEnum.promotedApiMode);
  }

  hasCategoryMode() {
    return this.config.hasFeature(FeatureEnum.categoryMode);
  }

  _getCategoryPath() {
    return this.activatedRoute.snapshot.params.categoryId;
  }

  inCategory() {
    return this._getCategoryPath() != null;
  }

  inCategoryAll() {
    return this.filterApiQuery === FilterApiQuery.ALL;
  }

  get canFilter() {
    return !this.inCategory() && this.hasCategoryMode() && this.categories && this.categories.length > 0;
  }

  toggleDocumentationPage($event: any) {
    $event.target.closest('.catalog__category__documentation').classList.toggle('hidden');
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  goToApi(api: Promise<Api>) {
    Promise.resolve(api).then(_api => {
      const queryParams = {};
      if (this.inCategory()) {
        queryParams[SearchQueryParam.CATEGORY] = this.category.id;
      } else {
        queryParams[SearchQueryParam.API_QUERY] = this.filterApiQuery;
      }
      this.router.navigate(['/catalog/api/' + _api.id], { queryParams });
    });
  }

  getRouteTitle() {
    return this.activatedRoute.snapshot.data.title;
  }

  goToSearchByTag(tag: string) {
    this.router.navigate(['catalog/search'], { queryParams: { q: `labels:"${tag}"` } });
  }

  async updateEmptyState(data = []) {
    const isEmpty = data.filter(Boolean).length === 0;
    if (isEmpty) {
      const key = this.inCategory() ? i18n('catalog.categories.emptyMessage') : `catalog.${this.filterApiQuery}.emptyMessage`;
      this.emptyMessage = await this.translateService.get(key).toPromise();
      this.empty = true;
    } else {
      this.empty = false;
    }
  }
}

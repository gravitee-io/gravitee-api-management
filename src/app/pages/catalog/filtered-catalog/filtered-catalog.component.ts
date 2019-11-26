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
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ActivatedRoute, Router } from '@angular/router';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-all',
  templateUrl: './filtered-catalog.component.html',
  styleUrls: ['./filtered-catalog.component.css']
})
export class FilteredCatalogComponent implements OnInit {

  static readonly RANDOM_MAX_SIZE = 4;
  static readonly DEFAULT_VIEW = 'all';

  public allApis: Array<Api>;
  public randomList: Array<Api>;
  public promotedApi: Api;
  public categoryApiQuery: CategoryApiQuery;
  public total: string;
  private page: number;
  private size: number;
  public views: Array<string>;
  public ratingEnabled: boolean;
  public currentView: string;


  constructor(private apiService: ApiService,
              private portalService: PortalService,
              private translateService: TranslateService,
              private activatedRoute: ActivatedRoute,
              private router: Router) {
    // @ts-ignore
    this.promotedApi = { _links: {} };
    this.currentView = FilteredCatalogComponent.DEFAULT_VIEW;
    this.allApis = [];
  }

  async ngOnInit() {
    this.categoryApiQuery = this.activatedRoute.snapshot.data.categoryApiQuery;
    const queryParamMap = this.activatedRoute.snapshot.queryParamMap;
    this.page = parseInt(queryParamMap.get(SearchQueryParam.PAGE), 10) || 1;
    this.size = parseInt(queryParamMap.get(SearchQueryParam.SIZE), 10) || 7;
    this.currentView = queryParamMap.get('view') || FilteredCatalogComponent.DEFAULT_VIEW;
    this._load();
  }

  getLabels(api) {
    return api.labels.map((label) => ({ value: label, major: true }));
  }

  getStates(api) {
    if (api.draft) {
      return [{ value: 'draft' }];
    }
    return [{ value: 'running', major: true }];
  }

  getTitle() {
    return this.activatedRoute.snapshot.data.title;
  }

  async _load() {
    forkJoin([
      this.apiService.getApis({ page: this.page, size: this.size, cat: this.categoryApiQuery, view: this.currentView }),
      this.apiService.getApis({ size: FilteredCatalogComponent.RANDOM_MAX_SIZE, _cat: this.categoryApiQuery }),
      this.translateService.get(i18n('filteredCatalog.defaultView'))
    ]).pipe(
      map(([allPage, all, label]) => [
        all.data.filter((api) => !allPage.data.some(({ id }) => (id === api.id))).slice(0, FilteredCatalogComponent.RANDOM_MAX_SIZE),
        allPage.data.splice(0, 1),
        allPage.data,
        [].concat(...new Set([].concat(...allPage.data.map((api) => api.views))).values()),
        label
      ])
    ).subscribe(([randomList, promotedApi, allList, views, label]) => {
      // @ts-ignore
      this.randomList = randomList;
      this.promotedApi = promotedApi[0];
      // @ts-ignore
      this.allApis = allList;
      // @ts-ignore
      this.views = [{ value: FilteredCatalogComponent.DEFAULT_VIEW, label }].concat(views.map((view) => ({ value: view })));
    });

    this.apiService.getApis({ cat: this.categoryApiQuery }).subscribe(({ metadata: { data: { total } } }) => {
      this.total = total;
    });

    this.portalService.getPortalConfiguration().subscribe((res) => {
      this.ratingEnabled = res.portal.rating.enabled === true;
    });
  }

  getViews() {
    return [{ value: FilteredCatalogComponent.DEFAULT_VIEW }].concat(this.views.map((view) => ({ value: view })));
  }

  hasViews() {
    return this.views && this.views.length > 2;
  }

  onSelectView({ target }) {
    this.router.navigate([], { relativeTo: this.activatedRoute, queryParams: { view: target.value } });
  }

  hasData() {
    return this.allApis.length > 0;
  }

}

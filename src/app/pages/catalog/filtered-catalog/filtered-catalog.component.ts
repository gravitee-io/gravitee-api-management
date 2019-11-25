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
import { ActivatedRoute, Router } from '@angular/router';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

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
  private views: Array<string>;
  public ratingEnabled: boolean;
  private currentView: string;


  constructor(private apiService: ApiService,
              private portalService: PortalService,
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

  async _load() {
    forkJoin([
      this.apiService.getApis({ page: this.page, size: this.size, cat: this.categoryApiQuery, view: this.currentView }),
      this.apiService.getApis({ size: FilteredCatalogComponent.RANDOM_MAX_SIZE, _cat: this.categoryApiQuery }),
    ]).pipe(
      map(([allPage, all]) => [
        all.data.filter((api) => !allPage.data.some(({ id }) => (id === api.id))).slice(0, FilteredCatalogComponent.RANDOM_MAX_SIZE),
        allPage.data.splice(0, 1),
        allPage.data,
        [].concat(...new Set([].concat(...allPage.data.map((api) => api.views))).values())
      ])
    ).subscribe(([randomList, promotedApi, allList, views]) => {
      this.randomList = randomList;
      this.promotedApi = promotedApi[0];
      this.allApis = allList;
      this.views = views;
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
    return this.views && this.views.length;
  }

  @HostListener(':gv-select:select', ['$event.detail'])
  onSelectView(view) {
    this.router.navigate([], { relativeTo: this.activatedRoute, queryParams: { view } });
  }

  hasData() {
    return this.allApis.length > 0;
  }

}

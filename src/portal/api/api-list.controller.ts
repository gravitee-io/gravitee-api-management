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
import * as _ from 'lodash';

import ViewService from "../../services/view.service";
import {IScope} from "angular";
import ApiService from "../../services/api.service";
import {StateParams, StateService, TransitionService} from '@uirouter/core';
import PortalService from "../../services/portal.service";

export class PortalApiListController {

  private query: string = '';
  private apis: any[];
  private views: any[];
  private view: any;
  private ratingEnabled: boolean;
  private selectedView: string;
  private tilesMode: boolean;
  private tilesModeKey = 'gv-tiles-mode';
  private apisLoading: boolean;
  private timer: any;
  private canceler: any;

  constructor (private $scope: IScope,
               private $state: StateService,
               private $stateParams: StateParams,
               private Constants,
               private ViewService: ViewService,
               private ApiService: ApiService,
               private PortalService: PortalService,
               private $window: ng.IWindowService,
               private resolvedApis,
               private resolvedViews,
               private $transitions: TransitionService,
               private $timeout: ng.ITimeoutService,
               private $q: ng.IQService) {
    'ngInject';

    this.$q = $q;
    if ($window.localStorage.getItem(this.tilesModeKey) === null) {
      if (Constants.portal && Constants.portal.apis) {
        this.tilesMode = Constants.portal.apis.tilesMode.enabled;
      } else {
        this.tilesMode = true;
      }
    } else {
      this.tilesMode = JSON.parse($window.localStorage.getItem(this.tilesModeKey));
    }
    this.query = $state.params.q;
    this.apis = resolvedApis.data;
    this.views = resolvedViews;
    this.ratingEnabled = this.ApiService.isRatingEnabled();
    let that = this;
    if($stateParams.view) {
      this.selectedView = $stateParams.view;
      this.view = _.find(this.views, function (view) {
        return that.selectedView === view.key;
      });
      this.sortByHighlightApi(that.view);
    } else {
      ViewService.getDefaultOrFirstOne().then( (response) => {
        that.selectedView = response.id;
        that.view = response;
        that.sortByHighlightApi(that.view);
      })
    }

    $scope.$watch('apisCtrl.query', (query: string, previousQuery: string) => {
      $timeout.cancel(this.timer);
      this.timer = $timeout(() => {
        if (query !== undefined && query !== previousQuery) {
          this.search();
        }
      }, 300);
    });
    this.canceler = $q.defer();
  }

  search() {
    // if search is already executed, cancel timer
    this.$timeout.cancel(this.timer);

    this.apisLoading = true;
    this.canceler.resolve();
    this.canceler = this.$q.defer();

    let promise;
    let promOpts = {timeout: this.canceler.promise};

    if (this.query === undefined || this.query.length === 0) {
      this.$state.transitionTo(
        this.$state.current,
        {q: this.query, view: 'all'},
        {notify: false});
      promise = this.ApiService.list('all', true, promOpts)
    } else {
      this.$state.transitionTo(
        this.$state.current,
        {q: this.query, view: 'results'},
        {notify: false});
      promise = this.PortalService.searchApis(this.query, promOpts);
    }

    let that = this;
    promise.then( (response) => {
      that.apis = response.data;
      that.apisLoading = false;
    });
  }

  goToApi(api) {
    this.$state.go('portal.api.detail', {apiId: api.id});
  }

  changeView(event, view) {
    event.stopPropagation();
    this.$state.go('portal.apilist', {view: view});
  }

  goToRating(event, api) {
    event.stopPropagation();
    this.$state.go('portal.api.rating', {apiId: api.id});
  }

  toggleDisplayMode() {
    this.tilesMode = !this.tilesMode;
    this.$window.localStorage.setItem(this.tilesModeKey, String(this.tilesMode));
  }

  sortByHighlightApi(view) {
    if (view && view.highlightApi) {
      this.apis = _.sortBy(this.apis, function(api) {
        return api.id === view.highlightApi ? 0 : 1;
      });
    }
  }

  getViewClass(api) {
    if (!api.views || api.views.length === 0) {
      return "";
    }

    return _.map(api.views, (view) => "api-card-view-" + view).join(' ');
  }
}

export default PortalApiListController;

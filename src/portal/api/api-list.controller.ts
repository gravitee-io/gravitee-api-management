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

export class PortalApiListController {

  private apis: any[];
  private views: any[];
  private view: any;
  private ratingEnabled: boolean;
  private selectedView: string;
  private tilesMode: boolean;
  private tilesModeKey = 'gv-tiles-mode';
  private hideApis: boolean;

  constructor (private $scope: IScope,
               private $state,
               private $stateParams,
               private Constants,
               private ViewService: ViewService,
               private ApiService: ApiService,
               private $window,
               private resolvedApis,
               private resolvedViews,
               private $transitions) {
    'ngInject';

    if ($window.sessionStorage.getItem(this.tilesModeKey) === null) {
      if (Constants.portal && Constants.portal.apis) {
        this.tilesMode = Constants.portal.apis.tilesMode.enabled;
      } else {
        this.tilesMode = true;
      }
    } else {
      this.tilesMode = JSON.parse($window.sessionStorage.getItem(this.tilesModeKey));
    }
    this.apis = resolvedApis.data;
    this.views = resolvedViews;
    this.ratingEnabled = this.ApiService.isRatingEnabled();
    let that = this;
    if($stateParams.view) {
      this.selectedView = $stateParams.view;
      this.view = _.find(this.views, function (view) {
        return that.selectedView === view.id;
      });
      this.sortByHighlightApi(that.view);
    } else {
      ViewService.getDefaultOrFirstOne().then( (response) => {
        that.selectedView = response.id;
        that.view = response;
        that.sortByHighlightApi(that.view);
      })
    }

    $transitions.onStart({to: $state.current.name}, () => {
      this.hideApis = true;
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
    this.$window.sessionStorage.setItem(this.tilesModeKey, this.tilesMode);
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

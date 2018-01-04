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
import {IScope} from 'angular';
import * as _ from 'lodash';
import ApiService from "../../services/api.service";
import ViewService from "../../services/view.service";

export class PortalApisController {

  private apis: any[];
  private views: any[];
  private view: any;
  private ratingEnabled: boolean;
  private selectedView: string;

  constructor (private resolvedApis,
               private resolvedViews,
               private $scope: IScope,
               private $state,
               private $stateParams,
               private Constants,
               private ViewService: ViewService,
               private ApiService: ApiService) {
    'ngInject';
    this.apis = resolvedApis.data;
    this.views = resolvedViews;
    this.ratingEnabled = this.ApiService.isRatingEnabled();
    let that = this;
    if($stateParams.view) {
      this.selectedView = $stateParams.view;
      this.view = _.find(this.views, function (view) {
        return that.selectedView === view.id;
      });
    } else {
      ViewService.getDefaultOrFirstOne().then( (response) => {
        that.selectedView = response.id;
        that.view = response;
      })
    }

    $scope.$on('$stateChangeStart', function() {
      this.hideApis = true;
    });
  }

  goToApi(api) {
    this.$state.go('portal.api.detail', {apiId: api.id});
  }

  changeView(event, view) {
    event.stopPropagation();
    this.$state.go('portal.apis.list', {view: view});
  }

  goToRating(event, api) {
    event.stopPropagation();
    this.$state.go('portal.api.rating', {apiId: api.id});
  }
}

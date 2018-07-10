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
import ApiService from "../../../services/api.service";

export default class PortalViewController {
  public view: any;
  public apis: any[];
  public highlightApi: any;
  public ratingEnabled: boolean;

  constructor (private resolvedView,
               private resolvedApis,
               private $state,
               private ApiService: ApiService) {
    'ngInject';
    this.view = resolvedView.data;
    this.apis = resolvedApis.data;
    if (this.apis && this.apis.length > 0) {
      if (this.view.highlightApi) {
        this.highlightApi = _.find(this.apis, api => api.id === this.view.highlightApi);
        if (this.highlightApi) {
          _.remove(this.apis, api => api.id === this.view.highlightApi);
        } else {
          this.highlightApi = this.apis[0];
          this.apis.shift();
        }
      } else {
        this.highlightApi = this.apis[0];
        this.apis.shift();
      }
      this.ratingEnabled = this.ApiService.isRatingEnabled();
    }
  }

  goToApi(event, api) {
    event.stopPropagation();
    this.$state.go('portal.api.detail', {apiId: api.id});
  }

  goToApiRating(event, api) {
    event.stopPropagation();
    this.$state.go('portal.api.rating', {apiId: api.id});
  }

  goToApiDocumentation(event, api) {
    event.stopPropagation();
    this.$state.go('portal.api.pages', {apiId: api.id});
  }
}

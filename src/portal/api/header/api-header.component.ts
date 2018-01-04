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
import ApiService from "../../../services/api.service";

const ApiHeaderComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    apiRatingSummary: '<'
  },
  template: require('./api-header.html'),
  controller: function(Constants, ApiService: ApiService, $stateParams, $rootScope) {
    'ngInject';
    this.ratingEnabled = ApiService.isRatingEnabled();

    this.getEndpoint = function () {
      return Constants.portal.entrypoint + this.api.context_path;
    };

    $rootScope.$on('onRatingSave', () => {
      ApiService.getApiRatingSummaryByApi($stateParams.apiId).then((response) => {
        this.apiRatingSummary = response.data;
      });
    });
  }
};

export default ApiHeaderComponent;

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
import TicketService from "../../../services/ticket.service";

const ApiHeaderComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    apiRatingSummary: '<',
    apiPortalHeaders: '<',
    entrypoints: '<'
  },
  template: require('./api-header.html'),
  controller: function (
    Constants,
    ApiService: ApiService,
    $state,
    $stateParams,
    $rootScope,
    TicketService: TicketService,
    $window,
    $timeout) {
    'ngInject';
    this.ratingEnabled = ApiService.isRatingEnabled();
    this.supportEnabled = TicketService.isSupportEnabled();
    this.Constants = Constants;

    $rootScope.$on('onRatingSave', () => {
      ApiService.getApiRatingSummaryByApi($stateParams.apiId).then((response) => {
        this.apiRatingSummary = response.data;
      });
    });

    this.$onInit = () => {
      $timeout(function () {
        const apiNavbar = document.getElementById("api-navbar");
        const headerDetail = document.getElementById("header-detail");
        const headerMetadata = document.getElementById("header-metadata");
        const header = document.getElementById("header");
        const headerTitle = document.getElementById("header-title");
        const content = document.getElementById("main-content-content");

        const pageHeader = document.getElementById("pageHeader");

        $window.onscroll = () => {
          if ($window.pageYOffset > 0) {
            content.classList.add("header-fixed");
            pageHeader.classList.add("sticky");
          } else {
            content.classList.remove("header-fixed");
            pageHeader.classList.remove("sticky");
          }
        };
      }, 0);
    };

    this.getEntrypointsByTags = () => {
      return ApiService.getTagEntrypoints(this.api, this.entrypoints);
    }
  }
};

export default ApiHeaderComponent;

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
    apiPortalHeaders: '<'
  },
  template: require('./api-header.html'),
  controller: function(
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
      $timeout(function() {
        const apiNavbar = document.getElementById("api-navbar");
        const headerDetail = document.getElementById("header-detail");
        const headerMetadata = document.getElementById("header-metadata");
        const headerTitle = document.getElementById("header");
        const content = document.getElementById("main-content-content")
        $window.onscroll = () => {
          if ($window.pageYOffset > 0) {
            headerDetail.classList.add("wipeoff");
            headerMetadata.classList.add("wipeoff");
            headerTitle.classList.add("sticky");
            apiNavbar.classList.add("sticky");
            content.classList.add("header-fixed");
          } else {
            headerDetail.classList.remove("wipeoff");
            headerMetadata.classList.remove("wipeoff");
            headerTitle.classList.remove("sticky");
            apiNavbar.classList.remove("sticky");
            content.classList.remove("header-fixed")
          }
        };
      }, 0);
    };
  }
};

export default ApiHeaderComponent;

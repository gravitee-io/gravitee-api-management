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
import _ = require('lodash');
import ApiService from "../../../services/api.service";
import TicketService from "../../../services/ticket.service";
import * as _ from "lodash";

const ApiHeaderComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    apiRatingSummary: '<',
    apiPortalHeaders: '<'
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
      // override api entrypoint
      if (Constants.portal.entrypoint && this.api.entrypoints.length <= 1) {
        let ctxtpath = "";
        if (this.api.proxy.virtual_hosts.length > 0) {
          ctxtpath = this.api.proxy.virtual_hosts[0].path;
        }
        this.api.entrypoints[0].target = Constants.portal.entrypoint + ctxtpath;
      }

      // resolve entry points
      this.resolvedEntrypoints = [];
      if (this.Constants.portal.apis.apiHeaderShowTags.enabled) {
        this.api.entrypoints.forEach(resolvedEntryPoint => {
          if (resolvedEntryPoint.tags) {
            resolvedEntryPoint.tags.forEach(tag => {
              this.resolvedEntrypoints.push({tags: [tag], value: resolvedEntryPoint.target});
            });
          }
        });
      } else {
        _.uniq(_.map(this.api.entrypoints, "target")).forEach(apiEntryPoint => this.resolvedEntrypoints.push({ tags: [""], value: apiEntryPoint}));
      }
      // set default entry point if none has been set
      if (this.resolvedEntrypoints.length === 0) {
        this.resolvedEntrypoints.push({ tags: [""], value: this.api.entrypoints[0].target});
      }
      // manage tags without entry point
      if (this.Constants.portal.apis.apiHeaderShowTags.enabled) {
        let resolvedTags = _.difference(this.api.tags, _.map(this.resolvedEntrypoints, resolvedEntrypoint => resolvedEntrypoint.tags[0]));
        if (resolvedTags.length > 0) {
          resolvedTags.forEach(resolvedTag => this.resolvedEntrypoints.push({tags: [resolvedTag], value: ""}));
        }
      }

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

    this.uniqueEntrypoints = () => {
      return _.uniq(_.map(this.api.entrypoints, 'target'));
    };
  }
};

export default ApiHeaderComponent;

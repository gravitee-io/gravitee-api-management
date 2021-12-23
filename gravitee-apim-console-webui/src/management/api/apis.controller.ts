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
import { StateParams, StateService, TransitionService } from '@uirouter/core';
import * as _ from 'lodash';

import { ApiService } from '../../services/api.service';
import UserService from '../../services/user.service';

interface IApisScope extends ng.IScope {
  apisLoading: boolean;
  formApi: any;
  searchResult: boolean;
}

export class ApisController {
  private query = '';
  private order: string = undefined;
  private currentOrder: string = undefined;
  private apis: any;
  private graviteeUIVersion: string;
  private isAPIsHome: boolean;
  private createMode: boolean;
  private syncStatus: any[];
  private qualityScores: any[];
  private NotificationService: any;
  private selectedApis: any[];
  private isQualityDisplayed: boolean;
  private timer: any;
  private canceler: any;
  private currentApisResponse: any;

  constructor(
    private ApiService: ApiService,
    private $mdDialog: ng.material.IDialogService,
    private $scope: IApisScope,
    private $state: StateService,
    private Constants,
    private Build,
    private resolvedApis,
    private UserService: UserService,
    private graviteeUser,
    private $filter: ng.IFilterService,
    private $transitions: TransitionService,
    private $stateParams: StateParams,
    private $timeout: ng.ITimeoutService,
    private $q: ng.IQService,
  ) {
    'ngInject';

    this.$q = $q;
    this.graviteeUser = graviteeUser;
    this.graviteeUIVersion = Build.version;
    this.query = $state.params.q;
    this.currentApisResponse = resolvedApis.data;
    this.order = this.currentOrder;
    this.apis = this.currentApisResponse.data;

    if (!this.currentApisResponse.data.length) {
      // if no APIs, maybe the auth token has been expired
      UserService.current();
    }

    this.isAPIsHome = this.$state.includes('apis');

    this.selectedApis = [];
    this.syncStatus = [];
    this.qualityScores = [];
    this.isQualityDisplayed = Constants.env.settings.apiQualityMetrics && Constants.env.settings.apiQualityMetrics.enabled;

    $scope.$watch('$ctrl.query', (query: string, previousQuery: string) => {
      $timeout.cancel(this.timer);
      this.timer = $timeout(() => {
        if (query !== undefined && query !== previousQuery) {
          this.searchWithQuery(query);
        }
      }, 300);
    });
    this.canceler = $q.defer();

    this.loadMore();
  }

  searchWithQuery(query: string) {
    this.canceler.resolve();
    this.canceler = this.$q.defer();

    const promOpts = { timeout: this.canceler.promise };
    this.$state.transitionTo(this.$state.current, { q: this.query }, { notify: false });

    this.ApiService.searchApis(query, 1, this.currentOrder, promOpts)
      .then((response) => {
        this.currentApisResponse = response.data;
        this.apis = this.currentApisResponse.data;
        this.loadMore();
      })
      .catch((err) => {
        if (err && err.interceptorFuture) {
          // avoid a duplicated notification with the same error
          err.interceptorFuture.cancel();
        }
      });
  }

  sort(order: string, field: string) {
    if (order === this.currentOrder || (order != null && !order.includes(field))) {
      return;
    }
    if (order) {
      this.currentOrder = order;
    }
    this.gotToPage(1);
  }

  scroll() {
    return this.gotToPage(this.currentApisResponse.page.current + 1);
  }

  gotToPage(requestedPage: number) {
    if (this.$scope.apisLoading) {
      return;
    }

    this.$scope.apisLoading = true;
    this.$scope.searchResult = true;

    if (requestedPage > this.currentApisResponse.page.total_pages) {
      // The last page has been reached, no need to search for more;
      this.$scope.apisLoading = false;
      return;
    }

    this.ApiService.searchApis(this.query || '', requestedPage, this.currentOrder).then((response) => {
      this.currentApisResponse = response.data;
      if (requestedPage > 1) {
        this.apis = this.apis.concat(this.currentApisResponse.data);
      } else {
        this.apis = this.currentApisResponse.data;
      }
      this.loadMore();

      this.$scope.apisLoading = false;
    });
  }

  isSearchResult() {
    return this.$state.params.q !== undefined || this.$scope.searchResult;
  }

  update(api) {
    this.ApiService.update(api).then(() => {
      this.$scope.formApi.$setPristine();
      this.NotificationService.show('Api updated with success');
    });
  }

  getVisibilityIcon(api) {
    switch (api.visibility) {
      case 'PUBLIC':
        return 'public';
      case 'PRIVATE':
        return 'lock';
    }
  }

  getVisibility(api) {
    switch (api.visibility) {
      case 'PUBLIC':
        return 'Public';
      case 'PRIVATE':
        return 'Private';
    }
  }

  showImportDialog() {
    this.$mdDialog
      .show({
        controller: 'DialogApiImportController',
        controllerAs: 'dialogApiImportCtrl',
        template: require('./portal/general/dialog/apiImport.dialog.html'),
        locals: {
          apiId: '',
        },
        clickOutsideToClose: true,
      })
      .then((response) => {
        if (response) {
          this.$state.go('apis.admin.general', { apiId: response.data.id }, { reload: true });
        }
      });
  }

  getSubMessage() {
    if (!this.graviteeUser.id) {
      return 'Login to get access to more APIs';
    } else if (this.UserService.isUserHasPermissions(['environment-api-c'])) {
      return 'Start creating an API';
    } else {
      return '';
    }
  }

  loadMore = () => {
    if (this.currentApisResponse.data) {
      _.forEach(this.currentApisResponse.data, (api: any) => {
        if (_.isUndefined(this.syncStatus[api.id])) {
          this.ApiService.isAPISynchronized(api.id).then((sync) => {
            this.syncStatus[api.id] = sync.data.is_synchronized;
          });
        }
        if (this.isQualityDisplayed && _.isUndefined(this.qualityScores[api.id])) {
          this.ApiService.getQualityMetrics(api.id).then((response) => {
            this.qualityScores[api.id] = _.floor(response.data.score * 100);
          });
        }
      });
    }
  };

  getQualityMetricCssClass(score) {
    return this.ApiService.getQualityMetricCssClass(score);
  }

  getWorkflowStateLabel(api) {
    if (api.lifecycle_state === 'DEPRECATED') {
      return 'DEPRECATED';
    }
    switch (api.workflow_state) {
      case 'DRAFT':
        return 'DRAFT';
      case 'IN_REVIEW':
        return 'IN REVIEW';
      case 'REQUEST_FOR_CHANGES':
        return 'NEED CHANGES';
      case 'REVIEW_OK':
        return '';
    }
  }

  getWorkflowStateColor(api) {
    if (api.lifecycle_state === 'DEPRECATED') {
      return '#d73a49';
    }
    switch (api.workflow_state) {
      case 'DRAFT':
        return '#54a3ff';
      case 'IN_REVIEW':
      case 'REQUEST_FOR_CHANGES':
        return '#d73a49';
    }
  }

  getEntrypoints(api) {
    return _.uniq(_.map(api.virtual_hosts, 'path')).join(' - ');
  }
}

export default ApisController;

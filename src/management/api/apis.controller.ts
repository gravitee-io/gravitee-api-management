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

import UserService from '../../services/user.service';
import {StateParams, StateService, TransitionService} from '@uirouter/core';
import ApiService from '../../services/api.service';

interface IApisScope extends ng.IScope {
  apisLoading: boolean;
  formApi: any;
  searchResult: boolean;
}
export class ApisController {

  private query: string = '';
  private apisProvider: any;
  private apis: any;
  private graviteeUIVersion: string;
  private apisScrollAreaHeight: number;
  private isAPIsHome: boolean;
  private createMode: boolean;
  private devMode: boolean;
  private syncStatus: any[];
  private qualityScores: any[];
  private NotificationService: any;
  private portalTitle: string;
  private selectedApis: any[];
  private isQualityDisplayed: boolean;
  private timer: any;
  private canceler: any;

  constructor(private ApiService: ApiService,
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
              private $q: ng.IQService) {
    'ngInject';

    this.$q = $q;
    this.graviteeUser = graviteeUser;
    this.graviteeUIVersion = Build.version;
    this.portalTitle = Constants.portal.title;
    this.query = $state.params.q;
    this.apisProvider = _.filter(resolvedApis.data, 'manageable');
    if (!this.apisProvider.length) {
      // if no APIs, maybe the auth token has been expired
      UserService.current();
    }

    this.apisScrollAreaHeight = this.$state.current.name === 'apis.list' ? 195 : 90;
    this.isAPIsHome = this.$state.includes('apis');

    this.createMode = !Constants.portal.devMode.enabled;
    this.selectedApis = [];
    this.syncStatus = [];
    this.qualityScores = [];
    this.isQualityDisplayed = Constants.apiQualityMetrics && Constants.apiQualityMetrics.enabled;

    $scope.$watch('$ctrl.query', (query: string, previousQuery: string) => {
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

    this.$scope.searchResult = true;
    this.$scope.apisLoading = true;
    this.canceler.resolve();
    this.canceler = this.$q.defer();

    let promise;
    let promOpts = {timeout: this.canceler.promise};
    this.$state.transitionTo(
      this.$state.current,
      {q: this.query},
      {notify: false});

    if (this.query) {
      promise = this.ApiService.searchApis(this.query, promOpts);
    } else {
      promise = this.ApiService.list(null, false, promOpts);
    }

    promise.then( (response) => {
      this.apisProvider = _.filter(response.data, 'manageable');
      this.loadMore(this.query.order, false);
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
      case 'public':
        return 'public';
      case 'private':
        return 'lock';
    }
  }

  getVisibility(api) {
    switch (api.visibility) {
      case 'public':
        return 'Public';
      case 'private':
        return 'Private';
    }
  }

  showImportDialog() {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogApiImportController',
      controllerAs: 'dialogApiImportCtrl',
      template: require('./portal/general/dialog/apiImport.dialog.html'),
      locals: {
        apiId: ''
      },
      clickOutsideToClose: true
    }).then(function (response) {
      if (response) {
        that.$state.go('apis.admin.general', {apiId: response.data.id}, {reload: true});
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

  loadMore = (order, showNext) => {
    // check if data must be refreshed or not when sorting or searching (when user is typing text)
    const doNotLoad = showNext && (this.apisProvider && this.apisProvider.length) === (this.apis && this.apis.length) &&
      _.difference(_.map(this.apisProvider, 'id'), _.map(this.apis, 'id')).length === 0;
    if (!doNotLoad && this.apisProvider) {
      let apisProvider = _.clone(this.apisProvider);
      apisProvider = _.sortBy(apisProvider, _.replace(order, '-', ''));
      if (_.startsWith(order, '-')) {
        apisProvider.reverse();
      }
      let apisLength = this.apis ? this.apis.length : 0;
      this.apis = _.take(apisProvider, 20 + apisLength);
      _.forEach(this.apis, (api: any) => {
        if (_.isUndefined(this.syncStatus[api.id])) {
          this.ApiService.isAPISynchronized(api.id)
            .then((sync) => {
              this.syncStatus[api.id] = sync.data.is_synchronized;
            });
        }
        if (this.isQualityDisplayed && _.isUndefined(this.qualityScores[api.id])) {
          this.ApiService.getQualityMetrics(api.id)
            .then((response) => {
              this.qualityScores[api.id] = _.floor(response.data.score * 100);
            });
        }
      });
    }
  }

  getQualityMetricCssClass(score) {
    return this.ApiService.getQualityMetricCssClass(score);
  }

  getWorkflowStateLabel(api) {
    if (api.lifecycle_state === 'deprecated') {
      return 'DEPRECATED';
    }
    switch (api.workflow_state) {
      case 'draft':
        return 'DRAFT';
      case 'in_review':
        return 'IN REVIEW';
      case 'request_for_changes':
        return 'NEED CHANGES';
      case 'review_ok':
        return '';
    }
  }

  getWorkflowStateColor(api) {
    if (api.lifecycle_state === 'deprecated') {
      return '#d73a49';
    }
    switch (api.workflow_state) {
      case 'draft':
        return '#54a3ff';
      case 'in_review':
      case 'request_for_changes':
        return '#d73a49';
    }
  }

  getEntrypoints(api) {
    return _.uniq(_.map(api.virtual_hosts, 'path')).join(' - ');
  }
}

export default ApisController;

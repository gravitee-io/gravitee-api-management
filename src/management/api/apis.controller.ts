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

export class ApisController {

  private apisProvider: any;
  private apis: any;
  private graviteeUIVersion: string;
  private apisScrollAreaHeight: number;
  private isAPIsHome: boolean;
  private createMode: boolean;
  private devMode: boolean;
  private syncStatus: any;
  private NotificationService: any;
  private portalTitle: string;
  private selectedApis: any[];

  constructor(private ApiService,
              private $mdDialog,
              private $scope,
              private $state: ng.ui.IStateService,
              private Constants,
              private Build,
              private resolvedApis,
              private UserService: UserService,
              private graviteeUser,
              private $filter) {
    'ngInject';

    this.graviteeUser = graviteeUser;
    this.graviteeUIVersion = Build.version;
    this.portalTitle = Constants.portal.title;
    this.apisProvider = _.filter(resolvedApis.data, 'manageable');
    if (!this.apisProvider.length) {
      // if no APIs, maybe the auth token has been expired
      UserService.current(true);
    }

    this.apisScrollAreaHeight = this.$state.current.name === 'apis.list' ? 195 : 90;
    this.isAPIsHome = this.$state.includes('apis');

    this.createMode = !Constants.portal.devMode.enabled; // && Object.keys($rootScope.graviteeUser).length > 0;
    this.selectedApis = [];
    this.syncStatus = [];

    $scope.$on('$stateChangeStart', function () {
      $scope.hideApis = true;
    });
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
      case 'restricted':
        return 'vpn_lock';
      case 'private':
        return 'lock';
    }
  }

  getVisibility(api) {
    switch (api.visibility) {
      case 'public':
        return 'Public';
      case 'restricted':
        return 'Restricted';
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
      apiId: '',
      clickOutsideToClose: true
    }).then(function (response) {
      if (response) {
        that.$state.go('apis.admin.general', {apiId: response.data.id}, {reload: true});
      }
    });
  }

  getSubMessage() {
    if (!this.graviteeUser.username) {
      return 'Login to get access to more APIs';
    } else if (this.UserService.isUserHasPermissions(['management-api-c'])) {
      return 'Start creating an API';
    } else {
      return '';
    }
  }

  loadMore = function (order, searchAPIs, showNext) {
    const doNotLoad = showNext && (this.apisProvider && this.apisProvider.length) === (this.apis && this.apis.length);
    if (!doNotLoad && this.apisProvider && this.apisProvider.length) {
      let apisProvider = _.clone(this.apisProvider);
      if (searchAPIs) {
        apisProvider = this.$filter('filter')(apisProvider, searchAPIs);
      }
      apisProvider = _.sortBy(apisProvider, _.replace(order, '-', ''));
      if (_.startsWith(order, '-')) {
        apisProvider.reverse();
      }
      let apisLength = this.apis? this.apis.length:0;
      this.apis = _.take(apisProvider, 20 + apisLength);
      _.forEach(this.apis, (api: any) => {
        if (_.isUndefined(this.syncStatus[api.id])) {
          this.ApiService.isAPISynchronized(api.id)
            .then((sync) => {
              this.syncStatus[api.id] = sync.data.is_synchronized;
            });
        }
      });
    }
  }
}

export default ApisController;

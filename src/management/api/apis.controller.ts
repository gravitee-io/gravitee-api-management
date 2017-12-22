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

  constructor(
    private ApiService,
    private $mdDialog,
    private $scope,
    private $state: ng.ui.IStateService,
    private Constants,
    private Build,
    private resolvedApis,
    private UserService: UserService,
    private graviteeUser,
    private $q: ng.IQService,
  ) {
    'ngInject';

    this.graviteeUser = graviteeUser;
    this.graviteeUIVersion = Build.version;
    this.portalTitle = Constants.portalTitle;
    this.apis = resolvedApis.data;

    this.apisScrollAreaHeight = this.$state.current.name === 'apis.list' ? 195 : 90;
    this.isAPIsHome = this.$state.includes('apis');

    this.createMode = !Constants.devMode; // && Object.keys($rootScope.graviteeUser).length > 0;

    this.reloadSyncState();

    this.selectedApis = [];

    $scope.$on('$stateChangeStart', function() {
      $scope.hideApis = true;
    });
  }

  reloadSyncState() {
    let promises = _.map(this.apis, (api: any) => {
        return this.ApiService.isAPISynchronized(api.id)
          .then((sync) => { return sync; });
    });

    this.$q.all( _.filter( promises, ( p ) => { return p !== undefined; } ) )
      .then((syncList) => {
        this.syncStatus = _.fromPairs(_.map(syncList, (sync: any) => {
          return [sync.data.api_id, sync.data.is_synchronized];
        }));
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
      template: require('./general/dialog/apiImport.dialog.html'),
      apiId: '',
      clickOutsideToClose: true
    }).then(function (response) {
      if (response) {
        that.$state.go('apis.admin.general', {apiId: response.data.id}, {reload: true});
      }
    });
  }

  getSubMessage() {
    if (! this.graviteeUser.username) {
      return 'Login to get access to more APIs';
    } else if (this.UserService.isUserHasPermissions(['management-api-c'])) {
      return 'Start creating an API';
    } else {
      return '';
    }
  }
}

export default ApisController;

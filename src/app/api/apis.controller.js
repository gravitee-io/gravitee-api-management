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
class ApisController {
  constructor ($window, ApiService, $mdDialog, $scope, $state, $rootScope, resolvedApis) {
    'ngInject';
    this.$window = $window;
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.$scope = $scope;
    this.$state = $state;
		this.$rootScope = $rootScope;
		this.graviteeUIVersion = Constants.version;
		this.apis = resolvedApis.data;

    this.tableMode = this.$state.current.name.endsWith('table')? true : false;
    this.apisScrollAreaHeight = this.$state.current.name === 'home' ? 195 : 90;
    this.isAPIsHome = this.$state.current.name.startsWith('apis')? true : false;
    this.init();
  }

  init() {
    var self = this;
    this.$scope.$on("showApiModal", function() {
      self.showApiModal();
    });
  }

  list() {
    this.apis = [];
    this.ApiService.list().then(response => {
      this.apis = response.data;
    });
  }

  showApiModal() {
    var _that = this;
    this.$mdDialog.show({
      controller: 'DialogApiController',
      controllerAs: 'dialogApiCtrl',
      templateUrl: 'app/api/dialog/api.dialog.html'
    }).then(function (api) {
      if (api) {
        _that.$window.location.href = '#/apis/' + api.data.id + '/settings/general';
      }
    });
  }

  update(api) {
    this.ApiService.update(api).then(() => {
      this.$scope.formApi.$setPristine();
      this.NotificationService.show('Api updated with success');
    });
  }

  changeMode(tableMode) {
    this.tableMode = tableMode;
    this.$state.go(tableMode? 'apis.list.table' : 'apis.list.thumb');
  }

  backToPreviousState() {
    if (!this.$scope.previousState) {
      this.$scope.previousState = 'apis.list.thumb';
    }
    this.$state.go(this.$scope.previousState, {}, {reload: true});
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

  isOwner(api) {
    return api.permission && (api.permission === 'owner' || api.permission === 'primary_owner');
  }

	login() {
		this.$rootScope.$broadcast("authenticationRequired");
	}

	createInitAPI() {
		if (!this.$rootScope.authenticated) {
			this.$rootScope.$broadcast("authenticationRequired");
		} else if (this.$state.includes("apis.list")) {
			this.showApiModal();
		} else {
		  this.$scope.$broadcast("showApiModal");
			this.$state.go('apis.list.thumb');
		}
	}

  showImportDialog() {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogApiDefinitionController',
      controllerAs: 'dialogApiDefinitionCtrl',
      templateUrl: 'app/api/admin/general/dialog/apiDefinition.dialog.html',
      apiId: ''
    }).then(function (response) {
      if (response) {
        that.list();
      }
    });
  }
}

export default ApisController;

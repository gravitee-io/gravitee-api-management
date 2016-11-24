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
  constructor($window, ApiService, $mdDialog, $scope, $state, $rootScope, Constants, resolvedApis, ViewService, $q) {
    'ngInject';
    this.$q = $q;
    this.$window = $window;
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.$scope = $scope;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.graviteeUIVersion = Constants.version;
    this.resolvedApis = resolvedApis;

    this.apisScrollAreaHeight = this.$state.current.name === 'apis.list' ? 195 : 90;
    this.isAPIsHome = this.$state.current.name.startsWith('apis') ? true : false;
    this.goToView(this.$state.params.view || 'all');
    this.createMode = !$rootScope.devMode && Object.keys($rootScope.graviteeUser).length > 0;

    var that = this;
    ViewService.list().then(function (response) {
      that.views = response.data;
      that.views.unshift({id: 'all', name: 'All APIs'});

      if (that.views.length && that.$state.params.view) {
        that.selectedIndex = _.findIndex(that.views, function (v) {
          return v.id === that.$state.params.view;
        });
      } else {
        that.selectedIndex = 0;
      }
    });
  }

  goToView(view) {
    this.$state.go(this.$state.current, {view: view}, {notify: false});
    this.loadApis(view);
  }

  loadApis(viewId) {
    var that = this;
    this.$q.resolve(viewId)
      .then( (viewId) => {
        if (viewId && viewId !== 'all') {
          return that.ApiService
            .list(viewId)
            .then( (response) => {
              return response;
            });
        } else {
          return that.resolvedApis;
        }
      })
      .then( response => {
        that.apis = response.data;
        return that.apis;
      })
      .then( (apis) => {
        const promises = _.map(apis, (api) => {
          if (that.isOwner(api) && !that.devMode) {
            return that.ApiService.isAPISynchronized(api.id)
              .then((sync) => {
                return sync;
              });
          }
        });
        return this.$q
          .all( _.filter( promises, ( p ) => { return p!== undefined; } ) )
          .then((syncList) => {
              that.syncStatus = _.fromPairs(_.map(syncList, (sync) => {
                return [sync.data.api_id, sync.data.is_synchronized];
              }));
          });
      });
  }

  list() {
    this.apis = [];
    this.ApiService.list().then(response => {
      this.apis = response.data;
    });
  }

  update(api) {
    this.ApiService.update(api).then(() => {
      this.$scope.formApi.$setPristine();
      this.NotificationService.show('Api updated with success');
    });
  }

  backToPreviousState() {
    if (!this.$scope.previousState) {
      this.$scope.previousState = 'apis.list';
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

  showImportSwaggerDialog() {
    var _that = this;
    this.$mdDialog.show({
      controller: 'DialogApiSwaggerImportController',
      controllerAs: 'dialogApiSwaggerImportCtrl',
      templateUrl: 'app/api/admin/creation/swagger/importSwagger.dialog.html',
      apiId: ''
    }).then(function (api) {
      if (api) {
        _that.$state.go('apis.new', {api: api});
      }
    });
  }
}

export default ApisController;

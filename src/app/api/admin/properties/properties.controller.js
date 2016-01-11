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
class ApiPropertiesController {
  constructor (ApiService, resolvedApi, $state, $mdDialog, NotificationService, $scope, $rootScope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.api = resolvedApi.data;
  }

  deleteProperty(key) {
    delete this.api.properties[key];
  }

  showPropertyModal() {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddPropertyController',
      controllerAs: 'dialogAddPropertyCtrl',
      templateUrl: 'app/api/admin/properties/add-property.dialog.html',
      clickOutsideToClose: true
    }).then(function (property) {
      var key = Object.keys(property)[0];

      if (_this.api.properties === undefined) {
        _this.api.properties = {};
      }

      _this.api.properties[key] = property[key];
    });
  }

  update(api) {
    var _this = this;
    this.ApiService.update(api).then((updatedApi) => {
      _this.api = updatedApi.data;
      _this.$rootScope.$broadcast('apiChangeSuccess');
      _this.NotificationService.show('API \'' + _this.$scope.$parent.apiCtrl.api.name + '\' saved');
    });
  }
}

export default ApiPropertiesController;

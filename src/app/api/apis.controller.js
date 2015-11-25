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
/* global document:false */
class ApisController {
  constructor (ApiService, $mdDialog, $scope, $state) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.$scope = $scope;
    this.$state = $state;

    this.tableMode = $state.current.name.endsWith('table')? true : false;
    this.list();
  }

  list() {
    this.ApiService.list().then(response => {
      this.apis = response.data;
    });
  }

  showApiModal(api) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogApiController',
      templateUrl: 'app/api/dialog/api.dialog.html',
      parent: angular.element(document.body),
      api: api
    }).then(function (api) {
      if (api) {
        that.list();
      }
    });
  }

  update(api) {
    this.ApiService.update(api).then(() => {
      this.$scope.formApi.$setPristine();
      this.NotificationService.show('Api updated with success');
    });
  }

  bgColorByIndex(index) {
    switch (index % 6) {
      case 0 :
        return '#f39c12';
      case 1 :
        return '#29b6f6';
      case 2 :
        return '#26c6da';
      case 3 :
        return '#26a69a';
      case 4 :
        return '#259b24';
      case 5 :
        return '#26a69a';
      default :
        return 'black';
    }
  };

  onModeChange() {
    this.$state.go(this.tableMode? 'apis.list.table' : 'apis.list.thumb');
  };
}

export default ApisController;

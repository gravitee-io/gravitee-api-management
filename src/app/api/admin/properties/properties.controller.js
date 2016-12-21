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
  constructor (ApiService, resolvedApi, $mdSidenav, $mdEditDialog, $state, $mdDialog, NotificationService, $scope, $rootScope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.api = resolvedApi.data;
    this.$mdSidenav = $mdSidenav;
    this.$mdEditDialog = $mdEditDialog;

    this.editor = undefined;

    this.joltSpecificationOptions = {
      placeholder: "Edit your JOLT specification here.",
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      mode: "javascript",
      controller: this
    };

    this.dynamicPropertyService = this.api.services && this.api.services['dynamic-property'];

    this.timeUnits = [ 'SECONDS', 'MINUTES', 'HOURS' ];
    this.dynamicPropertyProviders = [
      {
        id: 'HTTP',
        name: 'Custom (HTTP)'
      }
    ];

    if (this.dynamicPropertyService !== undefined) {
      this.$scope.dynamicPropertyEnabled = this.dynamicPropertyService.enabled;
    } else {
      this.$scope.dynamicPropertyEnabled = false;
    }
  }

  hasPropertiesDefined() {
    return this.api.properties && Object.keys(this.api.properties).length > 0;
  }

  deleteProperty(key) {
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      templateUrl: 'app/components/dialog/confirmWarning.dialog.html',
      clickOutsideToClose: true,
      title: 'Are you sure you want to remove property [' + key + '] ?',
      msg: "",
      confirmButton: "Remove"
    }).then(function (response) {
      if (response) {
        _.remove(that.api.properties, function(property) {
          return property.key === key;
        });

        that.update(that.api);
      }
    });
  }

  showPropertyModal() {
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogAddPropertyController',
      controllerAs: 'dialogAddPropertyCtrl',
      templateUrl: 'app/api/admin/properties/add-property.dialog.html',
      clickOutsideToClose: true
    }).then(function (property) {
      if (that.api.properties === undefined) {
        that.api.properties = [];
      }

      if (property) {
        that.api.properties.push(property);
        that.update(that.api);
      }
    });
  }

  update() {
    var _this = this;
    this.api.services['dynamic-property'] = this.dynamicPropertyService;
    this.ApiService.update(this.api).then((updatedApi) => {
      _this.api = updatedApi.data;
      _this.$rootScope.$broadcast('apiChangeSuccess');
      _this.NotificationService.show('API \'' + _this.$scope.$parent.apiCtrl.api.name + '\' saved');
    });
  }

  editValue(event, property) {
    event.stopPropagation();
    var _that = this;
    this.$mdEditDialog.small({
      modelValue: property.value,
      placeholder: 'Set property value',
      save: function (input) {
        property.value = input.$modelValue;
        property.dynamic = false;
        _that.update(_that.api);
      },
      targetEvent: event,
      validators: {
        'md-maxlength': 160
      }
    });
  }

  switchEnabled() {
    if (this.dynamicPropertyService === undefined) {
      this.dynamicPropertyService = {};
    }
    this.dynamicPropertyService.enabled = this.$scope.dynamicPropertyEnabled;
    this.update();
  }

  open() {
    let that = this;
    this.$mdSidenav('dynamic-properties-config')
      .open()
      .then(function() {
        that.editor.setSize("100%", "100%");
      });
  }

  close() {
    console.log(this.editor);
    this.$mdSidenav('dynamic-properties-config')
      .close();
  }

  codemirrorLoaded(_editor) {
    this.controller.editor = _editor;

    // Editor part
    var _doc = this.controller.editor.getDoc();

    // Options
    _doc.markClean();
  }

  showExpectedProviderOutput() {
    this.$mdDialog.show({
      controller: 'DialogDynamicProviderHttpController',
      controllerAs: 'ctrl',
      templateUrl: 'app/api/admin/properties/dynamic-provider-http.dialog.html',
      parent: angular.element(document.body),
      clickOutsideToClose:true,
      onComplete:function(scope) {
        scope.reload();
      }
    });
  }
}

export default ApiPropertiesController;

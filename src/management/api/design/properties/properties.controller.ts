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
import angular = require("angular");

import ApiService from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';

interface IApiPropertiesScope extends ng.IScope {
  dynamicPropertyEnabled: boolean;
  apiCtrl: any;
  $parent: any;
}

class ApiPropertiesController {

  private api: any;
  private dynamicPropertyService: any;
  private controller: any;
  private editor: any;
  private joltSpecificationOptions: any;
  private dynamicPropertyProviders: {id: string; name: string}[];
  private timeUnits: string[];

  constructor (
    private ApiService: ApiService,
    private resolvedApi,
    private $mdSidenav: angular.material.ISidenavService,
    private $mdEditDialog,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
    private $scope: IApiPropertiesScope,
    private $rootScope
  ) {
    'ngInject';
    this.dynamicPropertyProviders = [
      {
        id: 'HTTP',
        name: 'Custom (HTTP)'
      }
    ];
    this.timeUnits = [ 'SECONDS', 'MINUTES', 'HOURS' ];
    this.api = this.$scope.$parent.apiCtrl.api;
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

    if (this.dynamicPropertyService !== undefined) {
      this.$scope.dynamicPropertyEnabled = this.dynamicPropertyService.enabled;
    } else {
      this.$scope.dynamicPropertyEnabled = false;
    }

    this.$scope.$on('apiChangeSuccess', (event, args) => {
      this.api = args.api;
    });
  }

  hasPropertiesDefined() {
    return this.api.properties && Object.keys(this.api.properties).length > 0;
  }

  deleteProperty(key) {
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to remove property [' + key + '] ?',
        msg: '',
        confirmButton: 'Remove'
      }
    }).then(function (response) {
      if (response) {
        _.remove(that.api.properties, (property: any) => {
          return property.key === key;
        });

        that.update();
      }
    });
  }

  showPropertyModal() {
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogAddPropertyController',
      controllerAs: 'dialogAddPropertyCtrl',
      template: require('./add-property.dialog.html'),
      clickOutsideToClose: true
    }).then(function (property) {
      if (that.api.properties === undefined) {
        that.api.properties = [];
      }

      if (property) {
        that.api.properties.push(property);
        that.update();
      }
    });
  }

  update() {
    var _that = this;
    this.api.services['dynamic-property'] = this.dynamicPropertyService;
    this.ApiService.update(this.api).then((updatedApi) => {
      _that.api = updatedApi.data;
      _that.api.etag = updatedApi.headers('etag');
      _that.$rootScope.$broadcast('apiChangeSuccess', {api: _that.api});
      _that.NotificationService.show('API \'' + (_that.$scope as any).$parent.apiCtrl.api.name + '\' saved');
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
        _that.update();
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
        if (that.editor) {
          that.editor.setSize("100%", "100%");
        }
      });
  }

  close() {
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
      template: require('./dynamic-provider-http.dialog.html'),
      parent: angular.element(document.body),
      clickOutsideToClose: true
    });
  }
}

export default ApiPropertiesController;

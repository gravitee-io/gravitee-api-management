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

import angular from 'angular';
import * as _ from 'lodash';

import { ApiService } from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';
import { IfMatchEtagInterceptor } from '../../../../shared/interceptors/if-match-etag.interceptor';

interface IApiPropertiesScope extends ng.IScope {
  formDynamicProperties: any;
  dynamicPropertyEnabled: boolean;
}

class ApiV1PropertiesControllerAjs {
  resolvedApi: any;

  private api: any;
  private dynamicPropertyService: any;
  private controller: any;
  private editor: any;
  private joltSpecificationOptions: any;
  private requestBodyOptions: any;
  private dynamicPropertyProviders: { id: string; name: string }[];
  private selectedProperties: any = {};
  private selectAll: boolean;
  private _initialDynamicPropertyService: any;

  /* @ngInject */
  constructor(
    private ApiService: ApiService,
    private $mdSidenav: angular.material.ISidenavService,
    private $mdEditDialog,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
    private $scope: IApiPropertiesScope,
    private $rootScope,
    private readonly ngIfMatchEtagInterceptor: IfMatchEtagInterceptor,
  ) {
    this.$mdSidenav = $mdSidenav;
    this.$mdEditDialog = $mdEditDialog;
  }

  $onInit() {
    this.dynamicPropertyProviders = [
      {
        id: 'HTTP',
        name: 'Custom (HTTP)',
      },
    ];
    this.api = this.resolvedApi.data;

    this.editor = undefined;

    this.joltSpecificationOptions = {
      placeholder: 'Edit your JOLT specification here.',
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      mode: 'javascript',
      controller: this,
    };

    this.requestBodyOptions = {
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      controller: this,
    };

    this._initialDynamicPropertyService = _.cloneDeep(this.api.services && this.api.services['dynamic-property']);
    this.dynamicPropertyService = _.cloneDeep(this._initialDynamicPropertyService);

    if (this.dynamicPropertyService !== undefined) {
      this.$scope.dynamicPropertyEnabled = this.dynamicPropertyService.enabled;
    } else {
      this.$scope.dynamicPropertyEnabled = false;
    }

    this.$scope.$on('apiChangeSuccess', (event, args) => {
      if (args.api) {
        this.api = args.api;
      } else {
        this.ApiService.get(args.apiId).then((response) => (this.api = response.data));
      }
    });
  }

  hasPropertiesDefined() {
    return this.api.properties && Object.keys(this.api.properties).length > 0;
  }

  deleteProperty(key) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to remove property [' + key + ']?',
          msg: '',
          confirmButton: 'Remove',
        },
      })
      .then((response) => {
        if (response) {
          _.remove(this.api.properties, (property: any) => {
            return property.key === key;
          });

          this.update();
        }
      });
  }

  showPropertyModal() {
    this.$mdDialog
      .show({
        controller: 'DialogAddPropertyController',
        controllerAs: 'dialogAddPropertyCtrl',
        template: require('./add-property.dialog.html'),
        clickOutsideToClose: true,
      })
      .then((property) => {
        if (this.api.properties === undefined) {
          this.api.properties = [];
        }

        if (property) {
          this.api.properties.push(property);
          this.update();
        }
      });
  }

  updateSchedule(event) {
    if (event.target.valid) {
      this.dynamicPropertyService.schedule = event.target.value;
      this.$scope.formDynamicProperties.$invalid = false;
    } else {
      this.$scope.formDynamicProperties.$invalid = true;
    }
    this.$scope.formDynamicProperties.$pristine = false;
  }

  update() {
    this.api.services['dynamic-property'] = this.dynamicPropertyService;
    return this.ApiService.update(this.api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
      this.NotificationService.show("API '" + this.api.name + "' saved");
    });
  }

  editValue(event, property) {
    event.stopPropagation();
    this.$mdEditDialog.small({
      modelValue: property.value,
      placeholder: 'Set property value',
      save: (input) => {
        property.value = input.$modelValue;
        property.dynamic = false;
        this.update();
      },
      targetEvent: event,
      validators: {
        'md-maxlength': 160,
      },
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
    this.$mdSidenav('dynamic-properties-config')
      .open()
      .then(() => {
        if (this.editor) {
          this.editor.setSize('100%', '100%');
        }
      });
  }

  close() {
    this.$mdSidenav('dynamic-properties-config').close();
  }

  codemirrorLoaded(_editor) {
    this.controller.editor = _editor;

    // Editor part
    const _doc = this.controller.editor.getDoc();

    // Options
    _doc.markClean();
  }

  showExpectedProviderOutput() {
    this.$mdDialog.show({
      controller: 'DialogDynamicProviderHttpController',
      controllerAs: 'ctrl',
      template: require('./dynamic-provider-http.dialog.html'),
      parent: angular.element(document.body),
      clickOutsideToClose: true,
    });
  }

  deleteSelectedProperties() {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to remove selected properties?',
          msg: '',
          confirmButton: 'Remove',
        },
      })
      .then((response) => {
        if (response) {
          _.forEach(this.selectedProperties, (v, k) => {
            if (v) {
              _.remove(this.api.properties, (property: any) => {
                return property.key === k;
              });
              delete this.selectedProperties[k];
            }
          });
          this.update();
        }
      });
  }

  toggleSelectAll(selectAll) {
    if (selectAll) {
      _.forEach(this.api.properties, (p) => (this.selectedProperties[p.key] = true));
    } else {
      this.selectedProperties = {};
    }
  }

  checkSelectAll() {
    this.selectAll = _.filter(this.selectedProperties, (p) => p).length === Object.keys(this.api.properties).length;
  }

  hasSelectedProperties() {
    return _.filter(this.selectedProperties, (p) => p).length > 0;
  }

  reset() {
    this.dynamicPropertyService = _.cloneDeep(this._initialDynamicPropertyService);
    this.$scope.formDynamicProperties.$setPristine();
    this.$scope.formDynamicProperties.$setUntouched();
  }

  getHttpMethods() {
    return ['GET', 'DELETE', 'PATCH', 'POST', 'PUT', 'OPTIONS', 'TRACE', 'HEAD'];
  }

  addHTTPHeader() {
    if (this.dynamicPropertyService.configuration.headers === undefined) {
      this.dynamicPropertyService.configuration.headers = [];
    }

    this.dynamicPropertyService.configuration.headers.push({ name: '', value: '' });
  }

  removeHTTPHeader(idx) {
    if (this.dynamicPropertyService.configuration.headers !== undefined) {
      this.dynamicPropertyService.configuration.headers.splice(idx, 1);
    }
  }
}

export default ApiV1PropertiesControllerAjs;

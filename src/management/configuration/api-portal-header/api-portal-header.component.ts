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
import NotificationService from "../../../services/notification.service";
import {StateService} from '@uirouter/core';
import ApiHeaderService from "../../../services/apiHeader.service";
import {ApiPortalHeader} from "../../../entities/apiPortalHeader";
import PortalConfigService from "../../../services/portalConfig.service";
import _ = require('lodash');
import {IScope} from "angular";

const ApiPortalHeaderComponent: ng.IComponentOptions = {
  bindings: {
    apiPortalHeaders: '<'
  },
  template: require('./api-portal-header.html'),
  controller: function(
    ApiHeaderService: ApiHeaderService,
    NotificationService: NotificationService,
    PortalConfigService: PortalConfigService,
    $mdDialog: angular.material.IDialogService,
    Constants,
    $rootScope: IScope
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$mdDialog = $mdDialog;
    this.settings = _.cloneDeep(Constants);

    this.$onInit = () => {
    };

    this.upward = (header: ApiPortalHeader) => {
      header.order = header.order - 1;
      ApiHeaderService.update(header).then( response => {
        NotificationService.show("Header '" + header.name + "' saved");
        ApiHeaderService.list().then(response =>
          this.apiPortalHeaders = response.data);
      });
    };

    this.downward = (header: ApiPortalHeader) => {
      header.order = header.order + 1;
      ApiHeaderService.update(header).then( response => {
        NotificationService.show("Header '" + header.name + "' saved");
        ApiHeaderService.list().then(response =>
          this.apiPortalHeaders = response.data);
      });
    };

    this.createHeader = () => {
      let that = this;
      this.$mdDialog.show({
        controller: 'NewApiPortalHeaderDialogController',
        controllerAs: '$ctrl',
        template: require('./save.api-portal-header.dialog.html'),
        locals: {}
      }).then(function (newHeader) {
        NotificationService.show("Header '" + newHeader.name + "' saved");
        ApiHeaderService.list().then(response =>
          that.apiPortalHeaders = response.data);
      }).catch(function () {});
    };

    this.updateHeader = (header: ApiPortalHeader) => {
      let that = this;
      this.$mdDialog.show({
        controller: 'UpdateApiPortalHeaderDialogController',
        controllerAs: '$ctrl',
        template: require('./save.api-portal-header.dialog.html'),
        locals: {
          header: Object.assign({}, header)
        }
      }).then(function (updatedHeader) {
        NotificationService.show("Header '" + updatedHeader.name + "' saved");
        ApiHeaderService.list().then(response =>
          that.apiPortalHeaders = response.data);
      }).catch(function () {});
    };

    this.deleteHeader = (header: ApiPortalHeader) => {
      let that = this;
      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete this header ?',
          msg: '',
          confirmButton: 'Delete'
        }
      }).then(function (response) {
        if (response) {
          ApiHeaderService.delete(header).then(response => {
            NotificationService.show("Header '" + header.name + "' deleted");
            ApiHeaderService.list().then(response =>
              that.apiPortalHeaders = response.data);
          });
        }
      });
    };

    this.saveShowViews = () => {
      PortalConfigService.save({
        portal: {
          apis: {
            apiHeaderShowViews: {
              enabled: this.settings.portal.apis.apiHeaderShowViews.enabled
            }
          }
        }
      }).then( response => {
        NotificationService.show("Views are now " + (this.settings.portal.apis.apiHeaderShowViews.enabled?"visible":"hidden"));
      });
    };

    this.saveShowTags = () => {
      PortalConfigService.save({
        portal: {
          apis: {
            apiHeaderShowTags: {
              enabled: this.settings.portal.apis.apiHeaderShowTags.enabled
            }
          }
        }
      }).then( response => {
        NotificationService.show("Tags are now " + (this.settings.portal.apis.apiHeaderShowTags.enabled?"visible":"hidden"));
      });
    };


  }
};

export default ApiPortalHeaderComponent;

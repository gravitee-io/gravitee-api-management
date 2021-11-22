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
import { IScope } from 'angular';

import { ApiPortalHeader } from '../../../entities/apiPortalHeader';
import ApiHeaderService from '../../../services/apiHeader.service';
import NotificationService from '../../../services/notification.service';
import PortalSettingsService from '../../../services/portalSettings.service';

const ApiPortalHeaderComponent: ng.IComponentOptions = {
  bindings: {
    apiPortalHeaders: '<',
    settings: '<',
  },
  template: require('./api-portal-header.html'),
  controller: function (
    ApiHeaderService: ApiHeaderService,
    NotificationService: NotificationService,
    PortalSettingsService: PortalSettingsService,
    $mdDialog: angular.material.IDialogService,
    Constants,
    $rootScope: IScope,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$mdDialog = $mdDialog;

    this.providedConfigurationMessage = 'Configuration provided by the system';

    this.upward = (header: ApiPortalHeader) => {
      header.order = header.order - 1;
      ApiHeaderService.update(header).then(() => {
        NotificationService.show("Header '" + header.name + "' saved");
        ApiHeaderService.list().then((response) => (this.apiPortalHeaders = response.data));
      });
    };

    this.downward = (header: ApiPortalHeader) => {
      header.order = header.order + 1;
      ApiHeaderService.update(header).then(() => {
        NotificationService.show("Header '" + header.name + "' saved");
        ApiHeaderService.list().then((response) => (this.apiPortalHeaders = response.data));
      });
    };

    this.createHeader = () => {
      this.$mdDialog
        .show({
          controller: 'NewApiPortalHeaderDialogController',
          controllerAs: '$ctrl',
          template: require('./save.api-portal-header.dialog.html'),
          locals: {},
        })
        .then((newHeader) => {
          NotificationService.show("Header '" + newHeader.name + "' saved");
          ApiHeaderService.list().then((response) => (this.apiPortalHeaders = response.data));
        });
    };

    this.updateHeader = (header: ApiPortalHeader) => {
      this.$mdDialog
        .show({
          controller: 'UpdateApiPortalHeaderDialogController',
          controllerAs: '$ctrl',
          template: require('./save.api-portal-header.dialog.html'),
          locals: {
            header: Object.assign({}, header),
          },
        })
        .then((updatedHeader) => {
          NotificationService.show("Header '" + updatedHeader.name + "' saved");
          ApiHeaderService.list().then((response) => (this.apiPortalHeaders = response.data));
        });
    };

    this.deleteHeader = (header: ApiPortalHeader) => {
      this.$mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Are you sure you want to delete this header?',
            msg: '',
            confirmButton: 'Delete',
          },
        })
        .then((response) => {
          if (response) {
            ApiHeaderService.delete(header).then(() => {
              NotificationService.show("Header '" + header.name + "' deleted");
              ApiHeaderService.list().then((response) => (this.apiPortalHeaders = response.data));
            });
          }
        });
    };

    this.saveShowCategories = () => {
      PortalSettingsService.save(this.settings).then((response) => {
        NotificationService.show(
          'Categories are now ' + (this.settings.portal.apis.apiHeaderShowCategories.enabled ? 'visible' : 'hidden'),
        );
        this.settings = response.data;
      });
    };

    this.saveShowTags = () => {
      PortalSettingsService.save(this.settings).then((response) => {
        NotificationService.show('Tags are now ' + (this.settings.portal.apis.apiHeaderShowTags.enabled ? 'visible' : 'hidden'));
        this.settings = response.data;
      });
    };

    this.savePromotedApiMode = () => {
      PortalSettingsService.save(this.settings).then((response) => {
        NotificationService.show('Promoted API is now ' + (this.settings.portal.apis.promotedApiMode.enabled ? 'visible' : 'hidden'));
        this.settings = response.data;
      });
    };

    this.isReadonlySetting = (property: string): boolean => {
      return PortalSettingsService.isReadonly(this.settings, property);
    };
  },
};

export default ApiPortalHeaderComponent;

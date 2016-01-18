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
class ApplicationAPIKeysController {
  constructor(resolvedApplication, resolvedAPIKeys, ApplicationService, NotificationService, $mdDialog) {
    'ngInject';
    this.application = resolvedApplication.data;
    this.apiKeys = resolvedAPIKeys.data;
    this.showRevokedKeys = false;
    this.ApplicationService = ApplicationService;
    this.NotificationService = NotificationService;
    this.$mdDialog = $mdDialog;
  }

  getAPIKeys(applicationId) {
    this.ApplicationService.getAPIKeys(applicationId).then(response => {
      this.apiKeys = response.data;
    });
  }

  hasKeysDefined() {
    return this.apiKeys !== null && Object.keys(this.apiKeys).length > 0;
  }

  generateAPIKey(application, apiId) {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to renew your API Key ? Your previous API Key will be no longer valid in 1 hour !',
      ok: 'Renew',
      cancel: 'Cancel'
    });

    var _this = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        _this.ApplicationService.subscribe(application, apiId).then(() => {
          _this.NotificationService.show('A new API Key has been generated');
          _this.getAPIKeys(application.id);
        });
      })
      .catch(function () {
      });
  }

  revoke(apiKey) {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to revoke API Key \'' + apiKey + '\'?',
      ok: 'Revoke',
      cancel: 'Cancel'
    });

    var _this = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        _this.ApplicationService.revokeApiKey(_this.application.id, apiKey).then(() => {
          _this.NotificationService.show('API Key ' + apiKey + ' has been revoked !');

          _this.ApplicationService.getAPIKeys(_this.application.id).then(response => {
            _this.apiKeys = response.data;
          });
        });
      })
      .catch(function () {
      });
  }

  onClipboardSuccess(e) {
    this.NotificationService.show('API Key has been copied to clipboard');
    e.clearSelection();
  }

  showSubscribeApiModal(ev) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogSubscribeApiController',
      templateUrl: 'app/application/dialog/subscribeApi.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      application: that.application,
      apiKeys: that.apiKeys
    }).then(function (application) {
      if (application) {
        that.getAPIKeys(application.id);
      }
    }, function() {
       // You cancelled the dialog
    });
  }
}

export default ApplicationAPIKeysController;

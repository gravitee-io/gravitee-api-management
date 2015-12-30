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
class ApiKeysController {
  constructor($window, $mdDialog, $state, ApiService, NotificationService, resolvedApi, resolvedApiKeys) {
		'ngInject';
    this.$window = $window;
		this.$mdDialog = $mdDialog;
		this.$state = $state;
		this.ApiService = ApiService;
		this.NotificationService = NotificationService;

    this.api = resolvedApi.data;
    this.apiKeys = resolvedApiKeys.data;

    this.showRevokedKeys = false;
	}

  hasKeysDefined() {
    return this.apiKeys !== null && Object.keys(this.apiKeys).length > 0;
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
        _this.ApiService.revokeApiKey(_this.api.id, apiKey).then(() => {
          _this.NotificationService.show('API Key ' + apiKey + ' has been revoked !');

          _this.ApiService.getApiKeys(_this.api.id).then(response => {
            _this.apiKeys = response.data;
          });
        });
      })
      .catch(function () {
      });
  }

  showExpirationModal(apikey) {
    var _this = this;
    this.currentApiKey = apikey;

    this.$mdDialog.show({
      controller: 'DialogApiKeyExpirationController',
      controllerAs: 'dialogApiKeyExpirationController',
      templateUrl: 'app/api/admin/apikeys/apikey-expiration.dialog.html',
      clickOutsideToClose: true
    }).then(function (expirationDate) {
      _this.currentApiKey.expire_on = expirationDate;

      _this.ApiService.updateApiKey(_this.api.id, _this.currentApiKey).then(() => {
        _this.NotificationService.show('An expiration date has been settled for API Key');
      });
    });
  }
}

export default ApiKeysController;

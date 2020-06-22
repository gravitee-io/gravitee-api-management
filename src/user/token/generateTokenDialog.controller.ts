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
function DialogGenerateTokenController($scope, $mdDialog, locals, TokenService, NotificationService, Constants, $location) {
  'ngInject';

  this.title = locals.title;
  this.msg = locals.msg;

  this.cancel = function () {
    $mdDialog.hide(false);
  };

  this.confirm = function () {
    $mdDialog.hide(true);
  };

  this.generate = function () {
    TokenService.create({ name: this.name }).then((response) => {
      NotificationService.show('Token "' + this.name + '" has been successfully generated.');
      this.token = response.data;
    });
  };

  this.onClipboardSuccess = function () {
    NotificationService.show('The token has been copied to clipboard');
  };

  this.getExampleOfUse = function (token) {
    const envBaseURL = Constants.envBaseURL.startsWith('/') ?
      $location.absUrl().split('/#')[0] + Constants.envBaseURL : Constants.envBaseURL;
    return `curl -H "Authorization: Bearer ${token}" "${envBaseURL}"`;
  };
}

export default DialogGenerateTokenController;

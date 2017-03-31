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
function DialogApplicationPermissionsHelpController($scope, $mdDialog) {
  'ngInject';

  $scope.permissions = [
    {
      "action": "View application",
      "user": true,
      "owner": true,
      "primary_owner": true
    },
    {
      "action": "View API Keys",
      "user": true,
      "owner": true,
      "primary_owner": true
    },
    {
      "action": "Configure application",
      "user": false,
      "owner": true,
      "primary_owner": true
    },
    {
      "action": "Generate or renew API Keys",
      "user": false,
      "owner": true,
      "primary_owner": true
    },
    {
      "action": "Manage members",
      "anyone": false,
      "user": false,
      "owner": true,
      "primary_owner": true
    },
    {
      "action": "Delete application",
      "user": false,
      "owner": false,
      "primary_owner": true
    },
    {
      "action": "Transfer ownership (primary_owner)",
      "user": false,
      "owner": false,
      "primary_owner": true
    }
  ];

  this.cancel = $mdDialog.cancel;
}

export default DialogApplicationPermissionsHelpController;

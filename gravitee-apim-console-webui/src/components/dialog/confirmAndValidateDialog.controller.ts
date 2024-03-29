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
function DialogConfirmAndValidateController($scope, $mdDialog, locals) {
  $scope.title = locals.title;
  $scope.msg = locals.msg;
  $scope.warning = locals.warning;
  $scope.validationMessage = locals.validationMessage;
  $scope.confirmButton = locals.confirmButton;
  $scope.validationValue = locals.validationValue;
  $scope.confirmValue = '';

  this.cancel = function () {
    $mdDialog.hide(false);
  };

  this.confirm = function () {
    $mdDialog.hide(true);
  };
}
DialogConfirmAndValidateController.$inject = ['$scope', '$mdDialog', 'locals'];

export default DialogConfirmAndValidateController;

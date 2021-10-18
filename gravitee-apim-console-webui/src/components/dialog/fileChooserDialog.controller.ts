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
function FileChooserDialogController($scope, $mdDialog, locals, Constants, NotificationService) {
  'ngInject';

  $scope.title = locals.title;
  $scope.confirmButton = locals.confirmButton || 'OK';
  $scope.cancelButton = locals.cancelButton || 'Cancel';

  this.cancel = function () {
    $mdDialog.hide(false);
  };

  this.confirm = function () {
    if ($scope.selectedFile && $scope.selectedFile.size > Constants.env.settings.portal.uploadMedia.maxSizeInOctet) {
      NotificationService.showError(
        "file uploaded to big, you're limited at " + Constants.env.settings.portal.uploadMedia.maxSizeInOctet + ' bytes',
      );
    } else {
      $mdDialog.hide({ file: $scope.selectedFile, filename: $scope.filename });
    }
  };
}

export default FileChooserDialogController;

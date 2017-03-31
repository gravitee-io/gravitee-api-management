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
function DialogApplicationController($scope, $mdDialog, ApplicationService, NotificationService) {
  'ngInject';

  $scope.hide = function () {
     $mdDialog.cancel();
  };

  $scope.create = function (application) {
    ApplicationService.create(application).then(function (result) {
			NotificationService.show('Application created');
      $mdDialog.hide(result);
    }).catch(function (error) {
			NotificationService.show('Error while creating the application');
      $scope.error = error.data.message;
    });
  };
}

export default DialogApplicationController;

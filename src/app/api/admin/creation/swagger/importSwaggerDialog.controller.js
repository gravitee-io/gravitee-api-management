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
function DialogApiSwaggerImportController($scope, $mdDialog, ApiService, apiId) {
  'ngInject';

  $scope.swagger = {
    version: 'VERSION_2_0',
    type: 'INLINE',
    name: ''
  };

  this.onTypeChange = function() {
    delete $scope.swagger.payload;
    delete $scope.swagger.url;
  };

  this.hide = function() {
    $mdDialog.hide();
  };

  this.import = function() {
    delete $scope.swagger.name;
    if ($scope.swagger.type == 'INLINE') {
      delete $scope.swagger.url;
    } else {
      $scope.swagger.payload = $scope.swagger.url;
      delete $scope.swagger.url;
    }
    ApiService.importSwagger($scope.swagger).then(function (response) {
      $mdDialog.hide(response.data);
    });
  };
}

export default DialogApiSwaggerImportController;

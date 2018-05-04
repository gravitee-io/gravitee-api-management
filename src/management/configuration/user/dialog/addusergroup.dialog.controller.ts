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

function DialogAddUserGroupController(
  $scope: ng.IScope,
  $mdDialog: angular.material.IDialogService,
  groups: any[],
  apiRoles: any[],
  applicationRoles: any[]) {
  'ngInject';
  this.groups = groups;
  this.apiRoles = apiRoles;
  this.applicationRoles = applicationRoles;

  this.hide = () => {
    $mdDialog.hide();
  };

  this.atLeastOneRoleSelected = () => {
    return $scope.selectedApiRole || $scope.selectedApplicationRole;
  };

  this.save = () => {
    let roles = {};
    if($scope.selectedApplicationRole) {
      roles["APPLICATION"] = $scope.selectedApplicationRole;
    }
    if($scope.selectedApiRole) {
      roles["API"] = $scope.selectedApiRole;
    }

    $mdDialog.hide({
      id: $scope.selectedGroup,
      roles: roles
    });
  };
}

export default DialogAddUserGroupController;
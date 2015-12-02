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
class ApiAdminController {
  constructor (resolvedApi, $state, $scope) {
    'ngInject';
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;

    $scope.$on('$stateChangeSuccess', function (ev, to, toParams, from) {
      if (from.name.startsWith('apis.list.')) {
        $scope.$parent.previousState = from.name;
      }

      if ($state.current.name.includes('general')) {
        $scope.selectedTab = 0;
      } else if ($state.current.name.includes('policies')) {
        $scope.selectedTab = 1;
      } else if ($state.current.name.includes('documentation')) {
        $scope.selectedTab = 2;
      } else if ($state.current.name.endsWith('apikeys')) {
        $scope.selectedTab = 4;
      } else if ($state.current.name.endsWith('members')) {
        $scope.selectedTab = 5;
      } else if ($state.current.name.endsWith('properties')) {
        $scope.selectedTab = 6;
      } else if ($state.current.name.includes('analytics')) {
        $scope.selectedTab = 7;
      } else if ($state.current.name.endsWith('monitoring')) {
        $scope.selectedTab = 8;
      }
    });
  }
}

export default ApiAdminController;

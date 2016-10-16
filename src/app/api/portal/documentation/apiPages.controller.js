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
class ApiPortalPagesController {
  constructor(resolvedPages, $scope, $stateParams, $location) {
    'ngInject';
    this.pages = resolvedPages.data;

    $scope.selectedIndex = 0;

    var that = this;
    $scope.$watch('selectedIndex', function (current) {
      if (that.pages.length && !$stateParams.pageId && !current) {
        $location.url('/apis/' + $stateParams.apiId + '/pages/' + that.pages[0].id);
      }
    });
  }
}

export default ApiPortalPagesController;

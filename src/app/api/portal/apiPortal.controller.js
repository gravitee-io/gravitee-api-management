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
class ApiPortalController {
  constructor ($scope, $state, $location, DocumentationService, resolvedApi, resolvedPages, $rootScope) {
    'ngInject';
    this.$scope = $scope;
    this.$state = $state;
    this.$location = $location;
    this.DocumentationService = DocumentationService;
    this.api = resolvedApi.data;

    $rootScope.currentResource = this.api.name;

    this.pages = resolvedPages.data;
    this.$scope.selectedIndex = 0;

    var that = this;
    this.$scope.$watch('selectedIndex', function(current, old) {
      if (that.pages.length > 0 && that.$state.params.pageId === undefined) {
        switch (current) {
          case 0:
            $location.url('/apis/' + that.$state.params.apiId + '/pages/' + that.pages[0].id);
            break;
        }
      }
    });
  }

  fetchPage(page) {
    var that = this;
    this.DocumentationService.get(this.api.id, page.id).then(function(response) {
      that.page = response.data;
    });
  }
}

export default ApiPortalController;

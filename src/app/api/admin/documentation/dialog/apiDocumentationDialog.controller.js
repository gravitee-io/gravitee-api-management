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
function DialogDocumentationController($scope, $mdDialog, DocumentationService, apiId) {
  'ngInject';

  var that = this;
  $scope.$watch('pageContentFile.content', function (data) {
    if (data) {
      that.selectPageType($scope.IMPORT);
    } else {
      that.selectPageType(null);
    }
  });

  $scope.pageContentFile = {content: '', name : ''};

  $scope.MARKDOWN_PAGE = 'MARKDOWN';
  $scope.RAML_PAGE = 'RAML';
  $scope.SWAGGER_PAGE = 'SWAGGER';
  $scope.IMPORT = 'IMPORT';
  $scope.pageTitle = null;

  this.hide = function(){
    $mdDialog.hide();
  };

  this.selectPageType = function(pageType) {
    this.selectedPageType = pageType;
  };

  function getPageType() {
    if ($scope.pageContentFile.name) {
      var fileExtension = $scope.pageContentFile.name.split('.').pop();
      switch (fileExtension) {
        case 'md':
          return $scope.MARKDOWN_PAGE;
        case 'raml':
          return $scope.RAML_PAGE;
        case 'json':
          return $scope.SWAGGER_PAGE;
        default :
          throw 'The document extension is not managed:' + fileExtension;
      }
    }
    return that.selectedPageType;
  }

  this.createPage = function() {
    var selectedPageType = getPageType();
    if (selectedPageType && $scope.pageTitle) {
      var newPage = {
        'name' : $scope.pageTitle.replace(/\s/g, '-').toLowerCase(),
        'type' : selectedPageType,
        'title' : $scope.pageTitle,
        'content' : $scope.pageContentFile.content
      };
      DocumentationService.createPage(apiId, newPage).then(function (page) {
        $mdDialog.hide(page);
      }).catch(function (error) {
        $scope.error = error;
      });
    }
  };
}

export default DialogDocumentationController;
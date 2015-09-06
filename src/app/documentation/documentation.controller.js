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
/* global document:false */
class DocumentationController {
  constructor(DocumentationService, $mdDialog) {
    'ngInject';
    this.DocumentationService = DocumentationService;
    this.$mdDialog = $mdDialog;
    this.selected = null;
    this.pages = [ ];
    this.init();
  }

  selectPage(page) {
    this.selected = angular.isNumber(page) ? this.pages[page] : page;
  }

  init() {
    // TODO get the real api name
    var apiName = "TEST";
    this.DocumentationService.list(apiName).then(response => {
      this.pages = response.data;
      this.selected = this.pages[0];
    });
  }

  list() {
    // TODO get the real api name
    var apiName = "TEST";
    this.DocumentationService.list(apiName).then(response => {
      this.pages = response.data;
      this.selected = this.pages[this.pages.length - 1];
    });
  }

  editPage() {
    var self = this;
    var editPage = {
      "title" : this.selected.title,
      "content": this.selected.content
    };
    this.DocumentationService.editPage(this.selected.name, editPage).then(function () {
        self.init();
    });
  }

  deletePage() {
    if (confirm("Are you sure to delete")) {
      var self = this;
      this.DocumentationService.deletePage(this.selected.name).then(function () {
        self.init();
      });
    }
  }

  showNewPageDialog(event) {
    var self = this;
    this.$mdDialog.show({
      controller: DialogDocumentationController,
      templateUrl: 'app/documentation/documentation.dialog.html',
      parent: angular.element(document.body),
    }).then(function (response) {
      self.list();
    });
  }
}

function DialogDocumentationController($scope, $mdDialog, DocumentationService) {
  'ngInject';

  $scope.MARKDOWN_PAGE = 'MARKDOWN';
  $scope.RAML_PAGE = 'RAML';
  $scope.SWAGGER_PAGE = 'SWAGGER';
  $scope.selectedPageType = null;
  $scope.pageTitle = null;

  $scope.hide = function(){
    $mdDialog.hide();
  };

  $scope.selectPageType = function(pageType) {
    $scope.selectedPageType = pageType;
  };

  $scope.createPage = function() {
    if ($scope.selectedPageType != null && ($scope.pageTitle != null && $scope.pageTitle.trim() != '')) {
      var newPage = {
        "name" : $scope.pageTitle.replace(/\s/g, "-").toLowerCase(),
        "type" : $scope.selectedPageType,
        "title" : $scope.pageTitle,
        "apiName": "TEST"
      };
      DocumentationService.createPage(newPage).then(function (page) {
        $mdDialog.hide(page);
      }).catch(function (error) {
        $scope.error = error;
      });
    }
  };
}

export default DocumentationController;

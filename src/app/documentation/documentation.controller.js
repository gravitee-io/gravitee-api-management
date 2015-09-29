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
/* global document:false, confirm:false */
class DocumentationController {
  
	constructor(DocumentationService, $mdDialog, $location, $state, ApiService) {
    'ngInject';
    this.DocumentationService = DocumentationService;
    this.$mdDialog = $mdDialog;
    this.selected = null;
    this.pages = [];
		this.previewMode = true;
		this.editMode = false;
		this.MARKDOWN_PAGE = 'MARKDOWN';
    this.RAML_PAGE = 'RAML';
  	this.SWAGGER_PAGE = 'SWAGGER';
    this.location = $location;
    this.state = $state;

    var that = this;

    ApiService.list().then(function(response) {
        that.apis = response.data;
        if (that.apis.length > 0) {
          var currentApiName = that.location.$$search.api;
          for (var i = 0; i < that.apis.length; i++) {
            if (that.apis[i].name === currentApiName) {
              that.selectApi(that.apis[i]);
              break;
            }
          }
        }
    });
  }

  init() {
    this.list();
    this.preview();
  }

  selectApi(api, clearPage) {
    if (clearPage) {
      delete this.location.$$search.page;
      this.selected = null;
    }

    this.location.search('api', api.name);
    this.selectedApi = api;
    this.init();
  }

  selectPage(page) {
    this.selected = angular.isNumber(page) ? this.pages[page] : page;
    this.location.search('page', page.name);
  }

  list() {
    if (this.selectedApi) {
      this.DocumentationService.list(this.selectedApi.name).then(response => {
        this.pages = response.data;
        if (this.pages.length > 0) {
          var currentPageName = this.location.$$search.page;
          for (var i = 0; i < this.pages.length; i++) {
            if (this.pages[i].name === currentPageName) {
              this.selectPage(this.pages[i]);
              break;
            }
          }
        } else {
          this.selectApi(this.selectedApi, true);
        }
      });
    }
  }

  getContentUrl() {
    return this.DocumentationService.getContentUrl(this.selected.name);
  }

  editPage() {
    var self = this;
    var editPage = {
      'title' : this.selected.title,
      'content': this.selected.content
    };
    this.DocumentationService.editPage(this.selected.name, editPage).then(function () {
      self.state.transitionTo(self.state.current, self.state.$current.search, { reload: true, location:false });
    });
  }

  deletePage() {
    if (confirm('Are you sure to delete')) {
      var self = this;
      this.DocumentationService.deletePage(this.selected.name).then(function () {
        self.selectApi(self.selectedApi, true);
      });
    }
  }

  showNewPageDialog() {
    var self = this;
    this.$mdDialog.show({
      controller: DialogDocumentationController,
      templateUrl: 'app/documentation/documentation.dialog.html',
      parent: angular.element(document.body),
      apiName: self.selectedApi.name
    }).then(function (response) {
      self.location.search('page', response.data.name);
      if (!response.data.content) {
			  self.edit();
      }
      self.list();
    });
  }

	// swith mode (edit/preview)
	edit() {
		this.editMode = true;
		this.previewMode = false;
	}

	preview() {
		this.editMode = false;
		this.previewMode = true;
	}

  ramlType() {
    return this.selected && this.RAML_PAGE === this.selected.type;
  }

  markdownType() {
    return this.selected && this.MARKDOWN_PAGE === this.selected.type;
  }

  swaggerType() {
    return this.selected && this.SWAGGER_PAGE === this.selected.type;
  }
}

function DialogDocumentationController($scope, $mdDialog, DocumentationService, apiName) {
  'ngInject';

  $scope.$watch('pageContentFile.content', function (data) {
    if (data) {
      $scope.selectPageType($scope.IMPORT);
    } else {
      $scope.selectPageType(null);
    }
  });

  $scope.pageContentFile = {content: '', name : ''};

  $scope.MARKDOWN_PAGE = 'MARKDOWN';
  $scope.RAML_PAGE = 'RAML';
  $scope.SWAGGER_PAGE = 'SWAGGER';
  $scope.IMPORT = 'IMPORT';
  $scope.pageTitle = null;
  $scope.apiName = apiName;

  $scope.hide = function(){
    $mdDialog.hide();
  };

  $scope.selectPageType = function(pageType) {
    $scope.selectedPageType = pageType;
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
    return $scope.selectedPageType;
  }

  $scope.createPage = function() {
    var selectedPageType = getPageType();
    if (selectedPageType && $scope.pageTitle) {
      var newPage = {
        'name' : $scope.pageTitle.replace(/\s/g, '-').toLowerCase(),
        'type' : selectedPageType,
        'title' : $scope.pageTitle,
        'content' : $scope.pageContentFile.content,
        'apiName': $scope.apiName
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

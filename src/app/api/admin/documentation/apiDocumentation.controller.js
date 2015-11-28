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
class DocumentationController {
  
	constructor(DocumentationService, $mdDialog, $scope, $state, resolvedApi) {
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
    this.$state = $state;

    this.api = resolvedApi.data;

    var that = this;
    $scope.$on('onGraviteePageDeleted', function () {
      that.$state.go('apis.admin.documentation');
      that.list();
    });
  }

  list() {
    if (this.api) {
      this.DocumentationService.list(this.api.id).then(response => {
        this.pages = response.data;
      });
    }
  }

  showNewPageDialog() {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogDocumentationController',
      controllerAs: 'dialogDocumentationCtrl',
      templateUrl: 'app/api/admin/documentation/dialog/apiDocumentation.dialog.html',
      apiId: this.api.id
    }).then(function (response) {
      if (response) {
        that.$state.go('apis.admin.documentation.page', {pageId: response.data.id});
        that.list();
      }
    });
  }
}

export default DocumentationController;

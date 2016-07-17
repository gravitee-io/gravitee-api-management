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

	constructor(DocumentationService, $mdDialog, $scope, $state, dragularService) {
    'ngInject';
    this.DocumentationService = DocumentationService;
    this.$mdDialog = $mdDialog;
		this.editMode = false;

    this.$state = $state;
    this.$scope = $scope;
    this.DragularService = dragularService;

    $scope.listPagesDisplayed = true;

    var that = this;
    $scope.$on('onGraviteePageDeleted', function () {
      that.$state.go('apis.admin.documentation');
      that.list();
    });
  }

  init() {
    let that = this;
    this.list().then( ({pages}) => {
      let d = document.querySelector('.pages');
      that.DragularService([d], {
        scope: this.$scope,
        containersModel: _.cloneDeep(this.pages),
        nameSpace: 'documentation'
      });
      that.$scope.$on('dragulardrop', function(e, el, target, source, dragularList, index) {
        let movedPage = that.pages[index];
        for (let idx = 0; idx < dragularList.length; idx++) {
          if (movedPage.id === dragularList[idx].id) {
            movedPage.order = idx+1;
            break;
          }
        }
        that.pages = dragularList;
        that.DocumentationService.editPage(that.$scope.$parent.apiCtrl.api.id, movedPage.id, movedPage).then(function () {
          // sync list from server because orders has been changed
          that.list();
        });
      });
    });
  }
  list() {
    return this.DocumentationService.list(this.$scope.$parent.apiCtrl.api.id).then(response => {
      this.pages = response.data;
      return {pages: this.pages};
    }).then( response => {
      if(response.pages && response.pages.length > 0) {
        this.$state.go("apis.admin.documentation.page", {pageId: response.pages[0].id});
      }
      return response;
    });
  }

  showNewPageDialog(pageType) {
    this.$state.go('apis.admin.documentation.new', {type: pageType});
  }
}

export default DocumentationController;

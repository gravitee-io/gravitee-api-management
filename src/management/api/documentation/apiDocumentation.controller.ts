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

import _ = require('lodash');

class DocumentationController {
  private editMode: boolean;
  private pages: any;

	constructor(
    private DocumentationService,
    private $scope,
    private $state,
    private dragularService,
    private NotificationService) {
    'ngInject';

    this.DocumentationService = DocumentationService;
		this.editMode = false;

    $scope.listPagesDisplayed = true;

    $scope.$on('onGraviteePageDeleted', () => {
      this.$state.go('management.apis.detail.documentation', {}, {reload: true});
    });
  }

  $onInit() {
    this.list().then( () => {
      let d = document.querySelector('.pages');
      this.dragularService([d], {
        scope: this.$scope,
        containersModel: _.cloneDeep(this.pages),
        nameSpace: 'documentation'
      });
      this.$scope.$on('dragulardrop', (e, el, target, source, dragularList, index) => {
        let movedPage = this.pages[index];
        for (let idx = 0; idx < dragularList.length; idx++) {
          if (movedPage.id === dragularList[idx].id) {
            movedPage.order = idx+1;
            break;
          }
        }
        this.pages = dragularList;
        this.DocumentationService.editPage(this.$state.params.apiId, movedPage.id, movedPage).then( () => {
          this.NotificationService.show('Page \'' + movedPage.name + '\' has been moved with success');
          this.$state.go("management.apis.detail.documentation.page", {apiId: this.$state.params.apiId, pageId: movedPage.id});
          this.list();
        });
      });
    });
  }

  list() {
    return this.DocumentationService.list(this.$state.params.apiId).then(response => {
      this.pages = response.data;
      return {pages: this.pages};
    }).then( response => {
      if(response.pages && response.pages.length > 0) {
        this.$state.go("management.apis.detail.documentation.page", {pageId: this.getPageId(response.pages[0].id)});
      }
      return response;
    });
  }

  showNewPageDialog(pageType) {
	  this.$state.go('management.apis.detail.documentation.new', {type: pageType, fallbackPageId: this.getPageId()});
  }

  getPageId(defaultPageId?: string) {
    if (this.$state.params.pageId !== undefined) {
      return this.$state.params.pageId;
    } else {
      return defaultPageId;
    }
  }
}

export default DocumentationController;

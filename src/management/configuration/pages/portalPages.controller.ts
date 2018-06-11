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
import PortalPagesService from '../../../services/portalPages.service';

class PortalPagesController {
  private editMode: boolean;
  private pages: any[];

  constructor(
    private PortalPagesService: PortalPagesService,
    private $scope: any,
    private $state: any,
    private dragularService: any) {
    'ngInject';

    this.editMode = false;
    $scope.listPagesDisplayed = true;

    $scope.$on('onGraviteePageDeleted', () => {
      this.$state.go('management.settings.pages', {}, {reload: true});
    });
  }

  $onInit() {
    let that = this;
    if (this.pages.length && !this.$state.params.pageId) {
      this.$state.go("management.settings.pages.page", {pageId: this.pages[0].id});
    }
    this.list().then( () => {
      let d = document.querySelector('.pages');
      that.dragularService([d], {
        scope: this.$scope,
        containersModel: _.cloneDeep(this.pages),
        nameSpace: 'documentation'
      });
      that.$scope.$on('dragulardrop', function(e: any, el: any, target: any, source: any, dragularList: any[], index: number) {
        let movedPage = that.pages[index];
        for (let idx = 0; idx < dragularList.length; idx++) {
          if (movedPage.id === dragularList[idx].id) {
            movedPage.order = idx + 1;
            break;
          }
        }
        that.pages = dragularList;
        that.PortalPagesService.editPage(movedPage.id, movedPage).then( () => {
          that.$state.go("management.settings.pages.page", {apiId: that.$state.params.apiId, pageId: movedPage.id});
        });
      });
    });
  }

  list() {
    return this.PortalPagesService.fullList().then( response => {
      this.pages = response.pages;
      if (response.pages && response.pages.length > 0) {
        if (this.$state.params.pageId !== undefined) {
          this.$state.go("management.settings.pages.page", {pageId: this.$state.params.pageId});
        } else {
          this.$state.go("management.settings.pages.page", {pageId: response.pages[0].id});
        }
      }
      return response;
    });
  }

  showNewPageDialog(pageType: string) {
    this.$state.go('management.settings.pages.new', {type: pageType});
  }
}

export default PortalPagesController;

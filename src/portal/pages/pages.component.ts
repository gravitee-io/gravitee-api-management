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
class PagesComponentCtrl implements ng.IComponentController {

  public pages: any;
  private selectedPage;
  private icon: string;

  constructor(
    private $state,
    private $stateParams) {
    'ngInject';

  }

  $onInit() {
    this.icon = "icon-angle-up";

    if (this.pages.length && !this.$stateParams.pageId) {
      this.selectPage(this.pages[0]);
    } else {
      const page = this.pages.find(p => p.id === this.$stateParams.pageId);

      if (page && this.isFolder(page) && page.pages && page.pages.length > 0) {
        page.pages[0].selected = true;
        this.selectedPage = page.pages[0];
      } else if (page) {
        page.selected = true;
        this.selectedPage = page;
      }
    }
  };

  selectPage(page: any) {
    if (this.selectedPage !== undefined) {
      this.selectedPage.selected = false;
    }

    this.selectedPage = page;
    this.selectedPage.selected = true;

    page.selected = true;
    this.$state.go('portal.pages.page', {pageId: page.id});
  }

  isFolder(page: any) {
    return page.type === 'folder';
  }

  toggleFolder(page:any) {
    page.isFolderOpen = !page.isFolderOpen;
    page.icon = page.isFolderOpen ? "icon-angle-down" : "icon-angle-up";
  }

}

const PagesComponent: ng.IComponentOptions = {
  bindings: {
    pages: '<'
  },
  template: require('./pages.html'),
  controller: PagesComponentCtrl
};

export default PagesComponent;

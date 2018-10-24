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
import {StateService, StateParams} from '@uirouter/core';

class PortalPagesComponentCtrl implements ng.IComponentController {
  public resolvedPages: any[];
  private api: string;
  private selectedPage;

  constructor(
    private $state: StateService,
    private $stateParams: StateParams,
    ) {
    'ngInject';
  }

  pages = [];

  $onInit() {
    const pagesMap: any = _.keyBy(this.resolvedPages, 'id');
    for (let idx in this.resolvedPages) {
      let page = this.resolvedPages[idx];
      if (page["parentId"]) {
        let rootPage = pagesMap[page["parentId"]];
        if (!rootPage) {
          console.error("Unable to find parent page with id:", page["parentId"]);
          console.error("Child page is: ", page);
        } else {
          if (!rootPage.pages) {
            rootPage.pages = [page];
          } else {
            rootPage.pages.push(page)
          }
        }
      }
    }

    this.pages = _.sortBy(
      _.filter(_.values(pagesMap), (p) => !p.parentId),
      ["order"]);

    if (this.pages.length && !this.$stateParams.pageId) {
      let firstPage = _.find(this.pages, (p) => { return !p.parentId && p.type !== 'FOLDER'});
      if (firstPage) {
        this.selectPage(firstPage);
      }
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

  selectPage (page) {
    if (this.selectedPage !== undefined) {
      this.selectedPage.selected = false;
    }

    this.selectedPage = page;
    this.selectedPage.selected = true;

    page.selected = true;
    if (this.api) {
      this.$state.go('portal.api.pages.page', {pageId: page.id});
    } else {
      this.$state.go('portal.pages.page', {pageId: page.id});
    }
  }

  isFolder(page: any) {
    return page.type === 'FOLDER';
  }

  toggleFolder(page:any) {
    page.isFolderOpen = !page.isFolderOpen;
    page.icon = page.isFolderOpen ? "icon-angle-down" : "icon-angle-up";
  }
}

const PortalPagesComponent: ng.IComponentOptions = {
  bindings: {
    resolvedPages: '<',
    api: '<'
  },
  template: require('./pages.html'),
  controller: PortalPagesComponentCtrl
};

export default PortalPagesComponent;

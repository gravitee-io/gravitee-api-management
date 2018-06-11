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
import * as _ from 'lodash';

class ApiPagesComponentCtrl implements ng.IComponentController {
  public pages: any;
  private selectedPage;

  constructor(
    private $state,
    private $stateParams,
    private $location
    ) {
    'ngInject';

  }

  $onInit() {
    if (this.pages.length && !this.$stateParams.pageId) {

      this.selectedPage = this.pages[0];
      this.selectedPage.selected = true;
      this.$location.url(`/apis/${this.$stateParams.apiId}/pages/${this.pages[0].id}`);
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
    this.$state.go('portal.api.pages.page', {pageId: page.id});
  }

  isFolder(page: any) {
    return page.type === 'folder';
  }
}

const ApiPagesComponent: ng.IComponentOptions = {
  bindings: {
    pages: '<',
    api: '<'
  },
  template: require('./api-pages.html'),
  controller: ApiPagesComponentCtrl
};

export default ApiPagesComponent;

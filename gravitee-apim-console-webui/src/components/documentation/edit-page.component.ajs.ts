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

import angular, { IController, IScope } from 'angular';

import { ActivatedRoute, Router } from '@angular/router';
import { Dictionary, keyBy } from 'lodash';
import { deepClone } from '@gravitee/ui-components/src/lib/utils';

import { emptyFetcher } from './edit-tabs/edit-page-fetchers.component';

import { DocumentationService, PageType } from '../../services/documentation.service';
import NotificationService from '../../services/notification.service';
import UserService from '../../services/user.service';

interface IPageScope extends IScope {
  fetcherJsonSchema: {
    type: string;
    id: string;
    properties: any;
  };
  rename: boolean;
  editorReadonly: boolean;
  currentTab: string;
  currentTranslation: any;
  acls: any;
}

class EditPageComponentController implements IController {
  resolvedPage: any;
  resolvedGroups: any[];
  resolvedFetchers: any[];
  pagesToLink: any[];
  folders: any[];
  systemFolders: any[];
  pageResources: any[];
  categoryResources: any[];
  attachedResources: any[];
  activatedRoute: ActivatedRoute;

  apiId: string;
  pageId: string;
  tabs: { id: number; name: string; isUnavailable: () => boolean }[];
  error: any;
  page: any;
  selectedTab: number;
  currentTab: string;
  groups: any[];
  foldersById: Dictionary<any>;
  systemFoldersById: Dictionary<any>;
  pageList: any[];
  canUpdate: boolean;
  newName: any;
  isLoading = false;

  constructor(
    private readonly NotificationService: NotificationService,
    private readonly DocumentationService: DocumentationService,
    private readonly UserService: UserService,
    private $scope: IPageScope,
    private ngRouter: Router,
  ) {
    this.tabs = [
      {
        id: 0,
        name: 'content',
        isUnavailable: () => {
          return false;
        },
      },
      {
        id: 1,
        name: 'translations',
        isUnavailable: () => {
          return this.isMarkdownTemplate();
        },
      },
      {
        id: 2,
        name: 'config',
        isUnavailable: () => {
          return this.isLink();
        },
      },
      {
        id: 3,
        name: 'fetchers',
        isUnavailable: () => {
          return this.isLink();
        },
      },
      {
        id: 4,
        name: 'access-control',
        isUnavailable: () => {
          return false;
        },
      },
      {
        id: 5,
        name: 'attached-resources',
        isUnavailable: () => {
          return this.isMarkdownTemplate() || this.isLink();
        },
      },
    ];

    this.error = null;
    this.$scope.rename = false;
    this.$scope.acls = {
      groups: [],
      roles: [],
    };
  }

  $onInit() {
    this.apiId = this.activatedRoute.snapshot.params.apiId;
    this.pageId = this.activatedRoute.snapshot.params.pageId;
    this.page = deepClone(this.resolvedPage);
    this.tabs = this.tabs.filter((tab) => !tab.isUnavailable());
    const indexOfTab = this.tabs.findIndex((tab) => tab.name === this.activatedRoute.snapshot.queryParams.tab);
    this.selectedTab = indexOfTab > -1 ? indexOfTab : 0;
    this.currentTab = this.tabs[this.selectedTab].name;
    if (this.resolvedPage.messages && this.resolvedPage.messages.length > 0) {
      this.error = {
        title: 'Validation messages',
        message: this.resolvedPage.messages,
      };
    }
    this.groups = this.resolvedGroups;

    this.foldersById = keyBy(this.folders, 'id');
    this.systemFoldersById = keyBy(this.systemFolders, 'id');
    const folderSituation = this.DocumentationService.getFolderSituation(this.systemFoldersById, this.foldersById, this.page.parentId);
    this.pageList = this.DocumentationService.buildPageList(this.pageResources, true, folderSituation);
    this.pagesToLink = this.DocumentationService.buildPageList(this.pagesToLink, false, folderSituation);
    if (this.DocumentationService.supportedTypes(folderSituation).indexOf(this.page.type) < 0) {
      this.cancel();
    }

    this.initEditor();

    if (this.apiId) {
      this.canUpdate = this.UserService.isUserHasPermissions(['api-documentation-u']);
    } else {
      this.canUpdate = this.UserService.isUserHasPermissions(['environment-documentation-u']);
    }

    if (this.page.type === 'SWAGGER') {
      if (!this.page.configuration) {
        this.page.configuration = {};
      }
    }
  }

  isFolder(): boolean {
    return PageType.FOLDER === this.page?.type;
  }
  isLink(): boolean {
    return PageType.LINK === this.page?.type;
  }
  isSwagger(): boolean {
    return PageType.SWAGGER === this.page?.type;
  }
  isMarkdown(): boolean {
    return PageType.MARKDOWN === this.page?.type;
  }
  isMarkdownTemplate(): boolean {
    return PageType.MARKDOWN_TEMPLATE === this.page?.type;
  }
  isAsciiDoc(): boolean {
    return PageType.ASCIIDOC === this.page?.type;
  }
  isAsyncApi(): boolean {
    return PageType.ASYNCAPI === this.page?.type;
  }

  initEditor() {
    this.$scope.editorReadonly = false;
    if (this.page.source != null && this.page.source.type != null) {
      this.resolvedFetchers.forEach((fetcher) => {
        if (fetcher.id === this.page.source.type) {
          this.$scope.fetcherJsonSchema = angular.fromJson(fetcher.schema);
          this.$scope.editorReadonly = true;
        }
      });
    }
  }

  removeFetcher() {
    this.page.source = null;
    this.$scope.fetcherJsonSchema = emptyFetcher;
  }

  save() {
    this.error = null;
    this.DocumentationService.update(this.page, this.apiId)
      .then((response) => {
        if (response.data.messages && response.data.messages.length > 0) {
          this.NotificationService.showError("'" + this.page.name + "' has been updated (with validation errors)");
        } else {
          this.NotificationService.show("'" + this.page.name + "' has been updated");
        }
        this.ngRouter.navigate(['../'], { relativeTo: this.activatedRoute });
      })
      .catch((err) => {
        this.error = { ...err.data, title: 'Sorry, unable to update page' };
      });
  }

  saveTranslation() {
    this.$scope.$broadcast('saveTranslation');
  }

  cancel() {
    this.ngRouter.navigate(['../'], { queryParams: { parent: this.page.parentId }, relativeTo: this.activatedRoute });
  }

  onAttachedResourceUpdate() {
    this.DocumentationService.getMedia(this.page.id, this.apiId)
      .then((mediaResponse) => (this.attachedResources = mediaResponse.data))
      .then(() => this.DocumentationService.get(this.apiId, this.page.id))
      .then((pageResponse) => (this.resolvedPage = pageResponse.data))
      .then(() => this.reset());
  }

  reset() {
    this.page = null;
    this.groups = null;
    this.$scope.acls = {
      groups: [],
      roles: [],
    };
    this.$onInit();
  }

  toggleRename() {
    this.$scope.rename = !this.$scope.rename;
    if (this.$scope.rename) {
      this.newName = this.page.name;
    }
  }

  rename() {
    this.DocumentationService.partialUpdate('name', this.newName, this.page.id, this.apiId).then(() => {
      this.NotificationService.show("'" + this.page.name + "' has been renamed to '" + this.newName + "'");
      this.page.name = this.newName;
      this.toggleRename();
    });
  }

  goToExternalSource() {
    this.selectedTab = 3;
  }

  selectTab(idx: number) {
    const selectedTab = this.tabs.findIndex((tab) => tab.id === idx);

    // Change tabs + query params if there is a change
    if (this.tabs[selectedTab].name !== this.currentTab) {
      this.selectedTab = selectedTab;
      this.currentTab = this.tabs[this.selectedTab].name;
      this.ngRouter.navigate(['../', this.page.id], {
        queryParams: { tab: this.currentTab, type: this.page.type },
        relativeTo: this.activatedRoute,
      });
    }
  }

  fetch() {
    this.DocumentationService.fetch(this.page.id, this.apiId).then(() => {
      this.NotificationService.show("'" + this.page.name + "' has been successfully fetched");
      this.reset();
    });
  }

  getBannerMessage(): string {
    return this.isMarkdownTemplate()
      ? 'This page is not available for users yet'
      : 'This page is not published yet and will not be visible to other users';
  }
}

EditPageComponentController.$inject = ['NotificationService', 'DocumentationService', 'UserService', '$scope', 'ngRouter'];

export const DocumentationEditPageComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
    resolvedPage: '<',
    resolvedGroups: '<',
    resolvedFetchers: '<',
    pagesToLink: '<',
    folders: '<',
    systemFolders: '<',
    pageResources: '<',
    categoryResources: '<',
    attachedResources: '<',
  },
  template: require('html-loader!./edit-page.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: EditPageComponentController,
};

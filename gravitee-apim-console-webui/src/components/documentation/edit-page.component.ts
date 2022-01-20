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

import { StateService } from '@uirouter/core';
import angular, { IController, IScope } from 'angular';
import * as _ from 'lodash';

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

  apiId: string;
  tabs: { id: number; name: string; isUnavailable: () => boolean }[];
  error: any;
  page: any;
  selectedTab: number;
  currentTab: string;
  groups: any[];
  foldersById: _.Dictionary<any>;
  systemFoldersById: _.Dictionary<any>;
  pageList: any[];
  canUpdate: boolean;
  newName: any;

  constructor(
    private readonly NotificationService: NotificationService,
    private readonly DocumentationService: DocumentationService,
    private readonly UserService: UserService,
    private readonly $mdDialog: angular.material.IDialogService,
    private $state: StateService,
    private $scope: IPageScope,
  ) {
    'ngInject';
    this.apiId = $state.params.apiId;
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
    this.page = this.resolvedPage;
    this.tabs = this.tabs.filter((tab) => !tab.isUnavailable());
    const indexOfTab = this.tabs.findIndex((tab) => tab.name === this.$state.params.tab);
    this.selectedTab = indexOfTab > -1 ? indexOfTab : 0;
    this.currentTab = this.tabs[this.selectedTab].name;
    if (this.resolvedPage.messages && this.resolvedPage.messages.length > 0) {
      this.error = {
        title: 'Validation messages',
        message: this.resolvedPage.messages,
      };
    }
    this.groups = this.resolvedGroups;

    this.foldersById = _.keyBy(this.folders, 'id');
    this.systemFoldersById = _.keyBy(this.systemFolders, 'id');
    const folderSituation = this.DocumentationService.getFolderSituation(this.systemFoldersById, this.foldersById, this.page.parentId);
    this.pageList = this.DocumentationService.buildPageList(this.pageResources, true, folderSituation);
    this.pagesToLink = this.DocumentationService.buildPageList(this.pagesToLink, false, folderSituation);
    if (this.DocumentationService.supportedTypes(folderSituation).indexOf(this.page.type) < 0) {
      this.$state.go('management.settings.documentation');
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
    return PageType.FOLDER === this.page.type;
  }
  isLink(): boolean {
    return PageType.LINK === this.page.type;
  }
  isSwagger(): boolean {
    return PageType.SWAGGER === this.page.type;
  }
  isMarkdown(): boolean {
    return PageType.MARKDOWN === this.page.type;
  }
  isMarkdownTemplate(): boolean {
    return PageType.MARKDOWN_TEMPLATE === this.page.type;
  }
  isAsciiDoc(): boolean {
    return PageType.ASCIIDOC === this.page.type;
  }
  isAsyncApi(): boolean {
    return PageType.ASYNCAPI === this.page.type;
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
          this.NotificationService.showError(
            "'" + this.page.name + "' has been updated (with validation errors - check the bottom of the page for details)",
          );
        } else {
          this.NotificationService.show("'" + this.page.name + "' has been updated");
        }
        if (this.apiId) {
          this.$state.go(
            'management.apis.detail.portal.editdocumentation',
            { pageId: this.page.id, tab: this.currentTab },
            { reload: true },
          );
        } else {
          this.$state.go(
            'management.settings.editdocumentation',
            { pageId: this.page.id, type: this.page.type, tab: this.currentTab },
            { reload: true },
          );
        }
      })
      .catch((err) => {
        this.error = { ...err.data, title: 'Sorry, unable to update page' };
      });
  }

  saveTranslation() {
    this.$scope.$broadcast('saveTranslation');
  }

  cancel() {
    if (this.apiId) {
      this.$state.go('management.apis.detail.portal.documentation', { apiId: this.apiId, parent: this.page.parentId });
    } else {
      this.$state.go('management.settings.documentation', { parent: this.page.parentId });
    }
  }

  reset() {
    if (this.apiId) {
      this.$state.go('management.apis.detail.portal.editdocumentation', { pageId: this.page.id }, { reload: true });
    } else {
      this.$state.go('management.settings.editdocumentation', { pageId: this.page.id, type: this.page.type }, { reload: true });
    }
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
    this.changeTab(idx);
    if (this.apiId) {
      this.$state.transitionTo(
        'management.apis.detail.portal.editdocumentation',
        { apiId: this.apiId, type: this.page.type, pageId: this.page.id, tab: this.currentTab },
        { notify: false },
      );
    } else {
      this.$state.transitionTo(
        'management.settings.editdocumentation',
        { pageId: this.page.id, type: this.page.type, tab: this.currentTab },
        { notify: false },
      );
    }
  }

  changeTab(idx: number) {
    this.selectedTab = this.tabs.findIndex((tab) => tab.id === idx);
    this.currentTab = this.tabs[this.selectedTab].name;
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

export const EditPageComponent: ng.IComponentOptions = {
  bindings: {
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
  template: require('./edit-page.html'),
  controller: EditPageComponentController,
};

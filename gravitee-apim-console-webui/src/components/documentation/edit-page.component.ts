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

import NotificationService from '../../services/notification.service';
import DocumentationService, { FolderSituation, SystemFolderName } from '../../services/documentation.service';
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';
import UserService from '../../services/user.service';
import _ = require('lodash');

interface IPageScope extends IScope {
  fetcherJsonSchema: string;
  rename: boolean;
  editorReadonly: boolean;
  currentTab: string;
  currentTranslation: any;
}
const EditPageComponent: ng.IComponentOptions = {
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
  controller: function (
    NotificationService: NotificationService,
    DocumentationService: DocumentationService,
    UserService: UserService,
    $mdDialog: angular.material.IDialogService,
    $state: StateService,
    $scope: IPageScope,
    $http: ng.IHttpService,
    Constants: any,
  ) {
    'ngInject';
    this.apiId = $state.params.apiId;
    this.tabs = ['content', 'translations', 'config', 'fetchers', 'access-control', 'attached-resources'];
    const indexOfTab = this.tabs.indexOf($state.params.tab);
    this.selectedTab = indexOfTab > -1 ? indexOfTab : 0;
    this.currentTab = this.tabs[this.selectedTab];
    this.shouldShowOpenApiDocFormat = false;

    this.error = null;
    $scope.rename = false;

    this.$onInit = () => {
      this.page = this.resolvedPage;
      if (this.resolvedPage.messages && this.resolvedPage.messages.length > 0) {
        this.error = {
          title: 'Validation messages',
          message: this.resolvedPage.messages,
        };
      }
      this.groups = this.resolvedGroups;
      this.fetchers = this.resolvedFetchers;

      this.foldersById = _.keyBy(this.folders, 'id');
      this.systemFoldersById = _.keyBy(this.systemFolders, 'id');
      this.pageList = this.buildPageList(this.pageResources, true);
      this.pagesToLink = this.buildPageList(this.pagesToLink);
      if (DocumentationService.supportedTypes(this.getFolderSituation(this.page.parentId)).indexOf(this.page.type) < 0) {
        $state.go('management.settings.documentation');
      }

      this.emptyFetcher = {
        type: 'object',
        id: 'empty',
        properties: { '': {} },
      };
      $scope.fetcherJsonSchema = this.emptyFetcher;
      this.fetcherJsonSchemaForm = ['*'];
      this.initEditor();

      this.codeMirrorOptions = {
        lineWrapping: true,
        lineNumbers: true,
        allowDropFileTypes: true,
        autoCloseTags: true,
        readOnly: $scope.editorReadonly,
        mode: 'javascript',
      };

      if (this.apiId) {
        this.canUpdate = UserService.isUserHasPermissions(['api-documentation-u']);
      } else {
        this.canUpdate = UserService.isUserHasPermissions(['environment-documentation-u']);
      }

      if (this.page.type === 'SWAGGER') {
        if (!this.page.configuration) {
          this.page.configuration = {};
        }
      }

      this.settings = Constants.env.settings;
      this.shouldShowOpenApiDocFormat =
        this.settings &&
        this.settings.openAPIDocViewer &&
        this.settings.openAPIDocViewer.openAPIDocType.swagger.enabled &&
        this.settings.openAPIDocViewer.openAPIDocType.redoc.enabled;

      if (this.page.type === 'SWAGGER' && !this.page.configuration.viewer) {
        if (this.settings && this.settings.openAPIDocViewer) {
          this.page.configuration.viewer = this.settings.openAPIDocViewer.openAPIDocType.defaultType;
        }
      }
    };

    this.usedAsGeneralConditions = () => {
      return this.page.generalConditions;
    };

    this.selectTranslation = (translation: any) => {
      this.currentTranslation = translation;
      if (!this.currentTranslation.configuration.inheritContent) {
        this.currentTranslation.configuration.inheritContent = 'true';
      }
    };

    this.addTranslation = () => {
      this.currentTranslation = {
        type: 'TRANSLATION',
        parentId: this.page.id,
        configuration: {},
      };
      if (this.page.type === 'MARKDOWN' || this.page.type === 'SWAGGER') {
        this.currentTranslation.configuration.inheritContent = 'true';
      }
    };

    this.saveTranslation = () => {
      if (
        this.page.configuration &&
        ('page' === this.page.configuration.resourceType || 'category' === this.page.configuration.resourceType)
      ) {
        this.currentTranslation.content = this.page.content;
      }
      // save translation
      if (!this.currentTranslation.id) {
        DocumentationService.create(this.currentTranslation, this.apiId).then((response: any) => {
          const page = response.data;
          NotificationService.show("'" + page.name + "' has been created");
          this.refreshTranslations();
        });
      } else {
        DocumentationService.update(this.currentTranslation, this.apiId).then((response: any) => {
          NotificationService.show("'" + this.currentTranslation.name + "' has been updated");
          this.refreshTranslations();
        });
      }
    };

    this.remove = (page: any) => {
      let that = this;
      $mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Would you like to remove "' + page.name + '"?',
            confirmButton: 'Remove',
          },
        })
        .then(function (response: any) {
          if (response) {
            DocumentationService.remove(page.id, that.apiId).then(() => {
              NotificationService.show('Translation ' + page.name + ' has been removed');
              that.refreshTranslations();
            });
          }
        });
    };

    this.refreshTranslations = () => {
      DocumentationService.get(this.apiId, this.page.id).then((response: any) => (this.page.translations = response.data.translations));
      delete this.currentTranslation;
    };

    this.getFolderSituation = (folderId: string) => {
      if (!folderId) {
        return FolderSituation.ROOT;
      }
      if (this.systemFoldersById[folderId]) {
        if (SystemFolderName.TOPFOOTER === this.systemFoldersById[folderId].name) {
          return FolderSituation.SYSTEM_FOLDER_WITH_FOLDERS;
        } else {
          return FolderSituation.SYSTEM_FOLDER;
        }
      }
      if (this.foldersById[folderId]) {
        const parentFolderId = this.foldersById[folderId].parentId;
        if (this.systemFoldersById[parentFolderId]) {
          return FolderSituation.FOLDER_IN_SYSTEM_FOLDER;
        }
        return FolderSituation.FOLDER_IN_FOLDER;
      }
      // tslint:disable-next-line:no-console
      console.debug('impossible to determine folder situation : ' + folderId);
    };

    this.buildPageList = (pagesToFilter: any[], withRootFolder?: boolean) => {
      let pageList = _.filter(
        pagesToFilter,
        (p) =>
          p.type === 'MARKDOWN' ||
          p.type === 'SWAGGER' ||
          (p.type === 'FOLDER' && this.getFolderSituation(p.id) !== FolderSituation.FOLDER_IN_SYSTEM_FOLDER),
      ).sort((a, b) => {
        let comparison = 0;
        const aFullPath = a.parentPath + '/' + a.name;
        const bFullPath = b.parentPath + '/' + b.name;
        if (aFullPath > bFullPath) {
          comparison = 1;
        } else if (aFullPath < bFullPath) {
          comparison = -1;
        }
        return comparison;
      });

      if (withRootFolder) {
        pageList.unshift({ id: 'root', name: '', type: 'FOLDER', fullPath: '' });
      }
      return pageList;
    };

    this.getFolder = (id: string) => {
      if (id) {
        let folder = this.foldersById[id];
        if (!folder) {
          folder = this.systemFoldersById[id];
        }
        return folder;
      }
    };

    this.getFolderPath = (parentFolderId: string) => {
      const parent = this.getFolder(parentFolderId);
      if (parent) {
        return this.getFolderPath(parent.parentId) + '/' + parent.name;
      } else {
        return '';
      }
    };

    this.initEditor = () => {
      $scope.editorReadonly = false;
      if (!(_.isNil(this.page.source) || _.isNil(this.page.source.type))) {
        _.forEach(this.fetchers, (fetcher) => {
          if (fetcher.id === this.page.source.type) {
            $scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
            $scope.editorReadonly = true;
          }
        });
      }
    };

    this.configureFetcher = (fetcher) => {
      if (!this.page.source) {
        this.page.source = {};
      }

      this.page.source = {
        type: fetcher.id,
        configuration: this.page?.source?.configuration || {},
      };
      $scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
    };

    this.removeFetcher = () => {
      this.page.source = null;
      $scope.fetcherJsonSchema = this.emptyFetcher;
    };

    this.checkIfFolder = () => {
      if (this.page.content) {
        if (this.page.content === 'root') {
          this.page.configuration.isFolder = true;
          this.page.configuration.inherit = 'false';
        } else {
          const folder = this.getFolder(this.page.content);
          if (folder) {
            this.page.configuration.isFolder = true;
          } else {
            this.page.configuration.isFolder = false;
          }
        }
      }
    };

    this.onChangeLinkType = () => {
      delete this.page.content;
      delete this.page.configuration.isFolder;
      if (this.page.configuration.resourceType === 'external') {
        delete this.page.configuration.inherit;
        if (this.page.translations) {
          _.forEach(this.page.translations, (t) => delete t.content);
        }
      } else if (!this.page.configuration.inherit) {
        this.page.configuration.inherit = 'true';
      }
    };

    this.save = () => {
      this.error = null;
      DocumentationService.update(this.page, this.apiId)
        .then((response) => {
          if (response.data.messages && response.data.messages.length > 0) {
            NotificationService.showError(
              "'" + this.page.name + "' has been updated (with validation errors - check the bottom of the page for details)",
            );
          } else {
            NotificationService.show("'" + this.page.name + "' has been updated");
          }
          if (this.apiId) {
            $state.go('management.apis.detail.portal.editdocumentation', { pageId: this.page.id, tab: this.currentTab }, { reload: true });
          } else {
            $state.go(
              'management.settings.editdocumentation',
              { pageId: this.page.id, type: this.page.type, tab: this.currentTab },
              { reload: true },
            );
          }
        })
        .catch((err) => {
          this.error = { ...err.data, title: 'Sorry, unable to update page' };
        });
    };

    this.changeContentMode = (newMode) => {
      if ('fetcher' === newMode) {
        this.page.source = {
          configuration: {},
        };
      } else {
        delete this.page.source;
      }
    };

    this.cancel = () => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.documentation', { apiId: this.apiId, parent: this.page.parentId });
      } else {
        $state.go('management.settings.documentation', { parent: this.page.parentId });
      }
    };

    this.reset = () => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.editdocumentation', { pageId: this.page.id }, { reload: true });
      } else {
        $state.go('management.settings.editdocumentation', { pageId: this.page.id, type: this.page.type }, { reload: true });
      }
    };

    this.toggleRename = () => {
      $scope.rename = !$scope.rename;
      if ($scope.rename) {
        this.newName = this.page.name;
      }
    };

    this.rename = () => {
      DocumentationService.partialUpdate('name', this.newName, this.page.id, this.apiId).then(() => {
        NotificationService.show("'" + this.page.name + "' has been renamed to '" + this.newName + "'");
        this.page.name = this.newName;
        this.toggleRename();
      });
    };

    this.goToExternalSource = () => {
      this.selectedTab = 2;
    };

    this.selectTab = (idx: number) => {
      this.changeTab(idx);
      if (this.apiId) {
        $state.transitionTo(
          'management.apis.detail.portal.editdocumentation',
          { apiId: this.apiId, type: this.page.type, pageId: this.page.id, tab: this.currentTab },
          { notify: false },
        );
      } else {
        $state.transitionTo(
          'management.settings.editdocumentation',
          { pageId: this.page.id, type: this.page.type, tab: this.currentTab },
          { notify: false },
        );
      }
    };

    this.changeTab = (idx: number) => {
      this.selectedTab = idx;
      this.currentTab = this.tabs[this.selectedTab];
    };

    this.fetch = () => {
      DocumentationService.fetch(this.page.id, this.apiId).then(() => {
        NotificationService.show("'" + this.page.name + "' has been successfully fetched");
        this.reset();
      });
    };

    this.toggleEntrypointAsServer = () => {
      if (this.page.configuration.entrypointsAsServers === undefined) {
        // Enable adding context-path automatically only the first time user decides to use entrypoint url.
        this.page.configuration.entrypointAsBasePath = 'true';
      }
    };

    this.updateLinkName = (resourceName: string) => {
      if (this.page.configuration.inherit === 'true' && resourceName !== '') {
        this.page.name = resourceName;
      }
    };

    this.updateLinkNameWithPageId = (resourceId: string) => {
      const relatedPage = _.find(this.pageList, (p) => p.id === resourceId);
      if (relatedPage) {
        this.updateLinkName(relatedPage.name);
      }
    };

    this.updateLinkNameWithCategoryId = (resourceId: string) => {
      const relatedCategory = _.find(this.categoryResources, (p) => p.id === resourceId);
      if (relatedCategory) {
        this.updateLinkName(relatedCategory.name);
      }
    };

    this.updateTranslationContent = () => {
      if (
        this.currentTranslation.configuration.inheritContent === 'false' &&
        (!this.currentTranslation.content || this.currentTranslation.content === '')
      ) {
        this.currentTranslation.content = this.page.content;
      }
      if (this.currentTranslation.configuration.inheritContent === 'true') {
        delete this.currentTranslation.content;
      }
    };

    this.openApiFormatLabel = (format) => {
      if (this.settings && this.settings.openAPIDocViewer && format === this.settings.openAPIDocViewer.openAPIDocType.defaultType) {
        return `${format} (Default)`;
      } else {
        return format;
      }
    };

    this.addAttachedResource = () => {
      let that = this;
      $mdDialog
        .show({
          controller: 'FileChooserDialogController',
          controllerAs: 'ctrl',
          template: require('../dialog/fileChooser.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Select a file to attach',
            confirmButton: 'Add',
          },
        })
        .then(function (response: any) {
          if (response.file) {
            // upload new media to portal or api
            let fd = new FormData();
            let fileName = response.file.name;
            if (response.filename) {
              fileName = response.filename;
            }
            fd.append('file', response.file);
            fd.append('fileName', fileName);

            DocumentationService.addMedia(fd, that.page.id, that.apiId)
              .then((response) => that.reset())
              .then(() => NotificationService.show(fileName + ' has been attached'));
          }
        });
    };

    this.removeAttachedResource = (resource: any) => {
      let that = this;
      $mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Would you like to remove "' + resource.fileName + '"?',
            confirmButton: 'Remove',
          },
        })
        .then(function (response) {
          if (response) {
            that.page.attached_media = that.page.attached_media.filter(
              (media) =>
                !(media.mediaHash === resource.hash && media.mediaName === resource.fileName && media.attachedAt === resource.createAt),
            );
            DocumentationService.update(that.page, that.apiId)
              .then((response) => that.reset())
              .then(() => NotificationService.show(resource.fileName + ' has been removed from page'));
          }
        });
    };
  },
};

export default EditPageComponent;

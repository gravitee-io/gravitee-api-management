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
import DocumentationService, { FolderSituation } from '../../services/documentation.service';
import {StateService} from '@uirouter/core';
import {IScope} from 'angular';
import _ = require('lodash');
import UserService from '../../services/user.service';

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
    folders: '<',
    systemFolders: '<',
    pageResources: '<',
    viewResources: '<'
  },
  template: require('./edit-page.html'),
  controller: function (
    NotificationService: NotificationService,
    DocumentationService: DocumentationService,
    UserService: UserService,
    $mdDialog: angular.material.IDialogService,
    $state: StateService,
    $scope: IPageScope
  ) {
    'ngInject';
    this.apiId = $state.params.apiId;
    this.tabs = ['content', 'translations', 'config', 'fetchers', 'access-control'];
    const indexOfTab = this.tabs.indexOf($state.params.tab);
    this.selectedTab = indexOfTab > -1 ? indexOfTab : 0;
    this.currentTab = this.tabs[this.selectedTab];

    $scope.rename = false;

    this.$onInit = () => {
      this.page = this.resolvedPage;
      this.groups = this.resolvedGroups;
      this.fetchers = this.resolvedFetchers;

      this.foldersById = _.keyBy(this.folders, 'id');
      this.systemFoldersById = _.keyBy(this.systemFolders, 'id');
      this.pageList = this.buildPageList(this.pageResources);
      this.viewResources = _.filter(this.viewResources, (v) => v.id !== 'all');

      if ( DocumentationService.supportedTypes(this.getFolderSituation(this.page.parentId)).indexOf(this.page.type) < 0) {
        $state.go('management.settings.documentation');
      }

      this.emptyFetcher = {
        'type': 'object',
        'id': 'empty',
        'properties': {'' : {}}
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

      if (this.page.excluded_groups) {
        this.page.authorizedGroups = _.difference(_.map(this.groups, 'id'), this.page.excluded_groups);
      } else {
        this.page.authorizedGroups = _.map(this.groups, 'id');
      }
      if (this.apiId) {
        this.canUpdate = UserService.isUserHasPermissions(['api-documentation-u']);
      } else {
        this.canUpdate = UserService.isUserHasPermissions(['environment-documentation-u']);
      }

      if (this.page.type === 'SWAGGER') {
        if (!this.page.configuration) {
          this.page.configuration = {};
        }
        if (!this.page.configuration.viewer) {
          this.page.configuration.viewer = 'Swagger';
        }
      }
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
        configuration: {}
      };
      if (this.page.type === 'MARKDOWN' || this.page.type === 'SWAGGER') {
        this.currentTranslation.configuration.inheritContent = 'true';
      }
    };

    this.saveTranslation = () => {
      if (this.page.configuration && ('page' === this.page.configuration.resourceType || 'view' === this.page.configuration.resourceType)) {
        this.currentTranslation.content = this.page.content;
      }
      // save translation
      if (!this.currentTranslation.id) {
        DocumentationService.create(this.currentTranslation, this.apiId)
        .then((response: any) => {
          const page = response.data;
          NotificationService.show('\'' + page.name + '\' has been created');
          this.refreshTranslations();
        });
      } else {
        DocumentationService.update(this.currentTranslation, this.apiId)
          .then( (response: any) => {
            NotificationService.show('\'' + this.currentTranslation.name + '\' has been updated');
            this.refreshTranslations();
          });
        }
    };

    this.remove = (page: any) => {
      let that = this;
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to remove "' + page.name + '" ?',
          confirmButton: 'Remove'
        }
      }).then(function (response: any) {
        if (response) {
          DocumentationService.remove(page.id, that.apiId).then( () => {
            NotificationService.show('Translation ' + page.name + ' has been removed');
            that.refreshTranslations();
          });
        }
      });
    };

    this.refreshTranslations = () => {
      DocumentationService.get(this.apiId, this.page.id).then((response: any) => this.page.translations = response.data.translations);
      delete this.currentTranslation;
    };

    this.getFolderSituation = (folderId: string) => {
      if (!folderId) {
        return FolderSituation.ROOT;
      }
      if (this.systemFoldersById[folderId]) {
        return FolderSituation.SYSTEM_FOLDER;
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

    this.buildPageList = (pagesToFilter: any[]) => {
      let pageList = _
        .filter(pagesToFilter, (p) => p.type === 'MARKDOWN' || p.type === 'SWAGGER' || (p.type === 'FOLDER' && this.getFolderSituation(p.id) !== FolderSituation.FOLDER_IN_SYSTEM_FOLDER))
        .map((page) => { return {
          id: page.id,
          name: page.name,
          type: page.type,
          fullPath: this.getFolderPath(page.parentId)
        };
      }).sort((a, b) => {
        let comparison = 0;
        if (a.fullPath > b.fullPath) {
          comparison = 1;
        } else if (a.fullPath < b.fullPath) {
          comparison = -1;
        }
        return comparison;
      });

      pageList.unshift( {id: 'root', name: '', type: 'FOLDER', fullPath: ''});
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
        _.forEach(this.fetchers, fetcher => {
          if (fetcher.id === this.page.source.type) {
            $scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
            $scope.editorReadonly = true;
          }
        });
      }
    };

    this.configureFetcher = (fetcher) => {
      if (! this.page.source) {
        this.page.source = {};
      }

      this.page.source = {
        type: fetcher.id,
        configuration: {}
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
          _.forEach(this.page.translations, t => delete t.content);
        }
      } else if (!this.page.configuration.inherit) {
        this.page.configuration.inherit = 'true';
      }
    };

    this.save = () => {
      // convert authorized groups to excludedGroups
      this.page.excluded_groups = [];
      if (this.groups) {
        this.page.excluded_groups = _.difference(_.map(this.groups, 'id'), this.page.authorizedGroups);
      }

      DocumentationService.update(this.page, this.apiId)
        .then( (response) => {
          NotificationService.show('\'' + this.page.name + '\' has been updated');
          if (this.apiId) {
            $state.go('management.apis.detail.portal.editdocumentation', {pageId: this.page.id, tab: this.currentTab}, {reload: true});
          } else {
            $state.go('management.settings.editdocumentation', {pageId: this.page.id, type: this.page.type, tab: this.currentTab}, {reload: true});
          }
      });
    };

    this.changeContentMode = (newMode) => {
      if ('fetcher' === newMode) {
        this.page.source = {
          configuration: {}
        };
      } else {
        delete this.page.source;
      }
    };

    this.cancel = () => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.documentation', {apiId: this.apiId, parent: this.page.parentId});
      } else {
        $state.go('management.settings.documentation', {parent: this.page.parentId});
      }
    };

    this.reset = () => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.editdocumentation', {pageId: this.page.id}, {reload: true});
      } else {
        $state.go('management.settings.editdocumentation', {pageId: this.page.id, type: this.page.type}, {reload: true});
      }
    };

    this.toggleRename = () => {
      $scope.rename = !$scope.rename;
      if ($scope.rename) {
        this.newName = this.page.name;
      }
    };

    this.rename = () => {
      DocumentationService.partialUpdate('name', this.newName, this.page.id, this.apiId).then( () => {
        NotificationService.show('\'' + this.page.name + '\' has been renamed to \'' + this.newName + '\'');
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
        $state.transitionTo('management.apis.detail.portal.editdocumentation', {apiId: this.apiId, type: this.page.type, pageId: this.page.id, tab: this.currentTab}, {notify: false});
      } else {
        $state.transitionTo('management.settings.editdocumentation', {pageId: this.page.id, type: this.page.type, tab: this.currentTab}, {notify: false});
      }
    };

    this.changeTab = (idx: number) => {
      this.selectedTab = idx;
      this.currentTab = this.tabs[this.selectedTab];
    };

    this.fetch = () => {
      DocumentationService.fetch(this.page.id, this.apiId).then( () => {
        NotificationService.show('\'' + this.page.name + '\' has been successfully fetched');
        this.reset();
      });
    };

    this.updateLinkName = (resourceName: string) => {
      if (this.page.configuration.inherit === 'true' && resourceName !== '') {
        this.page.name = resourceName;
      }
    };

    this.updateLinkNameWithPageId = (resourceId: string) => {
      const relatedPage = _.find(this.pageList, p => p.id === resourceId);
      if (relatedPage) {
        this.updateLinkName(relatedPage.name);
      }
    };

    this.updateLinkNameWithViewId = (resourceId: string) => {
      const relatedView = _.find(this.viewResources, p => p.id === resourceId);
      if (relatedView) {
        this.updateLinkName(relatedView.name);
      }
    };

    this.updateTranslationContent = () => {
      if ( this.currentTranslation.configuration.inheritContent === 'false' && (!this.currentTranslation.content || this.currentTranslation.content === '')) {
        this.currentTranslation.content = this.page.content;
      }
      if ( this.currentTranslation.configuration.inheritContent === 'true') {
        delete this.currentTranslation.content;
      }
    };
  },
};

export default EditPageComponent;

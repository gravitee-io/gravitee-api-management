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
import DocumentationService, { DocumentationQuery, FolderSituation, SystemFolderName } from '../../services/documentation.service';
import {StateService} from '@uirouter/core';
import _ = require('lodash');
import {IScope} from 'angular';
import CategoryService from '../../services/category.service';

interface IPageScope extends IScope {
  getContentMode: string;
  fetcherJsonSchema: string;
}
const NewPageComponent: ng.IComponentOptions = {
  bindings: {
    resolvedFetchers: '<',
    folders: '<',
    systemFolders: '<',
    pageResources: '<',
    categoryResources: '<'
  },
  template: require('./new-page.html'),
  controller: function (
    NotificationService: NotificationService,
    DocumentationService: DocumentationService,
    $state: StateService,
    $scope: IPageScope
  ) {
    'ngInject';
    this.apiId = $state.params.apiId;

    this.page = {
      name: '',
      type: $state.params.type,
      parentId: $state.params.parent
    };

    $scope.getContentMode = 'inline';

    this.codeMirrorOptions = {
      lineWrapping: true,
      lineNumbers: true,
      allowDropFileTypes: true,
      autoCloseTags: true,
      mode: 'javascript'
    };


    this.$onInit = () => {
      this.foldersById = _.keyBy(this.folders, 'id');
      this.systemFoldersById = _.keyBy(this.systemFolders, 'id');
      this.pageList = this.buildPageList(this.pageResources);

      this.fetchers = this.resolvedFetchers;
      if (DocumentationService.supportedTypes(this.getFolderSituation(this.page.parentId)).indexOf(this.page.type) < 0) {
        $state.go('management.settings.documentation', {parent: $state.params.parent});
      }

      this.emptyFetcher = {
        'type': 'object',
        'id': 'empty',
        'properties': {'' : {}}
      };
      $scope.fetcherJsonSchema = this.emptyFetcher;
      this.fetcherJsonSchemaForm = ['*'];
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
      if (this.page.configuration.resourceType !== 'external' && !this.page.configuration.inherit) {
        this.page.configuration.inherit = 'true';
      }
    };

    this.save = () => {
      DocumentationService.create(this.page, this.apiId)
        .then( (response: any) => {
          const page = response.data;
          NotificationService.show('\'' + page.name + '\' has been created');
          if (page.type === 'FOLDER') {
            this.gotoParent();
          } else {
            this.gotoEdit(page);
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
      this.gotoParent();
    };

    this.gotoParent = () => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.documentation', {apiId: this.apiId, parent: $state.params.parent});
      } else {
        $state.go('management.settings.documentation', {parent: $state.params.parent});
      }
    };

    this.gotoEdit = (page) => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.editdocumentation', {apiId: this.apiId, pageId: page.id, type: page.type});
      } else {
        $state.go('management.settings.editdocumentation', {pageId: page.id, type: page.type});
      }
    };

    this.updateLinkName = (resourceName: string) => {
      if (!this.page.name || this.page.name === '' || this.page.configuration.inherit === 'true' || resourceName === '') {
        this.page.name = resourceName;
      }
    };
  }
};

export default NewPageComponent;

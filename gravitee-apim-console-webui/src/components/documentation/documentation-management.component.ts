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
import { IController, IScope } from 'angular';
import * as _ from 'lodash';

import { DocumentationQuery, DocumentationService, FolderSituation, PageType } from '../../services/documentation.service';
import NotificationService from '../../services/notification.service';

interface IDocumentationManagementScope extends IScope {
  renameFolder: boolean;
  translateFolder: boolean;
}

class DocumentationManagementComponentController implements IController {
  pages: any[];
  folders: any[];
  systemFolders: any[];

  apiId: string;
  rootDir: string;
  foldersById: _.Dictionary<any>;
  systemFoldersById: _.Dictionary<any>;
  currentFolder: any;
  supportedTypes: { type: PageType; tooltip: string }[];
  breadcrumb: { id: string; name: string }[];
  newFolderName: string;
  currentTranslation: any;
  fetchAllInProgress: boolean;
  constructor(
    private readonly NotificationService: NotificationService,
    private readonly DocumentationService: DocumentationService,
    private $state: StateService,
    private $scope: IDocumentationManagementScope,
    private readonly $mdDialog: angular.material.IDialogService,
  ) {
    'ngInject';
    this.apiId = $state.params.apiId;
  }

  $onInit() {
    // remove the ROOT page
    this.pages = this.filterROOTAndSystemPages(this.pages);

    this.rootDir = this.$state.params.parent;
    this.foldersById = _.keyBy(this.folders, 'id');
    this.systemFoldersById = _.keyBy(this.systemFolders, 'id');

    this.currentFolder = this.getFolder(this.rootDir);
    const folderSituation = this.DocumentationService.getFolderSituation(this.systemFoldersById, this.foldersById, this.rootDir);
    this.supportedTypes = this.DocumentationService.supportedTypes(folderSituation)
      .filter((type) => !this.apiId || type !== PageType.MARKDOWN_TEMPLATE)
      .map((type) => ({
        type,
        tooltip: type.replace('_', ' '),
      }));
    this.breadcrumb = this.generateBreadcrumb();
    this.$scope.renameFolder = false;
    this.$scope.translateFolder = false;
  }

  isFolder(type: string): boolean {
    return PageType.FOLDER === type;
  }
  isLink(type: string): boolean {
    return PageType.LINK === type;
  }
  isSwagger(type: string): boolean {
    return PageType.SWAGGER === type;
  }
  isMarkdown(type: string): boolean {
    return PageType.MARKDOWN === type;
  }
  isPage(type: string): boolean {
    return this.isMarkdown(type) || this.isSwagger(type);
  }
  isMarkdownTemplate(type: string): boolean {
    return PageType.MARKDOWN_TEMPLATE === type;
  }

  canCreateShortCut(pageId: string, pageType: string) {
    return (
      pageType === 'ASCIIDOC' ||
      pageType === 'ASYNCAPI' ||
      pageType === 'SWAGGER' ||
      pageType === 'MARKDOWN' ||
      (pageType === 'FOLDER' &&
        this.DocumentationService.getFolderSituation(this.systemFoldersById, this.foldersById, pageId) !==
          FolderSituation.FOLDER_IN_SYSTEM_FOLDER)
    );
  }

  filterROOTAndSystemPages(pagesToFilter: any[]) {
    return _.filter(pagesToFilter, (p) => p.type !== 'ROOT' && p.type !== 'SYSTEM_FOLDER' && p.type !== 'TRANSLATION');
  }

  toggleRenameFolder() {
    this.$scope.renameFolder = !this.$scope.renameFolder;
    if (this.$scope.renameFolder) {
      this.newFolderName = this.breadcrumb[this.breadcrumb.length - 1].name;
    }
  }

  renameFolder() {
    this.DocumentationService.partialUpdate('name', this.newFolderName, this.rootDir, this.apiId).then((response) => {
      this.NotificationService.show('Folder ' + this.newFolderName + ' has been changed with success');
      this.breadcrumb[this.breadcrumb.length - 1].name = response.data.name;
      this.toggleRenameFolder();
    });
  }

  changeFolderVisibility() {
    this.DocumentationService.partialUpdate(
      'visibility',
      this.currentFolder.visibility === 'PRIVATE' ? 'PUBLIC' : 'PRIVATE',
      this.rootDir,
      this.apiId,
    ).then((response) => {
      this.NotificationService.show(`Folder is now ${response.data.visibility}`);
      this.currentFolder.visibility = response.data.visibility;
    });
  }

  generateBreadcrumb(): { id: string; name: string }[] {
    const result: { id: string; name: string }[] = [];
    if (this.rootDir) {
      this.addParentToBreadcrumb(this.rootDir, result);
    }
    result.push({ id: '', name: '~' });
    return result.reverse();
  }

  getFolder(id: string) {
    if (id) {
      let folder = this.foldersById[id];
      if (!folder) {
        folder = this.systemFoldersById[id];
      }
      return folder;
    }
  }

  createShortCut(page: any) {
    this.$mdDialog
      .show({
        controller: 'SelectFolderDialogController',
        controllerAs: 'ctrl',
        template: require('./dialog/selectfolder.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Create shortcut for "' + page.name + '" in...',
          folders: this.generateCreateShortCutFolder(),
        },
      })
      .then((destinationId) => {
        if (destinationId) {
          const newLink = {
            name: page.name,
            content: page.id,
            parentId: destinationId,
            type: 'LINK',
            published: page.published,
            visibility: page.visibility,
            configuration: {
              resourceType: 'page',
              isFolder: page.type === 'FOLDER',
              inherit: 'true',
            },
          };
          this.DocumentationService.create(newLink, this.apiId).then(() => {
            this.NotificationService.show('"Link to ' + page.name + '" has been created with success');
            this.refresh();
          });
        }
      });
  }

  generateCreateShortCutFolder() {
    const result = [];
    if (!this.folders && !this.systemFolders) {
      return result;
    }

    const allFolders = _.concat(this.folders, this.systemFolders);

    allFolders.forEach((f) => {
      const situation = this.DocumentationService.getFolderSituation(this.systemFoldersById, this.foldersById, f.id);
      if (
        situation === FolderSituation.SYSTEM_FOLDER ||
        situation === FolderSituation.SYSTEM_FOLDER_WITH_FOLDERS ||
        situation === FolderSituation.FOLDER_IN_SYSTEM_FOLDER
      ) {
        const path = this.getFolderPath(f.id);
        if (path) {
          result.push({
            id: f.id,
            path: path,
          });
        }
      }
    });
    return _.orderBy(result, ['path'], ['asc']);
  }

  moveToFolder(page: any) {
    this.$mdDialog
      .show({
        controller: 'SelectFolderDialogController',
        controllerAs: 'ctrl',
        template: require('./dialog/selectfolder.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Move "' + page.name + '" to...',
          folders: this.generateMoveToFolder(page.id, page.type),
        },
      })
      .then((destinationId) => {
        if (destinationId) {
          this.DocumentationService.partialUpdate('parentId', destinationId === -1 ? '' : destinationId, page.id, this.apiId).then(() => {
            this.NotificationService.show('"' + page.name + '" has been moved with success');
            this.refresh();
          });
        }
      });
  }

  generateMoveToFolder(pageId: string, pageType: string) {
    const result = [];
    if (!this.folders && !this.systemFolders) {
      return result;
    }

    const allFolders = _.concat(this.folders, this.systemFolders);

    // If it can ba a link, it can't be moved in a system folder. If not, it can only be moved inside a system folder
    const canBeALink = this.canCreateShortCut(pageId, pageType);

    if (canBeALink) {
      result.push({
        id: -1,
        path: '/',
      });
    }

    allFolders.forEach((f) => {
      const situation = this.DocumentationService.getFolderSituation(this.systemFoldersById, this.foldersById, f.id);
      if (
        (canBeALink && (situation === FolderSituation.ROOT || situation === FolderSituation.FOLDER_IN_FOLDER)) ||
        (!canBeALink &&
          ((pageType === 'FOLDER' && situation === FolderSituation.SYSTEM_FOLDER_WITH_FOLDERS) ||
            (pageType !== 'FOLDER' &&
              (situation === FolderSituation.SYSTEM_FOLDER ||
                situation === FolderSituation.SYSTEM_FOLDER_WITH_FOLDERS ||
                situation === FolderSituation.FOLDER_IN_SYSTEM_FOLDER))))
      ) {
        const path = this.getFolderPath(f.id, pageId);
        if (path) {
          result.push({
            id: f.id,
            path: path,
          });
        }
      }
    });
    return _.orderBy(result, ['path'], ['asc']);
  }

  getFolderPath(folderId: string, pageToMoveId?: string) {
    const hierarchyNames = [];
    const folder = this.getFolder(folderId);
    hierarchyNames.push(folder.name);
    this.getFolderParentName(folderId, hierarchyNames, pageToMoveId);
    if (hierarchyNames.length === 0) {
      return;
    }
    return (
      '/ ' +
      _.reduceRight(hierarchyNames, (path, name) => {
        return path + ' / ' + name;
      })
    );
  }

  getFolderParentName(folderId: string, names: string[], pageToMoveId: string) {
    const folder = this.getFolder(folderId);

    // do not move a folder to itself
    if (folderId === pageToMoveId || (folder.parentId && pageToMoveId === folder.parentId)) {
      names.length = 0;
      return;
    }

    if (folder.parentId) {
      const parentFolder = this.getFolder(folder.parentId);
      if (parentFolder) {
        names.push(parentFolder.name);
        this.getFolderParentName(folder.parentId, names, pageToMoveId);
      }
    }
  }

  addParentToBreadcrumb(id: string, breadcrumb: any[]) {
    const folder = this.getFolder(id);
    if (folder) {
      breadcrumb.push(folder);
      if (folder.parentId) {
        this.addParentToBreadcrumb(folder.parentId, breadcrumb);
      }
    }
  }

  refresh() {
    const q = new DocumentationQuery();
    if (this.rootDir) {
      q.parent = this.rootDir;
    } else {
      q.root = true;
    }
    this.DocumentationService.search(q, this.apiId).then((response) => (this.pages = this.filterROOTAndSystemPages(response.data)));
  }

  refreshCurrentFolder() {
    if (this.rootDir) {
      this.DocumentationService.get(this.apiId, this.rootDir).then((response) => (this.currentFolder = response.data));
      delete this.currentTranslation;
    }
  }

  togglePublish(page: any) {
    if (page.generalConditions) {
      this.NotificationService.showError('Page ' + page.name + ' is used as general conditions');
    } else {
      this.DocumentationService.partialUpdate('published', !page.published, page.id, this.apiId).then(() => {
        page.published = !page.published;
        const message = this.isMarkdownTemplate(page.type)
          ? 'Template ' + page.name + ' has been made ' + (page.published ? '' : 'un') + 'available with success'
          : 'Page ' + page.name + ' has been ' + (page.published ? '' : 'un') + 'published with success';
        this.NotificationService.show(message);
      });
    }
  }

  upward(page: any) {
    page.order = page.order - 1;
    this.DocumentationService.partialUpdate('order', page.order, page.id, this.apiId).then(() => {
      this.NotificationService.show('Page ' + page.name + ' order has been changed with success');
      this.refresh();
    });
  }

  downward(page: any) {
    page.order = page.order + 1;
    this.DocumentationService.partialUpdate('order', page.order, page.id, this.apiId).then(() => {
      this.NotificationService.show('Page ' + page.name + ' order has been changed with success');
      this.refresh();
    });
  }

  remove(page: any) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to remove "' + page.name + '"?',
          msg: page.type !== 'LINK' ? 'All related links will also be removed.' : '',
          confirmButton: 'Remove',
        },
      })
      .then((response) => {
        if (response) {
          this.DocumentationService.remove(page.id, this.apiId).then(() => {
            this.NotificationService.show('Page ' + page.name + ' has been removed');
            this.refresh();
            this.refreshCurrentFolder();
            if (this.currentTranslation.id === page.id) {
              delete this.currentTranslation;
            }
          });
        }
      });
  }

  newPage(type: string) {
    if (this.apiId) {
      this.$state.go('management.apis.detail.portal.newdocumentation', { type: type, parent: this.rootDir });
    } else {
      this.$state.go('management.settings.newdocumentation', { type: type, parent: this.rootDir });
    }
  }

  openUrl(page: any) {
    if ('FOLDER' === page.type || 'SYSTEM_FOLDER' === page.type) {
      if (this.apiId) {
        return this.$state.go('management.apis.detail.portal.documentation', { apiId: this.apiId, type: page.type, parent: page.id });
      } else {
        return this.$state.go('management.settings.documentation', { parent: page.id });
      }
    } else {
      if (this.apiId) {
        return this.$state.go('management.apis.detail.portal.editdocumentation', { apiId: this.apiId, type: page.type, pageId: page.id });
      } else {
        return this.$state.go('management.settings.editdocumentation', { pageId: page.id, type: page.type, tab: 'content' });
      }
    }
  }

  importPages() {
    if (this.apiId) {
      this.$state.go('management.apis.detail.portal.importdocumentation', { apiId: this.apiId });
    } else {
      this.$state.go('management.settings.importdocumentation');
    }
  }

  fetch() {
    this.fetchAllInProgress = true;
    this.DocumentationService.fetchAll(this.apiId)
      .then(() => {
        this.refresh();
        this.NotificationService.show('Pages has been successfully fetched');
      })
      .finally(() => {
        this.fetchAllInProgress = false;
      });
  }

  hasExternalDoc() {
    const externalPages = this.pages.filter((page) => Object.prototype.hasOwnProperty.call(page, 'source'));
    return externalPages.length > 0;
  }

  toggleTranslateFolder() {
    this.$scope.translateFolder = !this.$scope.translateFolder;
  }

  saveFolderTranslation() {
    if (!this.currentTranslation.id) {
      this.DocumentationService.create(this.currentTranslation, this.apiId).then((response: any) => {
        const page = response.data;
        this.NotificationService.show("'" + page.name + "' has been created");
        this.refreshCurrentFolder();
      });
    } else {
      this.DocumentationService.update(this.currentTranslation, this.apiId).then(() => {
        this.NotificationService.show("'" + this.currentTranslation.name + "' has been updated");
        this.refreshCurrentFolder();
      });
    }
  }

  selectTranslation(translation: any) {
    this.currentTranslation = translation;
  }

  addTranslation() {
    this.currentTranslation = {
      type: 'TRANSLATION',
      parentId: this.currentFolder.id,
    };
  }
}

export const DocumentationManagementComponent: ng.IComponentOptions = {
  bindings: {
    pages: '<',
    folders: '<',
    systemFolders: '<',
  },
  template: require('./documentation-management.html'),
  controller: DocumentationManagementComponentController,
};

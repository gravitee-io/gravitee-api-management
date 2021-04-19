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
import { StateService } from '@uirouter/core';
import _ = require('lodash');
import { IScope } from 'angular';

interface IDocumentationManagementScope extends IScope {
  renameFolder: boolean;
  translateFolder: boolean;
}

const DocumentationManagementComponent: ng.IComponentOptions = {
  bindings: {
    pages: '<',
    folders: '<',
    systemFolders: '<',
  },
  template: require('./documentation-management.html'),
  controller: function (
    NotificationService: NotificationService,
    DocumentationService: DocumentationService,
    $state: StateService,
    $scope: IDocumentationManagementScope,
    $mdDialog: angular.material.IDialogService,
    $rootScope: IScope,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.apiId = $state.params.apiId;

    this.$onInit = () => {
      // remove the ROOT page
      this.pages = this.filterROOTAndSystemPages(this.pages);

      this.rootDir = $state.params.parent;
      this.foldersById = _.keyBy(this.folders, 'id');
      this.systemFoldersById = _.keyBy(this.systemFolders, 'id');

      this.currentFolder = this.getFolder(this.rootDir);
      this.supportedTypes = DocumentationService.supportedTypes(this.getFolderSituation(this.rootDir));
      this.breadcrumb = this.generateBreadcrumb();
      $scope.renameFolder = false;
      $scope.translateFolder = false;
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

    this.canCreateShortCut = (pageId: string, pageType: string) => {
      return (
        pageType === 'SWAGGER' ||
        pageType === 'MARKDOWN' ||
        (pageType === 'FOLDER' && this.getFolderSituation(pageId) !== FolderSituation.FOLDER_IN_SYSTEM_FOLDER)
      );
    };

    this.filterROOTAndSystemPages = (pagesToFilter: any[]) => {
      return _.filter(pagesToFilter, (p) => p.type !== 'ROOT' && p.type !== 'SYSTEM_FOLDER' && p.type !== 'TRANSLATION');
    };

    this.toggleRenameFolder = () => {
      $scope.renameFolder = !$scope.renameFolder;
      if ($scope.renameFolder) {
        this.newFolderName = this.breadcrumb[this.breadcrumb.length - 1].name;
      }
    };

    this.renameFolder = () => {
      DocumentationService.partialUpdate('name', this.newFolderName, this.rootDir, this.apiId).then((response) => {
        NotificationService.show('Folder ' + this.newFolderName + ' has been changed with success');
        this.breadcrumb[this.breadcrumb.length - 1].name = response.data.name;
        this.toggleRenameFolder();
      });
    };

    this.generateBreadcrumb = () => {
      let result = [];
      if (this.rootDir) {
        this.addParentToBreadcrumb(this.rootDir, result);
      }
      result.push({ id: '', name: '~' });
      return result.reverse();
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

    this.createShortCut = (page: any) => {
      $mdDialog
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
              configuration: {
                resourceType: 'page',
                isFolder: page.type === 'FOLDER',
                inherit: 'true',
              },
            };
            DocumentationService.create(newLink, this.apiId).then(() => {
              NotificationService.show('"Link to ' + page.name + '" has been created with success');
              this.refresh();
            });
          }
        });
    };

    this.generateCreateShortCutFolder = () => {
      let result = [];
      if (this.folders || this.systemFolders) {
        const allFolders = _.concat(this.folders, this.systemFolders);
        _.forEach(allFolders, (f) => {
          const situation = this.getFolderSituation(f.id);
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
      return result;
    };

    this.moveToFolder = (page: any) => {
      $mdDialog
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
            DocumentationService.partialUpdate('parentId', destinationId === -1 ? '' : destinationId, page.id, this.apiId).then(() => {
              NotificationService.show('"' + page.name + '" has been moved with success');
              this.refresh();
            });
          }
        });
    };

    this.generateMoveToFolder = (pageId: string, pageType: string) => {
      let result = [];
      if (this.folders || this.systemFolders) {
        const allFolders = _.concat(this.folders, this.systemFolders);

        // If it can ba a link, it can't be moved in a system folder. If not, it can only be moved inside a system folder
        const canBeALink = this.canCreateShortCut(pageId, pageType);

        if (canBeALink) {
          result.push({
            id: -1,
            path: '/',
          });
        }

        _.forEach(allFolders, (f) => {
          const situation = this.getFolderSituation(f.id);
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
      return result;
    };

    this.getFolderPath = (folderId: string, pageToMoveId: string) => {
      let hierarchyNames = [];
      let folder = this.getFolder(folderId);
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
    };

    this.getFolderParentName = (folderId: string, names: string[], pageToMoveId: string) => {
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
    };

    this.addParentToBreadcrumb = (id: string, breadcrumb: any[]) => {
      let folder = this.getFolder(id);
      if (folder) {
        breadcrumb.push(folder);
        if (folder.parentId) {
          this.addParentToBreadcrumb(folder.parentId, breadcrumb);
        }
      }
    };

    this.refresh = () => {
      const q = new DocumentationQuery();
      if (this.rootDir) {
        q.parent = this.rootDir;
      } else {
        q.root = true;
      }
      DocumentationService.search(q, this.apiId).then((response) => (this.pages = this.filterROOTAndSystemPages(response.data)));
    };

    this.refreshCurrentFolder = () => {
      if (this.rootDir) {
        DocumentationService.get(this.apiId, this.rootDir).then((response) => (this.currentFolder = response.data));
        delete this.currentTranslation;
      }
    };

    this.togglePublish = (page: any) => {
      if (page.generalConditions) {
        NotificationService.showError('Page ' + page.name + ' is used as general conditions');
      } else {
        DocumentationService.partialUpdate('published', !page.published, page.id, this.apiId).then(() => {
          page.published = !page.published;
          NotificationService.show('Page ' + page.name + ' has been ' + (page.published ? '' : 'un') + 'published with success');
        });
      }
    };

    this.upward = (page: any) => {
      page.order = page.order - 1;
      DocumentationService.partialUpdate('order', page.order, page.id, this.apiId).then(() => {
        NotificationService.show('Page ' + page.name + ' order has been changed with success');
        this.refresh();
      });
    };

    this.downward = (page: any) => {
      page.order = page.order + 1;
      DocumentationService.partialUpdate('order', page.order, page.id, this.apiId).then(() => {
        NotificationService.show('Page ' + page.name + ' order has been changed with success');
        this.refresh();
      });
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
            msg: page.type !== 'LINK' ? 'All related links will also be removed.' : '',
            confirmButton: 'Remove',
          },
        })
        .then(function (response) {
          if (response) {
            DocumentationService.remove(page.id, that.apiId).then(() => {
              NotificationService.show('Page ' + page.name + ' has been removed');
              that.refresh();
              that.refreshCurrentFolder();
              if (that.currentTranslation.id === page.id) {
                delete that.currentTranslation;
              }
            });
          }
        });
    };

    this.newPage = (type: string) => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.newdocumentation', { type: type, parent: this.rootDir });
      } else {
        $state.go('management.settings.newdocumentation', { type: type, parent: this.rootDir });
      }
    };

    this.openUrl = (page: any) => {
      if ('FOLDER' === page.type || 'SYSTEM_FOLDER' === page.type) {
        if (this.apiId) {
          return $state.go('management.apis.detail.portal.documentation', { apiId: this.apiId, type: page.type, parent: page.id });
        } else {
          return $state.go('management.settings.documentation', { parent: page.id });
        }
      } else {
        if (this.apiId) {
          return $state.go('management.apis.detail.portal.editdocumentation', { apiId: this.apiId, type: page.type, pageId: page.id });
        } else {
          return $state.go('management.settings.editdocumentation', { pageId: page.id, type: page.type, tab: 'content' });
        }
      }
    };

    this.importPages = () => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.importdocumentation', { apiId: this.apiId });
      } else {
        $state.go('management.settings.importdocumentation');
      }
    };

    this.fetch = () => {
      this.fetchAllInProgress = true;
      DocumentationService.fetchAll(this.apiId)
        .then(() => {
          this.refresh();
          NotificationService.show('Pages has been successfully fetched');
        })
        .finally(() => {
          this.fetchAllInProgress = false;
        });
    };

    this.hasExternalDoc = () => {
      let externalPages = this.pages.filter((page) => page.hasOwnProperty('source'));
      return externalPages.length > 0;
    };

    this.toggleTranslateFolder = () => {
      $scope.translateFolder = !$scope.translateFolder;
    };

    this.saveFolderTranslation = () => {
      if (!this.currentTranslation.id) {
        DocumentationService.create(this.currentTranslation, this.apiId).then((response: any) => {
          const page = response.data;
          NotificationService.show("'" + page.name + "' has been created");
          this.refreshCurrentFolder();
        });
      } else {
        DocumentationService.update(this.currentTranslation, this.apiId).then((response) => {
          NotificationService.show("'" + this.currentTranslation.name + "' has been updated");
          this.refreshCurrentFolder();
        });
      }
    };

    this.selectTranslation = (translation: any) => {
      this.currentTranslation = translation;
    };

    this.addTranslation = () => {
      this.currentTranslation = {
        type: 'TRANSLATION',
        parentId: this.currentFolder.id,
      };
    };
  },
};

export default DocumentationManagementComponent;

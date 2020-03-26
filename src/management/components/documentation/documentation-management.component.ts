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

import NotificationService from "../../../services/notification.service";
import DocumentationService, {DocumentationQuery} from "../../../services/documentation.service";
import {StateService} from "@uirouter/core";
import _ = require('lodash');
import {IScope} from "angular";

interface IDocumentationManagementScope extends IScope {
  renameFolder: boolean;
}

const DocumentationManagementComponent: ng.IComponentOptions = {
  bindings: {
    pages: '<',
    folders: '<'
  },
  template: require('./documentation-management.html'),
  controller: function (
    NotificationService: NotificationService,
    DocumentationService: DocumentationService,
    $state: StateService,
    $scope: IDocumentationManagementScope,
    $mdDialog: angular.material.IDialogService,
    $rootScope: IScope
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.apiId = $state.params.apiId;

    this.$onInit = () => {
      // remove the ROOT page
      this.pages = this.filterROOTPages(this.pages);

      this.rootDir = $state.params.parent;
      this.supportedTypes = DocumentationService.supportedTypes();
      this.foldersById = _.keyBy(this.folders, 'id');
      this.breadcrumb = this.generateBreadcrumb();
      $scope.renameFolder = false;
    };

    this.filterROOTPages = (pagesToFilter: any[]) => {
      return _.filter(pagesToFilter, (p) => p.type !== 'ROOT');
    };

    this.toggleRenameFolder = () => {
      $scope.renameFolder = !$scope.renameFolder;
      if ($scope.renameFolder) {
        this.newFolderName = this.breadcrumb[this.breadcrumb.length -1].name;
      }
    };

    this.renameFolder = () => {
      DocumentationService.partialUpdate("name", this.newFolderName , this.rootDir, this.apiId).then( (response) => {
        NotificationService.show('Folder ' + this.newFolderName + ' has been changed with success');
        this.breadcrumb[this.breadcrumb.length -1].name = response.data.name;
        this.toggleRenameFolder();
      });
    };

    this.generateBreadcrumb = () => {
      let result = [];
      if (this.rootDir) {
        this.addParentToBreadcrumb(this.rootDir, result);
      }
      result.push( { id: "", name: "~" } );
      return result.reverse();
    };

    this.moveToFolder = (page: any) => {
      $mdDialog.show({
        controller: 'MoveToFolderDialogController',
        controllerAs: 'ctrl',
        template: require('./movetofolder.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          itemName: page.name,
          folders: this.generateMoveToFolder(page.id)
        }
      }).then( (destinationId) => {
        if (destinationId) {
          DocumentationService.partialUpdate("parentId", destinationId == -1 ? "" : destinationId, page.id, this.apiId).then( () => {
            NotificationService.show('"' + page.name + '" has been moved with success');
            this.refresh();
          });
        }
      });
    };

    this.generateMoveToFolder = (pageId: string) => {
      let result = [];
      if (this.folders) {
        _.forEach(this.folders, (f) => {
          const path = this.getFolderPath(f.id, pageId);
          if (path) {
            result.push({
              id: f.id,
              path: path
            });
          }
        });
        return _.orderBy(result, ['path'], ['asc']);
      }
      return result;
    };

    this.getFolderPath = (folderId: string, pageToMoveId: string) => {
      let hierarchyNames = [];
      hierarchyNames.push(this.foldersById[folderId].name);
      this.getFolderParentName(folderId, hierarchyNames, pageToMoveId);
      if (hierarchyNames.length == 0) {
        return;
      }
      return "/ " + _.reduceRight(hierarchyNames, (path, name) => {
        return path + " / " + name;
      });
    };

    this.getFolderParentName = (folderId: string, names: string[], pageToMoveId: string) => {
      //do not move a folder to itself
      if (folderId === pageToMoveId ||
        (this.foldersById[folderId].parentId && pageToMoveId === this.foldersById[folderId].parentId)) {
        names.length = 0;
        return;
      }

      if (this.foldersById[folderId].parentId) {
        names.push(this.foldersById[this.foldersById[folderId].parentId].name);
        this.getFolderParentName(this.foldersById[folderId].parentId, names, pageToMoveId);
      }
    };

    this.addParentToBreadcrumb = (id: string, breadcrumb: any[]) => {
      const folder = this.foldersById[id];
      breadcrumb.push(folder);
      if (folder.parentId) {
        this.addParentToBreadcrumb(folder.parentId, breadcrumb);
      }
    };

    this.refresh = () => {
      const q = new DocumentationQuery();
      if (this.rootDir) {
        q.parent = this.rootDir;
      } else {
        q.root = true;
      }
      DocumentationService.search(q, this.apiId).then((response) => this.pages = this.filterROOTPages(response.data));
    };

    this.togglePublish = (page: any) => {
      page.published = !page.published;
      DocumentationService.partialUpdate("published", page.published, page.id, this.apiId).then( () => {
        NotificationService.show('Page ' + page.name + ' has been ' + (page.published ? '':'un') + 'published with success');
      });
    };

    this.upward = (page: any) => {
      page.order = page.order-1;
      DocumentationService.partialUpdate("order", page.order, page.id, this.apiId).then( () => {
        NotificationService.show('Page ' + page.name + ' order has been changed with success');
        this.refresh();
      });
    };

    this.downward = (page: any) => {
      page.order = page.order+1;
      DocumentationService.partialUpdate("order", page.order, page.id, this.apiId).then( () => {
        NotificationService.show('Page ' + page.name + ' order has been changed with success');
        this.refresh();
      });
    };

    this.remove = (page: any) => {
      let that = this;
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to remove "' +page.name + '" ?',
          confirmButton: 'Remove'
        }
      }).then(function (response) {
        if (response) {
          DocumentationService.remove(page.id, that.apiId).then( () => {
            NotificationService.show('Page ' + page.name + ' has been removed');
            that.refresh();
          });
        }
      });
    };

    this.newPage = (type: string) => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.newdocumentation', {type: type, parent: this.rootDir});
      } else {
        $state.go('management.settings.newdocumentation', {type: type, parent: this.rootDir});
      }
    };

    this.openUrl = (page: any) => {
      if ('FOLDER' === page.type) {
        if (this.apiId) {
          return $state.go('management.apis.detail.portal.documentation', {apiId: this.apiId, parent: page.id});
        } else {
          return $state.go('management.settings.documentation', {parent: page.id});
        }
      } else {
        if (this.apiId) {
          return $state.go('management.apis.detail.portal.editdocumentation', {apiId: this.apiId, pageId: page.id});
        } else {
          return $state.go('management.settings.editdocumentation', {pageId: page.id});
        }
      }
    };

    this.importPages = () => {
      if (this.apiId) {
        $state.go('management.apis.detail.portal.importdocumentation', {apiId: this.apiId});
      } else {
        $state.go('management.settings.importdocumentation');
      }
    };

    this.fetch = () => {
      this.fetchAllInProgress = true;
      DocumentationService.fetchAll(this.apiId).then( () => {
        this.refresh();
        NotificationService.show('Pages has been successfully fetched');
      }).finally(() => {
        this.fetchAllInProgress = false;
      });
    };

    this.hasExternalDoc = () => {
      let externalPages = this.pages.filter(page => page.hasOwnProperty("source"));
      return externalPages.length > 0;
    };
  }
};

export default DocumentationManagementComponent;

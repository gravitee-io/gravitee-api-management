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
import { IController, IPromise, IScope } from 'angular';
import * as _ from 'lodash';

import { DocumentationQuery, DocumentationService, PageType } from '../../services/documentation.service';
import NotificationService from '../../services/notification.service';

interface IPageScope extends IScope {
  getContentMode: string;
  fetcherJsonSchema: string;
}
class NewPageComponentController implements IController {
  resolvedFetchers: any[];
  folders: any[];
  systemFolders: any[];
  pageResources: any[];
  categoryResources: any[];
  pagesToLink: any[];

  apiId: string;
  error: any;
  page: any;
  foldersById: _.Dictionary<any>;
  systemFoldersById: _.Dictionary<any>;
  pageList: any[];
  templates: any[];
  selectedTemplate: any;
  constructor(
    private readonly NotificationService: NotificationService,
    private readonly DocumentationService: DocumentationService,
    private readonly Constants,
    private $state: StateService,
    private $scope: IPageScope,
  ) {
    'ngInject';
    this.apiId = this.$state.params.apiId;
    this.error = null;
    this.page = {
      name: '',
      type: this.$state.params.type,
      parentId: this.$state.params.parent,
      visibility: 'PUBLIC',
    };

    this.$scope.getContentMode = 'inline';
  }

  $onInit() {
    this.foldersById = _.keyBy(this.folders, 'id');
    this.systemFoldersById = _.keyBy(this.systemFolders, 'id');
    const folderSituation = this.DocumentationService.getFolderSituation(this.systemFoldersById, this.foldersById, this.page.parentId);
    this.pageList = this.DocumentationService.buildPageList(this.pageResources, true, folderSituation);
    this.pagesToLink = this.DocumentationService.buildPageList(this.pagesToLink, false, folderSituation);

    if (this.DocumentationService.supportedTypes(folderSituation).indexOf(this.page.type) < 0) {
      this.$state.go('management.settings.documentation', { parent: this.$state.params.parent });
    }

    const q = new DocumentationQuery();
    q.type = PageType.MARKDOWN_TEMPLATE;
    q.published = true;
    q.translated = true;
    this.DocumentationService.search(q, null).then((response) => {
      this.templates = response.data;
    });

    const settings = this.Constants.env.settings;
    if (this.page.type === 'SWAGGER' && settings && settings.openAPIDocViewer) {
      this.page.configuration = {
        viewer: settings.openAPIDocViewer.openAPIDocType.defaultType,
      };
    }
  }

  getPageName(): string {
    switch (this.page.type) {
      case PageType.ASCIIDOC:
        return 'New AsciiDoc';
      case PageType.ASYNCAPI:
        return 'New AsyncApi';
      case PageType.FOLDER:
        return 'New Folder';
      case PageType.LINK:
        return 'New Link';
      case PageType.MARKDOWN_TEMPLATE:
        return 'New Markdown Template';
      case PageType.MARKDOWN:
        return 'New Markdown Page';
      case PageType.SWAGGER:
        return 'New Swagger Template';
      default:
        return 'New Page';
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

  onChangeMarkdownTemplate() {
    if (this.selectedTemplate.type) {
      this.page = { ...this.page, content: this.selectedTemplate.content };
    }
  }

  save(gotoParent: boolean): IPromise<void> {
    this.error = null;
    return this.DocumentationService.create(this.page, this.apiId)
      .then((response: any) => {
        const page = response.data;
        if (page.messages && page.messages.length > 0) {
          this.NotificationService.showError(
            "'" + page.name + "' has been created (with validation errors - check the bottom of the page for details)",
          );
        } else {
          this.NotificationService.show("'" + page.name + "' has been created");
        }
        if (gotoParent) {
          this.gotoParent();
        } else {
          this.gotoEdit(page);
        }
      })
      .catch((err) => {
        this.error = { ...err.data, title: 'Sorry, unable to create page' };
      });
  }

  changeContentMode(newMode) {
    if ('fetcher' === newMode) {
      this.page.source = {
        configuration: {},
      };
    } else {
      delete this.page.source;
    }
    this.error = null;
  }

  cancel() {
    this.gotoParent();
  }

  gotoParent() {
    if (this.apiId) {
      this.$state.go('management.apis.detail.portal.documentation', { apiId: this.apiId, parent: this.$state.params.parent });
    } else {
      this.$state.go('management.settings.documentation', { parent: this.$state.params.parent });
    }
  }

  gotoEdit(page: any) {
    if (this.apiId) {
      this.$state.go('management.apis.detail.portal.editdocumentation', { apiId: this.apiId, pageId: page.id, type: page.type });
    } else {
      this.$state.go('management.settings.editdocumentation', { pageId: page.id, type: page.type });
    }
  }
}

export const NewPageComponent: ng.IComponentOptions = {
  bindings: {
    resolvedFetchers: '<',
    folders: '<',
    systemFolders: '<',
    pageResources: '<',
    categoryResources: '<',
    pagesToLink: '<',
  },
  template: require('./new-page.html'),
  controller: NewPageComponentController,
};

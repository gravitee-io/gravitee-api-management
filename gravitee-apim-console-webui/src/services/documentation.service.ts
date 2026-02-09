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

import { IHttpPromise, IPromise } from 'angular';
import { Injector } from '@angular/core';

import { Constants } from '../entities/Constants';

export class DocumentationQuery {
  api?: string;
  name?: string;
  type?: string;
  homepage?: boolean;
  published?: boolean;
  parent?: string;
  root?: boolean;
  translated?: boolean;
}

export class ImportPageEntity {
  type: string;
  published: boolean;
  lastContributor: string;
  source: any;
  configuration: any;
  excluded_groups: string[];
  excludedAccessControls: boolean;
  accessControls: any[];
}

export class Page {
  id?: string;
  content: string;
  name: string;
  parentId: string;
  type: PageType;
}

export enum SystemFolderName {
  HEADER = 'Header',
  TOPFOOTER = 'TopFooter',
  FOOTER = 'Footer',
  ASIDE = 'Aside',
}

export enum PageType {
  ASCIIDOC = 'ASCIIDOC',
  ASYNCAPI = 'ASYNCAPI',
  FOLDER = 'FOLDER',
  LINK = 'LINK',
  SWAGGER = 'SWAGGER',
  MARKDOWN = 'MARKDOWN',
  MARKDOWN_TEMPLATE = 'MARKDOWN_TEMPLATE',
}

export enum FolderSituation {
  SYSTEM_FOLDER,
  SYSTEM_FOLDER_WITH_FOLDERS,
  FOLDER_IN_SYSTEM_FOLDER,
  ROOT,
  FOLDER_IN_FOLDER,
}

export class DocumentationService {
  constructor(
    private readonly $http: ng.IHttpService,
    private readonly $q: ng.IQService,
    private readonly Constants: Constants,
  ) {}

  buildPageList(pagesToFilter: any[], withRootFolder?: boolean, folderSituation?: FolderSituation) {
    const pageList = pagesToFilter
      ?.filter(
        (p) =>
          p.type === 'ASCIIDOC' ||
          p.type === 'ASYNCAPI' ||
          p.type === 'MARKDOWN' ||
          p.type === 'SWAGGER' ||
          (p.type === 'FOLDER' && folderSituation !== FolderSituation.FOLDER_IN_SYSTEM_FOLDER),
      )
      .sort((a, b) => {
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
      pageList?.unshift({ id: 'root', name: '', type: 'FOLDER', fullPath: '' });
    }
    return pageList;
  }

  url(apiId: string, pageId?: string, importFiles?: boolean): string {
    if (apiId) {
      return `${this.Constants.env.baseURL}/apis/${apiId}/pages/` + (importFiles ? '_import' : '') + (pageId ? pageId : '');
    }
    return `${this.Constants.env.baseURL}/portal/pages/` + (importFiles ? '_import' : '') + (pageId ? pageId : '');
  }

  supportedTypes(folderSituation: FolderSituation): PageType[] {
    switch (folderSituation) {
      case FolderSituation.ROOT:
        return [PageType.ASCIIDOC, PageType.ASYNCAPI, PageType.SWAGGER, PageType.MARKDOWN, PageType.MARKDOWN_TEMPLATE, PageType.FOLDER];
      case FolderSituation.SYSTEM_FOLDER:
        return [PageType.LINK];
      case FolderSituation.SYSTEM_FOLDER_WITH_FOLDERS:
        return [PageType.FOLDER, PageType.LINK];
      case FolderSituation.FOLDER_IN_FOLDER:
        return [PageType.ASCIIDOC, PageType.ASYNCAPI, PageType.SWAGGER, PageType.MARKDOWN, PageType.MARKDOWN_TEMPLATE, PageType.FOLDER];
      case FolderSituation.FOLDER_IN_SYSTEM_FOLDER:
        return [PageType.LINK];
    }
  }

  getFolder(systemFoldersById: Record<string, any>, foldersById: Record<string, any>, id: string): any | undefined {
    if (id) {
      let folder = foldersById[id];
      if (!folder) {
        folder = systemFoldersById[id];
      }
      return folder;
    }
  }

  getFolderSituation(
    systemFoldersById: Record<string, any>,
    foldersById: Record<string, any>,
    folderId: string,
  ): FolderSituation | undefined {
    if (!folderId) {
      return FolderSituation.ROOT;
    }
    if (systemFoldersById[folderId]) {
      if (SystemFolderName.TOPFOOTER === systemFoldersById[folderId].name) {
        return FolderSituation.SYSTEM_FOLDER_WITH_FOLDERS;
      } else {
        return FolderSituation.SYSTEM_FOLDER;
      }
    }
    if (foldersById[folderId]) {
      const parentFolderId = foldersById[folderId].parentId;
      if (systemFoldersById[parentFolderId]) {
        return FolderSituation.FOLDER_IN_SYSTEM_FOLDER;
      }
      return FolderSituation.FOLDER_IN_FOLDER;
    }
  }

  partialUpdate(propKey: string, propValue: any, pageId: string, apiId?: string): IHttpPromise<any> {
    const prop = {};
    prop[propKey] = propValue;
    return this.$http.patch(this.url(apiId, pageId), prop);
  }

  search(q: DocumentationQuery, apiId?: string, translated?: boolean): IHttpPromise<Page[]> {
    let url: string = this.url(apiId);
    if (q || translated) {
      // add query parameters
      const queryParams: string[] = [];
      if (q) {
        const keys = Object.keys(q);
        keys?.forEach((key) => {
          const val = q[key];
          if (val !== undefined && val !== '') {
            queryParams.push(key + '=' + val);
          }
        });
      }
      if (translated) {
        queryParams.push('translated=' + translated);
      }
      url += '?' + queryParams.join('&');
    }
    return this.$http.get(url);
  }

  remove(pageId: string, apiId?: string): IHttpPromise<any> {
    return this.$http.delete(this.url(apiId, pageId));
  }

  create(newPage: any, apiId?: string, config?: any): IHttpPromise<any> {
    return this.$http.post(this.url(apiId), newPage, config);
  }

  update(page: any, apiId?: string, config?: any): IHttpPromise<any> {
    return this.$http.put(
      this.url(apiId, page.id),
      {
        name: page.name,
        description: page.description,
        order: page.order,
        content: page.content || '',
        source: page.source,
        published: page.published,
        visibility: page.visibility,
        homepage: page.homepage,
        configuration: page.configuration,
        attached_media: page.attached_media,
        parentId: page.parentId,
        accessControls: page.accessControls,
        excludedAccessControls: page.excludedAccessControls,
      },
      config,
    );
  }

  get(apiId: string, pageId?: string, portal?: boolean, translated?: boolean) {
    if (pageId) {
      const url: string = this.url(apiId, pageId);
      const params: any = {};
      if (portal && portal !== undefined) {
        params.portal = portal;
      }
      if (translated) {
        params.translated = translated;
      }
      return this.$http.get(url, { params });
    }
  }

  getApiHomepage(apiId: string): IPromise<any> {
    const deferred = this.$q.defer();
    this.$http
      .get(this.url(apiId), { params: { homepage: true } })
      .then((response) => {
        if ((<any[]>response.data).length > 0) {
          this.get(apiId, response.data[0].id, true).then((response) => deferred.resolve(response));
        } else {
          deferred.resolve({});
        }
      })
      .catch((msg) => deferred.reject(msg));

    return deferred.promise;
  }

  import(newPage: any, apiId?: string): IHttpPromise<any> {
    const entity = new ImportPageEntity();
    entity.type = newPage.type;
    entity.published = newPage.published;
    entity.lastContributor = newPage.lastContributor;
    entity.source = newPage.source;
    entity.configuration = newPage.configuration;
    entity.excluded_groups = newPage.excluded_groups;
    entity.excludedAccessControls = newPage.excludedAccessControls;
    entity.accessControls = newPage.accessControls;
    return this.$http.post(this.url(apiId, null, true), entity, { timeout: 30000 });
  }

  fetch(pageId: string, apiId?: string): IHttpPromise<any> {
    return this.$http.post(this.url(apiId, pageId) + '/_fetch', null, { timeout: 30000 });
  }

  fetchAll(apiId: string): IHttpPromise<any> {
    return this.$http.post(this.url(apiId) + '_fetch', null, { timeout: 30000 });
  }

  addMedia(media: any, pageId: string, apiId?: string): IHttpPromise<any> {
    return this.$http.post(this.url(apiId, pageId) + '/media', media, { headers: { 'Content-Type': undefined } });
  }

  getMedia(pageId: string, apiId?: string): IHttpPromise<any> {
    return this.$http.get(this.url(apiId, pageId) + '/media');
  }
}
DocumentationService.$inject = ['$http', '$q', 'Constants'];

export const ajsDocumentationServiceProvider = {
  deps: ['$injector'],
  provide: 'ajsDocumentationService',
  useFactory: (injector: Injector) => injector.get('DocumentationService'),
};

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
import _ = require('lodash');

export class DocumentationQuery {
  api: string;
  name: string;
  type: string;
  homepage?: boolean;
  published?: boolean;
  parent: string;
  root?: boolean;
}

export class ImportPageEntity {
  type: string;
  published: boolean;
  lastContributor: string;
  source: any;
  configuration: any;
  excluded_groups: string[];
}

export enum SystemFolderName {
  HEADER = 'Header',
  TOPFOOTER = 'TopFooter',
  FOOTER = 'Footer',
  ASIDE = 'Aside',
}

export enum FolderSituation {
  SYSTEM_FOLDER,
  SYSTEM_FOLDER_WITH_FOLDERS,
  FOLDER_IN_SYSTEM_FOLDER,
  ROOT,
  FOLDER_IN_FOLDER,
}

class DocumentationService {
  private folderPromise;

  constructor(private $http: ng.IHttpService, private $q: ng.IQService, private Constants: any) {
    'ngInject';
  }

  url = (apiId: string, pageId?: string, importFiles?: boolean): string => {
    if (apiId) {
      return `${this.Constants.env.baseURL}/apis/${apiId}/pages/` + (importFiles ? '_import' : '') + (pageId ? pageId : '');
    }
    return `${this.Constants.env.baseURL}/portal/pages/` + (importFiles ? '_import' : '') + (pageId ? pageId : '');
  };

  supportedTypes = (folderSituation: FolderSituation): string[] => {
    switch (folderSituation) {
      case FolderSituation.ROOT:
        return ['SWAGGER', 'MARKDOWN', 'FOLDER'];
      case FolderSituation.SYSTEM_FOLDER:
        return ['LINK'];
      case FolderSituation.SYSTEM_FOLDER_WITH_FOLDERS:
        return ['FOLDER', 'LINK'];
      case FolderSituation.FOLDER_IN_FOLDER:
        return ['SWAGGER', 'MARKDOWN', 'FOLDER'];
      case FolderSituation.FOLDER_IN_SYSTEM_FOLDER:
        return ['LINK'];
    }
  };

  partialUpdate = (propKey: string, propValue: any, pageId: string, apiId?: string): IHttpPromise<any> => {
    let prop = {};
    prop[propKey] = propValue;
    return this.$http.patch(this.url(apiId, pageId), prop);
  };

  search = (q: DocumentationQuery, apiId?: string, translated?: boolean): IHttpPromise<any> => {
    let url: string = this.url(apiId);
    if (q || translated) {
      // add query parameters
      let queryParams: string[] = [];
      if (q) {
        const keys = Object.keys(q);
        _.forEach(keys, (key) => {
          let val = q[key];
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
  };

  remove = (pageId: string, apiId?: string): IHttpPromise<any> => {
    return this.$http.delete(this.url(apiId, pageId));
  };

  create = (newPage: any, apiId?: string, config?: any): IHttpPromise<any> => {
    return this.$http.post(this.url(apiId), newPage, config);
  };

  update = (page: any, apiId?: string, config?: any): IHttpPromise<any> => {
    return this.$http.put(
      this.url(apiId, page.id),
      {
        name: page.name,
        description: page.description,
        order: page.order,
        content: page.content || '',
        source: page.source,
        published: page.published,
        homepage: page.homepage,
        configuration: page.configuration,
        excluded_groups: page.excluded_groups,
        attached_media: page.attached_media,
        parentId: page.parentId,
      },
      config,
    );
  };

  get(apiId: string, pageId?: string, portal?: boolean, translated?: boolean) {
    if (pageId) {
      let url: string = this.url(apiId, pageId);
      let params: any = {};
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
    let deferred = this.$q.defer();
    let that = this;
    this.$http
      .get(this.url(apiId), { params: { homepage: true } })
      .then(function (response) {
        if ((<any[]>response.data).length > 0) {
          that.get(apiId, response.data[0].id, true).then((response) => deferred.resolve(response));
        } else {
          deferred.resolve({});
        }
      })
      .catch((msg) => deferred.reject(msg));

    return deferred.promise;
  }

  import(newPage: any, apiId?: string): IHttpPromise<any> {
    let entity = new ImportPageEntity();
    entity.type = newPage.type;
    entity.published = newPage.published;
    entity.lastContributor = newPage.lastContributor;
    entity.source = newPage.source;
    entity.configuration = newPage.configuration;
    entity.excluded_groups = newPage.excluded_groups;
    return this.$http.post(this.url(apiId, null, true), entity, { timeout: 30000 });
  }

  fetch = (pageId: string, apiId?: string): IHttpPromise<any> => {
    return this.$http.post(this.url(apiId, pageId) + '/_fetch', null, { timeout: 30000 });
  };

  fetchAll = (apiId: string): IHttpPromise<any> => {
    return this.$http.post(this.url(apiId) + '_fetch', null, { timeout: 30000 });
  };

  addMedia = (media: any, pageId: string, apiId?: string): IHttpPromise<any> => {
    return this.$http.post(this.url(apiId, pageId) + '/media', media, { headers: { 'Content-Type': undefined } });
  };

  getMedia = (pageId: string, apiId?: string): IHttpPromise<any> => {
    return this.$http.get(this.url(apiId, pageId) + '/media');
  };
}

export default DocumentationService;

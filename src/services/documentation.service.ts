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

import {IHttpPromise} from "angular";
import {IPromise} from "angular";
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

class DocumentationService {
  private folderPromise;

  constructor(
    private $http: ng.IHttpService,
    private $q: ng.IQService,
    private Constants: any) {
    'ngInject';
  }

  url = (apiId: string, pageId?: string): string => {
    if (apiId) {
      return `${this.Constants.baseURL}apis/${apiId}/pages/` + (pageId ? pageId : '');
    }
    return `${this.Constants.baseURL}portal/pages/` + (pageId ? pageId : '');

  };

  supportedTypes = (): string[] => {
    return ["SWAGGER", "MARKDOWN", "FOLDER"]
  };

  partialUpdate = (propKey: string, propValue: any, pageId: string, apiId?: string): IHttpPromise<any> => {
    let prop = {};
    prop[propKey] = propValue;
    return this.$http.patch(this.url(apiId, pageId), prop);
  };


  search = (q: DocumentationQuery, apiId?: string): IHttpPromise<any> => {
    let url: string = this.url(apiId);
    if (q) {
      // add query parameters
      url += "?";
      const keys = Object.keys(q);
      _.forEach(keys, function (key) {
        let val = q[key];
        if (val !== undefined && val !== '') {
          url += key + '=' + val + '&';
        }
      });
    }
    return this.$http.get(url);
  };

  remove = (pageId: string, apiId?: string): IHttpPromise<any> => {
    return this.$http.delete(this.url(apiId, pageId));
  };

  get2 = (pageId: string, apiId?: string): IHttpPromise<any> => {
    return this.$http.get(this.url(apiId, pageId));
  };

  create = (newPage: any, apiId?: string): IHttpPromise<any> => {
    return this.$http.post(this.url(apiId), newPage);
  };

  update = (page: any, apiId?: string): IHttpPromise<any> => {
    return this.$http.put(this.url(apiId, page.id),
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
        parentId: page.parentId
      }
    );
  };

  get(apiId: string, pageId?: string, portal?: boolean) {
    if (pageId) {
      return this.$http.get(this.url(apiId, pageId) + (portal !== undefined?'?portal=' + portal:''));
    }
  }

  getApiHomepage(apiId: string): IPromise<any> {
    let deferred = this.$q.defer();
    let that = this;
    this.$http
      .get(this.url(apiId), {params:{"homepage": true}})
      .then(function(response) {
        if ((<any[]>response.data).length > 0) {
          that
            .get(apiId, response.data[0].id, true)
            .then(response => deferred.resolve(response));
        } else {
          deferred.resolve({});
        }
      })
      .catch( msg => deferred.reject(msg) );

    return deferred.promise;
  }
}

export default DocumentationService;

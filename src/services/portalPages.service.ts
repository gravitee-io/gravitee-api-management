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
import * as _ from 'lodash';

class PortalPagesService {
  private pagesURL: () => string;
  private folderPromise;

  constructor(private $http, private $q, Constants) {
    'ngInject';
    this.pagesURL = () => `${Constants.baseURL}portal/pages/`;
  }

  list() {
    return this.$http.get(this.pagesURL());
  }

  fullList() {
    const deferredFolders = this.$q.defer();
    this.folderPromise = deferredFolders.promise;

    return this.$http.get(this.pagesURL(), {params: {"flatMode": true}})
      .then(response => {

        let pages = response.data;

        const map = new Map<string, string>();
        pages.forEach(p => {
          if (p.type === 'folder') {
            map.set(p.id, p.name);
          }
        });

        deferredFolders.resolve(map);

        return {pages: pages};
      });
  }

  getFolderPromise() {
    return this.folderPromise;
  }

  get(pageId?: string) {
    if (pageId) {
      return this.$http.get(this.pagesURL() + pageId);
    } else {
      return this.$q.resolve({});
    }
  }

  getHomepage() {
    let deferred = this.$q.defer();
    let that = this;
    this.$http
      .get(this.pagesURL(), {params:{"homepage": true}})
      .then(function(response) {
        if (response.data.length > 0) {
          that
            .get(response.data[0].id)
            .then(response => deferred.resolve(response));
        } else {
          deferred.resolve({});
        }
      })
      .catch( msg => deferred.reject(msg) );

    return deferred.promise;
  }

  listPortalDocumentation() {
    return this.$http
      .get(this.pagesURL(), {params:{"homepage": false}});
  }

  getContentUrl(pageId) {
    return this.pagesURL() + pageId + '/content';
  }

  createPage(newPage) {
    return this.$http.post(this.pagesURL(), newPage);
  }

  deletePage(pageId) {
    return this.$http.delete(this.pagesURL() + pageId);
  }

  editPage(pageId, editPage) {
    return this.$http.put(this.pagesURL() + pageId,
      {
        name: editPage.name,
        description: editPage.description,
        order: editPage.order,
        published: editPage.published,
        content: editPage.content || '',
        source: editPage.source,
        homepage: editPage.homepage,
        configuration: editPage.configuration,
        excluded_groups: editPage.excludedGroups,
        parentId: editPage.parentId
      }
    );
  }
}

export default PortalPagesService;

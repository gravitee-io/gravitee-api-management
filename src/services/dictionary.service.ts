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
class DictionaryService {

  private dictionariesURL: string;

  constructor(
    private $http: ng.IHttpService,
    Constants) {
    'ngInject';
    this.dictionariesURL = `${Constants.envBaseURL}/configuration/dictionaries`;
  }

  list(): ng.IPromise<any> {
    return this.$http.get(this.dictionariesURL);
  }

  get(id: string): ng.IPromise<any> {
    return this.$http.get([this.dictionariesURL, id].join('/'));
  }

  create(dictionary) {
    return this.$http.post(this.dictionariesURL, dictionary);
  }

  update(dictionary) {
    return this.$http.put([this.dictionariesURL, dictionary.id].join('/'), {
      name: dictionary.name,
      description: dictionary.description,
      type: dictionary.type,
      properties: dictionary.properties,
      provider: dictionary.provider,
      trigger: dictionary.trigger
    });
  }

  delete(dictionary: any) {
    return this.$http.delete([this.dictionariesURL, dictionary.id].join('/'));
  }

  deploy(dictionary: any) {
    return this.$http.post([this.dictionariesURL, dictionary.id, '_deploy'].join('/'), {});
  }

  undeploy(dictionary: any) {
    return this.$http.post([this.dictionariesURL, dictionary.id, '_undeploy'].join('/'), {});
  }

  start(dictionary: any) {
    return this.$http.post([this.dictionariesURL, dictionary.id].join('/') + '?action=START', {});
  }

  stop(dictionary: any) {
    return this.$http.post([this.dictionariesURL, dictionary.id].join('/') + '?action=STOP', {});
  }
}

export default DictionaryService;

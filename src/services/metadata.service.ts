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
class MetadataService {
  private metadataURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.metadataURL = `${Constants.envBaseURL}/configuration/metadata/`;
  }

  listFormats() {
    return ['string', 'numeric', 'boolean', 'date', 'mail', 'url'];
  }

  list() {
    return this.$http.get(this.metadataURL);
  }

  create(metadata) {
    return this.$http.post(this.metadataURL, metadata);
  }

  update(metadata) {
    return this.$http.put(this.metadataURL, metadata);
  }

  delete(metadata) {
    return this.$http.delete(this.metadataURL + metadata.key);
  }
}

export default MetadataService;

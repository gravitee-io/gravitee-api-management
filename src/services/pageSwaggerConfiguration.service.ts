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

class PageSwaggerConfigurationService {

  constructor(private $q, private DocumentationService) {
    'ngInject';
  }

  execute(swaggerUrl, swaggerSpec) {
    let deferred = this.$q.defer();
    let url = new URL(swaggerUrl);
    let swaggerConfig = this.DocumentationService.getPageConfigurationFromCache(url.pathname);
    if (!_.isNil(swaggerConfig) && swaggerConfig.tryIt && swaggerConfig.tryItURL) {
      try {
        let apiUrl = new URL(swaggerConfig.tryItURL);
        swaggerSpec.host = apiUrl.host;
        swaggerSpec.schemes = [apiUrl.protocol.slice(0, -1)];
        swaggerSpec.basePath = apiUrl.pathname;
        if (swaggerSpec.basePath[swaggerSpec.basePath.length-1] === "/") {
          swaggerSpec.basePath = swaggerSpec.basePath.slice(0, -1);
        }
        deferred.resolve(swaggerSpec);
      } catch (error) {
        deferred.reject({message: 'Bad URL in Swagger descriptor', code: '500'});
      }
    } else {
      deferred.resolve(false);
    }

    return deferred.promise;
  }

}

export default PageSwaggerConfigurationService;

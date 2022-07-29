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
import { ApiEntityFlowModeEnum } from '@management-models/ApiEntity';
import { Visibility } from '@management-models/Visibility';
import { LoadBalancerTypeEnum } from '@management-models/LoadBalancer';
import faker from '@faker-js/faker';
export var ApiMetadataFormat;
(function (ApiMetadataFormat) {
  ApiMetadataFormat['STRING'] = 'STRING';
  ApiMetadataFormat['NUMERIC'] = 'NUMERIC';
  ApiMetadataFormat['BOOLEAN'] = 'BOOLEAN';
  ApiMetadataFormat['DATE'] = 'DATE';
  ApiMetadataFormat['MAIL'] = 'MAIN';
  ApiMetadataFormat['URL'] = 'URL';
})(ApiMetadataFormat || (ApiMetadataFormat = {}));
export class ApisFaker {
  static version() {
    const major = faker.datatype.number({ min: 1, max: 5 });
    const minor = faker.datatype.number({ min: 1, max: 10 });
    const patch = faker.datatype.number({ min: 1, max: 30 });
    return `${major}.${minor}.${patch}`;
  }
  static uniqueWord() {
    return `${faker.random.word()}-${faker.datatype.uuid()}`;
  }
  static apiImport(attributes) {
    const name = faker.commerce.productName();
    const version = this.version();
    const description = faker.commerce.productDescription();
    return {
      name,
      version,
      description,
      visibility: Visibility.PRIVATE,
      gravitee: '2.0.0',
      flow_mode: ApiEntityFlowModeEnum.DEFAULT,
      resources: [],
      properties: [],
      groups: [],
      plans: [],
      path_mappings: [],
      proxy: this.proxy(),
      response_templates: {},
      flows: [],
      ...attributes,
    };
  }
  static api(attributes) {
    const name = faker.commerce.productName();
    const version = this.version();
    const description = faker.commerce.productDescription();
    return {
      name,
      version,
      description,
      visibility: Visibility.PRIVATE,
      gravitee: '2.0.0',
      flow_mode: ApiEntityFlowModeEnum.DEFAULT,
      resources: [],
      properties: [],
      groups: [],
      plans: [],
      path_mappings: [],
      proxy: this.proxy(),
      response_templates: {},
      ...attributes,
    };
  }
  static newApi(attributes) {
    const name = faker.commerce.productName();
    const version = this.version();
    const description = faker.commerce.productDescription();
    return {
      contextPath: `/${faker.random.word()}-${faker.datatype.uuid()}-${Math.floor(Date.now() / 1000)}`,
      name,
      description,
      version,
      endpoint: `${process.env.WIREMOCK_BASE_URL}/hello`,
      ...attributes,
    };
  }
  static proxy(attributes) {
    return {
      virtual_hosts: [
        {
          path: `/${faker.helpers.slugify(faker.commerce.productName())}`,
        },
      ],
      strip_context_path: false,
      preserve_host: false,
      groups: [
        {
          name: 'default-group',
          endpoints: [
            {
              inherit: true,
              name: 'default',
              target: `${process.env.WIREMOCK_BASE_URL}/hello`,
              weight: 1,
              backup: false,
              type: 'http',
            },
          ],
          load_balancing: {
            type: LoadBalancerTypeEnum.ROUNDROBIN,
          },
          http: {
            connectTimeout: 5000,
            idleTimeout: 60000,
            keepAlive: true,
            readTimeout: 10000,
            pipelining: false,
            maxConcurrentConnections: 100,
            useCompression: true,
            followRedirects: false,
          },
        },
      ],
      ...attributes,
    };
  }
  static newRating() {
    return {
      title: faker.random.word(),
      comment: `${faker.commerce.productDescription()}`,
      rate: `${faker.datatype.number({ min: 1, max: 5, precision: 1 })}`,
    };
  }
}

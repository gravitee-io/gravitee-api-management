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
import { ApiEntity, ApiEntityFlowModeEnum } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { PageEntity, PageEntityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/PageEntity';
import { Visibility, VisibilityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/Visibility';
import { LoadBalancerTypeEnum } from '@gravitee/management-webclient-sdk/src/lib/models/LoadBalancer';
import { Proxy, ProxyToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/Proxy';
import { NewApiEntity } from '@gravitee/management-webclient-sdk/src/lib/models/NewApiEntity';
import { faker } from '@faker-js/faker';
import { NewRatingEntity } from '@gravitee/management-webclient-sdk/src/lib/models/NewRatingEntity';
import { RatingInput } from '@gravitee/portal-webclient-sdk/src/lib/models/RatingInput';
import { PrimaryOwnerEntity, PrimaryOwnerEntityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/PrimaryOwnerEntity';
import { ResponseTemplate } from '@gravitee/management-webclient-sdk/src/lib/models/ResponseTemplate';
import { Flow, FlowToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/Flow';
import { PlanEntity, PlanEntityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/PlanEntity';
import { PropertyToJSON, ResourceToJSON } from '@gravitee/management-webclient-sdk/src/lib/models';

export interface ApiImportEntity {
  id?: string;
  crossId?: string;
  name?: string;
  version?: string;
  description?: string;
  visibility?: Visibility;
  gravitee?: string;
  execution_mode?: string;
  flow_mode?: ApiEntityFlowModeEnum;
  flows?: Array<Flow>;
  resources?: Array<any>;
  properties?: Array<any>;
  groups?: Array<string>;
  path_mappings?: Array<string>;
  members?: Array<any>;
  pages?: Array<PageEntity>;
  plans?: Array<PlanEntity>;
  metadata?: any;
  primaryOwner?: PrimaryOwnerEntity;
  response_templates?: { [key: string]: { [key: string]: ResponseTemplate } };
  proxy?: Proxy;
}

export enum ApiMetadataFormat {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  MAIL = 'MAIN',
  URL = 'URL',
}

export class ApisFaker {
  static version() {
    const major = faker.number.int({ min: 1, max: 5 });
    const minor = faker.number.int({ min: 1, max: 10 });
    const patch = faker.number.int({ min: 1, max: 30 });
    return `${major}.${minor}.${patch}`;
  }

  static uniqueWord() {
    return `${faker.lorem.word()}-${faker.string.uuid()}`;
  }

  static apiImport(attributes?: Partial<ApiImportEntity>): ApiImportEntity {
    const name = faker.commerce.productName();
    const version = this.version();
    const description = faker.lorem.words(10);

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

  static api(attributes?: Partial<ApiEntity>): ApiEntity {
    const name = faker.commerce.productName();
    const version = this.version();
    const description = faker.lorem.words(10);

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

  static newApi(attributes?: Partial<NewApiEntity>): NewApiEntity {
    const name = faker.commerce.productName();
    const version = this.version();
    const description = faker.lorem.words(10);

    return {
      contextPath: `/${faker.lorem.word()}-${faker.string.uuid()}-${Math.floor(Date.now() / 1000)}`,
      name,
      description,
      version,
      endpoint: `${process.env.WIREMOCK_BASE_URL}/hello`,
      ...attributes,
    };
  }

  static proxy(attributes?: Partial<Proxy>): Proxy {
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
            type: LoadBalancerTypeEnum.ROUND_ROBIN,
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

  static newRating(): NewRatingEntity {
    return {
      title: faker.lorem.word(),
      comment: `${faker.lorem.words(10)}`,
      rate: `${faker.number.int({ min: 1, max: 5 })}`,
    };
  }

  static newRatingInput(): RatingInput {
    return {
      title: faker.lorem.word(),
      comment: `${faker.lorem.words(10)}`,
      value: faker.number.int({ min: 1, max: 5 }),
    };
  }
}

export function ApiImportEntityToJSON(value?: ApiImportEntity | null): any {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  return {
    id: value.id,
    crossId: value.crossId,
    name: value.name,
    version: value.version,
    description: value.description,
    visibility: VisibilityToJSON(value.visibility),
    gravitee: value.gravitee,
    flow_mode: value.flow_mode,
    flows: value.flows === undefined ? undefined : (value.flows as Array<any>).map(FlowToJSON),
    resources: value.resources === undefined ? undefined : (value.resources as Array<any>).map(ResourceToJSON),
    properties: value.properties === undefined ? undefined : (value.properties as Array<any>).map(PropertyToJSON),
    groups: value.groups,
    path_mappings: value.path_mappings,
    members: value.members,
    pages: value.pages === undefined ? undefined : (value.pages as Array<any>).map(PageEntityToJSON),
    plans: value.plans === undefined ? undefined : (value.plans as Array<any>).map(PlanEntityToJSON),
    metadata: value.metadata,
    primaryOwner: PrimaryOwnerEntityToJSON(value.primaryOwner),
    response_templates: value.response_templates,
    proxy: ProxyToJSON(value.proxy),
  };
}

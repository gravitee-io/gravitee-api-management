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
import * as faker from 'faker';
import { ApiImport, ApiImportMember, ApiImportPage, ApiImportPlan, ApiImportProxyGroupLoadBalancerType } from '@model/api-imports';
import { ApiPageType, ApiVisibility } from '@model/apis';
import { FlowMode } from '@model/api-flows';
import { ApiFakers } from './apis';
import { ApiUser } from '@model/users';
import { Role } from '@model/roles';
import { PlanSecurityType, PlanStatus, PlanType, PlanValidation } from '@model/plan';

export class ApiImportFakers {
  static api(attributes?: Partial<ApiImport>): ApiImport {
    const name = faker.commerce.productName();
    const version = ApiFakers.version();
    const description = faker.commerce.productDescription();

    return {
      name,
      version,
      description,
      visibility: ApiVisibility.PRIVATE,
      gravitee: '2.0.0',
      flow_mode: FlowMode.DEFAULT,
      resources: [],
      properties: [],
      groups: [],
      members: [],
      pages: [],
      plans: [],
      metadata: [],
      path_mappings: [],
      proxy: {
        virtual_hosts: [
          {
            path: `/${faker.helpers.slugify(name)}`,
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
                target: 'http://localhost:8080/hello-world',
                weight: 1,
                backup: false,
                type: 'http',
              },
            ],
            load_balancing: {
              type: ApiImportProxyGroupLoadBalancerType.ROUND_ROBIN,
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
      },
      response_templates: {},
      ...attributes,
    };
  }

  static page(attributes?: Partial<ApiImportPage>): ApiImportPage {
    const content = faker.lorem.paragraph(3);
    const name = faker.commerce.productName();

    return {
      name,
      type: ApiPageType.MARKDOWN,
      content,
      order: 1,
      published: false,
      visibility: ApiVisibility.PUBLIC,
      contentType: 'application/json',
      homepage: false,
      parentPath: '',
      excludedAccessControls: false,
      accessControls: [],
      ...attributes,
    };
  }

  static plan(attributes?: Partial<ApiImportPlan>): ApiImportPlan {
    const name = faker.commerce.productName();
    const description = faker.commerce.productDescription();

    return {
      name,
      description,
      validation: PlanValidation.AUTO,
      security: PlanSecurityType.KEY_LESS,
      type: PlanType.API,
      status: PlanStatus.STAGING,
      order: 1,
      characteristics: [],
      paths: {},
      flows: [],
      comment_required: false,
      ...attributes,
    };
  }

  static user(attributes?: Partial<ApiUser>): ApiUser {
    const firstname = faker.name.firstName();
    const lastname = faker.name.lastName();
    const email = faker.internet.email(firstname, lastname);

    return {
      firstname,
      lastname,
      email,
      source: 'gravitee',
      sourceId: email,
      service: false,
      ...attributes,
    };
  }

  static member(attributes?: Partial<ApiImportMember>): ApiImportMember {
    return {
      source: 'gravitee',
      sourceId: faker.internet.email(),
      roles: [],
      ...attributes,
    };
  }

  static role(attributes?: Partial<Role>): Role {
    return {
      default: false,
      description: faker.commerce.productDescription(),
      name: faker.commerce.productName(),
      permissions: {},
      scope: 'API',
      system: false,
      ...attributes,
    };
  }
}

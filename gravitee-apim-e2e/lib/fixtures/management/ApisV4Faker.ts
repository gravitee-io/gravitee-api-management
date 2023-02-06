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
import faker from '@faker-js/faker';
import {
  FlowExecutionV4,
  FlowExecutionV4ModeEnum,
  HttpListenerV4,
  ListenerV4,
  NewApiEntityV4,
  NewApiEntityV4DefinitionVersionEnum,
  NewApiEntityV4TypeEnum,
  SubscriptionListenerV4,
} from '@gravitee/management-webclient-sdk/src/lib/models';

export class ApisV4Faker {
  static version() {
    const major = faker.datatype.number({ min: 1, max: 5 });
    const minor = faker.datatype.number({ min: 1, max: 10 });
    const patch = faker.datatype.number({ min: 1, max: 30 });
    return `${major}.${minor}.${patch}`;
  }

  static newApi(attributes?: Partial<NewApiEntityV4>): NewApiEntityV4 {
    const name = faker.commerce.productName();
    const apiVersion = this.version();
    const description = faker.commerce.productDescription();

    return {
      apiVersion,
      definitionVersion: NewApiEntityV4DefinitionVersionEnum._4_0_0,
      description,
      name,
      endpointGroups: [],
      flowExecution: {
        flowMode: FlowExecutionV4ModeEnum.DEFAULT,
        matchRequired: false,
      } as FlowExecutionV4,
      flows: [],
      groups: [],
      listeners: [],
      tags: [],
      type: NewApiEntityV4TypeEnum.ASYNC,
      ...attributes,
    };
  }

  static newHttpListener(attributes?: Partial<HttpListenerV4>): ListenerV4 {
    return {
      // @ts-ignore
      type: 'http',
      paths: [],
      pathMappings: [],
      entrypoints: [],
      ...attributes,
    };
  }

  static newSubscriptionListener(attributes?: Partial<SubscriptionListenerV4>): ListenerV4 {
    return {
      // @ts-ignore
      type: 'subscription',
      paths: [],
      pathMappings: [],
      entrypoints: [],
      ...attributes,
    };
  }
}

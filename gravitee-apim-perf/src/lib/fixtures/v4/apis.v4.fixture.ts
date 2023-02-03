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
import { randomString, randomVersion } from '@helpers/random.helper';
import {
  NewApiEntityV4,
  NewApiEntityV4DefinitionVersionEnum,
  NewApiEntityV4FlowModeEnum,
  NewApiEntityV4TypeEnum,
} from '@models/v4/NewApiEntityV4';
import { HttpListenerV4, ListenerV4 } from '@models/v4/ListenerV4';

export class ApisV4Fixture {
  static newApi(attributes?: Partial<NewApiEntityV4>): NewApiEntityV4 {
    const name = randomString();
    const apiVersion = randomVersion();
    const description = `Description: ${randomString()}`;

    return {
      apiVersion,
      definitionVersion: NewApiEntityV4DefinitionVersionEnum._4_0_0,
      description,
      name,
      endpointGroups: [],
      flowMode: NewApiEntityV4FlowModeEnum.DEFAULT,
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
}

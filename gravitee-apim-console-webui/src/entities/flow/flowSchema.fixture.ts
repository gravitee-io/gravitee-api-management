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
import { FlowSchema } from './flowSchema';

export function fakeFlowSchema(attributes?: FlowSchema): FlowSchema {
  const base: FlowSchema = {
    type: 'object',
    id: 'apim',
    properties: {
      name: {
        title: 'Name',
        description: 'The name of flow. If empty, the name will be generated with the path and methods',
        type: 'string',
      },
      'path-operator': {
        type: 'object',
        properties: {
          operator: {
            title: 'Operator path',
            description: 'The operator path',
            type: 'string',
            enum: ['EQUALS', 'STARTS_WITH'],
            default: 'STARTS_WITH',
            'x-schema-form': {
              titleMap: {
                EQUALS: 'Equals',
                STARTS_WITH: 'Starts with',
              },
            },
          },
          path: {
            title: 'Path',
            description: 'The path of flow (must start by /)',
            type: 'string',
            pattern: '^/',
            default: '/',
          },
        },
        required: ['path', 'operator'],
      },
      methods: {
        title: 'Methods',
        description: 'The HTTP methods of flow (ALL if empty)',
        type: 'array',
        items: {
          type: 'string',
          enum: ['CONNECT', 'DELETE', 'GET', 'HEAD', 'OPTIONS', 'PATCH', 'POST', 'PUT', 'TRACE'],
        },
      },
      condition: {
        title: 'Condition',
        description: 'The condition of the flow. Supports EL.',
        type: 'string',
        'x-schema-form': {
          'expression-language': true,
        },
      },
    },
    required: [],
    disabled: [],
  };
  return { ...base, ...attributes };
}

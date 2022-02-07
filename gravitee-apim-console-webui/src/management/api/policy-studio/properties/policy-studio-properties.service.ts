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

import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { Json } from '../../../../util';

const PROPERTY_PROVIDERS = [
  {
    id: 'HTTP',
    name: 'Custom (HTTP)',
    schema: {
      type: 'object',
      properties: {
        method: {
          title: 'HTTP Method',
          description: 'HTTP method to invoke the endpoint.',
          type: 'string',
          default: 'GET',
          enum: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'CONNECT', 'OPTIONS', 'TRACE'],
        },
        url: {
          title: 'Http service URL',
          description: 'http://localhost',
          type: 'string',
          pattern: '^(http://|https://)',
        },
        useSystemProxy: {
          title: 'Use system proxy',
          description: 'Use the system proxy configured by your administrator.',
          type: 'boolean',
        },
        headers: {
          type: 'array',
          title: 'Request Headers',
          items: {
            type: 'object',
            title: 'Header',
            properties: {
              name: {
                title: 'Name',
                type: 'string',
              },
              value: {
                title: 'Value',
                type: 'string',
              },
            },
          },
        },
        body: {
          title: 'Request body',
          type: 'string',
          'x-schema-form': {
            type: 'codemirror',
            codemirrorOptions: {
              lineWrapping: true,
              lineNumbers: true,
              allowDropFileTypes: true,
              autoCloseTags: true,
            },
          },
        },
        specification: {
          title: 'Transformation (JOLT Specification)',
          type: 'string',
          'x-schema-form': {
            type: 'codemirror',
            codemirrorOptions: {
              lineWrapping: true,
              lineNumbers: true,
              allowDropFileTypes: true,
              autoCloseTags: true,
              mode: 'javascript',
            },
          },
        },
      },
      required: ['url', 'specification'],
    },
    documentation:
      '= Custom (HTTP)\n\n=== How to ?\n\n 1. Set `Polling frequency interval` and `Time unit`\n2. Set the `HTTP service URL`\n 3. If the HTTP service doesn\'t return the expected output, add a JOLT `transformation` \n\n[source, json]\n----\n[\n  {\n    "key": 1,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 2,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 3,\n    "value": "https://south-asia.company.com/"\n  }\n]\n----\n',
  },
];
const PROPERTY_PROVIDER_TITLES = PROPERTY_PROVIDERS.reduce((map, provider) => {
  map[provider.id] = provider.name;
  return map;
}, {} as Record<string, string>);
const PROPERTY_PROVIDER_IDS = Object.keys(PROPERTY_PROVIDER_TITLES);
const DYNAMIC_PROPERTY_SCHEMA = {
  properties: {
    enabled: {
      type: 'boolean',
      title: 'Enabled',
      description: ' This service is requiring an API deployment. Do not forget to deploy API to start dynamic-properties service.',
    },
    trigger: {
      type: 'object',
      properties: {
        rate: {
          type: 'integer',
          title: 'Polling frequency interval',
        },
        unit: {
          type: 'string',
          title: 'Time unit',
          enum: ['SECONDS', 'MINUTES', 'HOURS'],
        },
      },
      required: ['rate', 'unit'],
    },
    provider: {
      type: 'string',
      title: 'Provider type',
      enum: PROPERTY_PROVIDER_IDS,
      default: PROPERTY_PROVIDER_IDS[0],
      'x-schema-form': {
        titleMap: PROPERTY_PROVIDER_TITLES,
      },
    },
  },
  required: ['trigger', 'provider'],
};

@Injectable({
  providedIn: 'root',
})
export class PolicyStudioPropertiesService {
  getProviders(): Observable<Json> {
    return of(PROPERTY_PROVIDERS);
  }

  getDynamicPropertySchema(): Observable<Record<string, Json>> {
    return of(DYNAMIC_PROPERTY_SCHEMA);
  }
}

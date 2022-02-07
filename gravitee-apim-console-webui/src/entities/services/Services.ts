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

import { HttpMethod } from '../HttpMethod';

export interface DynamicPropertyService {
  enabled?: boolean;
  provider?: 'HTTP';
  configuration?: unknown;
  schedule?: string;
}

export interface Services {
  discovery?: {
    enabled?: boolean;
    provider?: string;
    configuration?: string;
  };
  'health-check'?: {
    enabled?: boolean;
    schedule?: string;
    steps?: {
      name?: string;
      request?: {
        path?: string;
        method?: HttpMethod;
        headers?: { name: string; value: string }[];
        body?: string;
        fromRoot?: boolean;
      };
      response?: {
        assertions: string[];
      };
    }[];
  };
  'dynamic-property'?: DynamicPropertyService;
}

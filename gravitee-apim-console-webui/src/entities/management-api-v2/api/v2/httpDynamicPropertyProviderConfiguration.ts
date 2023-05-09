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

import { HttpHeader } from './httpHeader';

import { HttpMethod } from '../httpMethod';

export interface HttpDynamicPropertyProviderConfiguration {
  /**
   * The url of the dynamic property provider
   */
  url?: string;
  /**
   * The specification of the dynamic property provider
   */
  specification?: string;
  /**
   * Use the system proxy or not
   */
  useSystemProxy?: boolean;
  method?: HttpMethod;
  headers?: HttpHeader[];
  /**
   * The body of the request
   */
  body?: string;
}

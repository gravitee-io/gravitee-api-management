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
export interface HttpClientOptions {
  /**
   *
   * @type {boolean}
   * @memberof HttpClientOptions
   */
  clearTextUpgrade?: boolean;
  /**
   *
   * @type {number}
   * @memberof HttpClientOptions
   */
  connectTimeout?: number;
  /**
   *
   * @type {boolean}
   * @memberof HttpClientOptions
   */
  followRedirects?: boolean;
  /**
   *
   * @type {number}
   * @memberof HttpClientOptions
   */
  idleTimeout?: number;
  /**
   *
   * @type {boolean}
   * @memberof HttpClientOptions
   */
  keepAlive?: boolean;
  /**
   *
   * @type {number}
   * @memberof HttpClientOptions
   */
  maxConcurrentConnections?: number;
  /**
   *
   * @type {boolean}
   * @memberof HttpClientOptions
   */
  pipelining?: boolean;
  /**
   *
   * @type {boolean}
   * @memberof HttpClientOptions
   */
  propagateClientAcceptEncoding?: boolean;
  /**
   *
   * @type {number}
   * @memberof HttpClientOptions
   */
  readTimeout?: number;
  /**
   *
   * @type {boolean}
   * @memberof HttpClientOptions
   */
  useCompression?: boolean;
  /**
   *
   * @type {string}
   * @memberof HttpClientOptions
   */
  version?: HttpClientOptionsVersionEnum;
}

export const enum HttpClientOptionsVersionEnum {
  _1_1 = 'HTTP_1_1',
  _2 = 'HTTP_2',
}

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

import { ProtocolVersion } from './protocolVersion';

export interface HttpClientOptions {
  /**
   * The idle timeout of the http client in ms
   */
  idleTimeout?: number;
  /**
   * The connect timeout of the http client in ms
   */
  connectTimeout?: number;
  /**
   * The keep alive parameter of the http client
   */
  keepAlive?: boolean;
  /**
   * The read timeout of the http client in ms
   */
  readTimeout?: number;
  /**
   * The pipelining parameter of the http client
   */
  pipelining?: boolean;
  /**
   * The max connections of the http client
   */
  maxConcurrentConnections?: number;
  /**
   * Use compression or not
   */
  useCompression?: boolean;
  /**
   * Propagate the client accept encoding or not
   */
  propagateClientAcceptEncoding?: boolean;
  /**
   * Follow redirects or not
   */
  followRedirects?: boolean;
  /**
   * Clear text upgrade or not
   */
  clearTextUpgrade?: boolean;
  version?: ProtocolVersion;
}

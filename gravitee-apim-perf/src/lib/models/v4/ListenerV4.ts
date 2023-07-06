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
import { EntrypointV4 } from '@models/v4/EntrypointV4';
import { Cors, Logging } from '@models/v3/Proxy';

export type ListenerV4 =
  | ({ type: 'HTTP' } & HttpListenerV4)
  | ({ type: 'SUBSCRIPTION' } & SubscriptionListenerV4)
  | ({ type: 'TCP' } & TcpListenerV4);

export interface HttpListenerV4 {
  /**
   *
   * @type {string}
   * @memberof HttpListenerV4
   */
  type: string;
  /**
   *
   * @type {Array<EntrypointV4>}
   * @memberof HttpListenerV4
   */
  entrypoints: Array<EntrypointV4>;
  /**
   *
   * @type {Array<PathV4>}
   * @memberof HttpListenerV4
   */
  paths: Array<PathV4>;
  /**
   *
   * @type {Array<string>}
   * @memberof HttpListenerV4
   */
  pathMappings?: Array<string>;
  /**
   *
   * @type {Cors}
   * @memberof HttpListenerV4
   */
  cors?: Cors;
  /**
   *
   * @type {Logging}
   * @memberof HttpListenerV4
   */
  logging?: Logging;
}

export interface PathV4 {
  /**
   *
   * @type {string}
   * @memberof PathV4
   */
  host?: string;
  /**
   *
   * @type {boolean}
   * @memberof PathV4
   */
  overrideAccess?: boolean;
  /**
   *
   * @type {string}
   * @memberof PathV4
   */
  path: string;
}

export interface SubscriptionListenerV4 {
  /**
   *
   * @type {string}
   * @memberof SubscriptionListenerV4
   */
  type: string;
  /**
   *
   * @type {Array<EntrypointV4>}
   * @memberof SubscriptionListenerV4
   */
  entrypoints: Array<EntrypointV4>;
}

export interface TcpListenerV4 {
  /**
   *
   * @type {string}
   * @memberof TcpListenerV4
   */
  type: string;
  /**
   *
   * @type {Array<EntrypointV4>}
   * @memberof TcpListenerV4
   */
  entrypoints: Array<EntrypointV4>;
}

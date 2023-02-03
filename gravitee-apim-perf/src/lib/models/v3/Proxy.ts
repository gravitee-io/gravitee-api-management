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
import { EndpointGroup } from '@lib/models/v3/EndpointGroup';

export interface Proxy {
  /**
   *
   * @type {Cors}
   * @memberof Proxy
   */
  cors?: Cors;
  /**
   *
   * @type {Failover}
   * @memberof Proxy
   */
  failover?: Failover;
  /**
   *
   * @type {Array<EndpointGroup>}
   * @memberof Proxy
   */
  groups?: Array<EndpointGroup>;
  /**
   *
   * @type {Logging}
   * @memberof Proxy
   */
  logging?: Logging;
  /**
   *
   * @type {boolean}
   * @memberof Proxy
   */
  preserve_host?: boolean;
  /**
   *
   * @type {boolean}
   * @memberof Proxy
   */
  strip_context_path?: boolean;
  /**
   *
   * @type {Array<VirtualHost>}
   * @memberof Proxy
   */
  virtual_hosts?: Array<VirtualHost>;
}

export interface Cors {
  /**
   *
   * @type {boolean}
   * @memberof Cors
   */
  allowCredentials?: boolean;
  /**
   *
   * @type {Array<string>}
   * @memberof Cors
   */
  allowHeaders?: Array<string>;
  /**
   *
   * @type {Array<string>}
   * @memberof Cors
   */
  allowMethods?: Array<string>;
  /**
   *
   * @type {Array<string>}
   * @memberof Cors
   */
  allowOrigin?: Array<string>;
  /**
   *
   * @type {boolean}
   * @memberof Cors
   */
  enabled?: boolean;
  /**
   *
   * @type {Array<string>}
   * @memberof Cors
   */
  exposeHeaders?: Array<string>;
  /**
   *
   * @type {number}
   * @memberof Cors
   */
  maxAge?: number;
  /**
   *
   * @type {boolean}
   * @memberof Cors
   */
  runPolicies?: boolean;
}

export interface Failover {
  /**
   *
   * @type {Array<string>}
   * @memberof Failover
   */
  cases?: Array<FailoverCasesEnum>;
  /**
   *
   * @type {number}
   * @memberof Failover
   */
  maxAttempts?: number;
  /**
   *
   * @type {number}
   * @memberof Failover
   */
  retryTimeout?: number;
}

export interface Logging {
  /**
   *
   * @type {string}
   * @memberof Logging
   */
  condition?: string;
  /**
   *
   * @type {string}
   * @memberof Logging
   */
  content?: LoggingContentEnum;
  /**
   *
   * @type {string}
   * @memberof Logging
   */
  mode?: LoggingModeEnum;
  /**
   *
   * @type {string}
   * @memberof Logging
   */
  scope?: LoggingScopeEnum;
}

export interface VirtualHost {
  /**
   *
   * @type {string}
   * @memberof VirtualHost
   */
  host?: string;
  /**
   *
   * @type {boolean}
   * @memberof VirtualHost
   */
  override_entrypoint?: boolean;
  /**
   *
   * @type {string}
   * @memberof VirtualHost
   */
  path?: string;
}

export const enum FailoverCasesEnum {
  TIMEOUT = 'TIMEOUT',
}

export const enum LoggingContentEnum {
  NONE = 'NONE',
  HEADERS = 'HEADERS',
  PAYLOADS = 'PAYLOADS',
  HEADERS_PAYLOADS = 'HEADERS_PAYLOADS',
}

/**
 * @export
 */
export const enum LoggingModeEnum {
  NONE = 'NONE',
  CLIENT = 'CLIENT',
  PROXY = 'PROXY',
  CLIENT_PROXY = 'CLIENT_PROXY',
}

/**
 * @export
 */
export const enum LoggingScopeEnum {
  NONE = 'NONE',
  REQUEST = 'REQUEST',
  RESPONSE = 'RESPONSE',
  REQUEST_RESPONSE = 'REQUEST_RESPONSE',
}

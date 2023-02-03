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
import { HttpHeader } from '@lib/models/v3/HttpHeader';
import { HttpClientOptions } from '@lib/models/v3/HttpClientOptions';
import { Services } from '@lib/models/v3/Services';

export interface EndpointGroup {
  /**
   *
   * @type {Array<Endpoint>}
   * @memberof EndpointGroup
   */
  endpoints?: Array<Endpoint>;
  /**
   *
   * @type {Array<HttpHeader>}
   * @memberof EndpointGroup
   */
  headers?: Array<HttpHeader>;
  /**
   *
   * @type {HttpClientOptions}
   * @memberof EndpointGroup
   */
  http?: HttpClientOptions;
  /**
   *
   * @type {LoadBalancer}
   * @memberof EndpointGroup
   */
  load_balancing?: LoadBalancer;
  /**
   *
   * @type {string}
   * @memberof EndpointGroup
   */
  name?: string;
  /**
   *
   * @type {HttpProxy}
   * @memberof EndpointGroup
   */
  proxy?: HttpProxy;
  /**
   *
   * @type {Services}
   * @memberof EndpointGroup
   */
  services?: Services;
  /**
   *
   * @type {HttpClientSslOptions}
   * @memberof EndpointGroup
   */
  ssl?: HttpClientSslOptions;
}

export interface Endpoint {
  /**
   *
   * @type {boolean}
   * @memberof Endpoint
   */
  backup?: boolean;
  /**
   *
   * @type {EndpointHealthCheckService}
   * @memberof Endpoint
   */
  healthcheck?: EndpointHealthCheckService;
  /**
   *
   * @type {boolean}
   * @memberof Endpoint
   */
  inherit?: boolean;
  /**
   *
   * @type {string}
   * @memberof Endpoint
   */
  name?: string;
  /**
   *
   * @type {string}
   * @memberof Endpoint
   */
  target?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof Endpoint
   */
  tenants?: Array<string>;
  /**
   *
   * @type {string}
   * @memberof Endpoint
   */
  type?: string;
  /**
   *
   * @type {number}
   * @memberof Endpoint
   */
  weight?: number;
}

export interface EndpointHealthCheckService {
  /**
   *
   * @type {boolean}
   * @memberof EndpointHealthCheckService
   */
  enabled?: boolean;
  /**
   *
   * @type {boolean}
   * @memberof EndpointHealthCheckService
   */
  inherit?: boolean;
  /**
   *
   * @type {string}
   * @memberof EndpointHealthCheckService
   */
  schedule?: string;
  /**
   *
   * @type {Array<HealthCheckStep>}
   * @memberof EndpointHealthCheckService
   */
  steps?: Array<HealthCheckStep>;
}

export interface HealthCheckStep {
  /**
   *
   * @type {string}
   * @memberof HealthCheckStep
   */
  name?: string;
  /**
   *
   * @type {HealthCheckRequest}
   * @memberof HealthCheckStep
   */
  request?: HealthCheckRequest;
  /**
   *
   * @type {HealthCheckResponse}
   * @memberof HealthCheckStep
   */
  response?: HealthCheckResponse;
}

export interface HealthCheckRequest {
  /**
   *
   * @type {string}
   * @memberof HealthCheckRequest
   */
  body?: string;
  /**
   *
   * @type {boolean}
   * @memberof HealthCheckRequest
   */
  fromRoot?: boolean;
  /**
   *
   * @type {Array<HttpHeader>}
   * @memberof HealthCheckRequest
   */
  headers?: Array<HttpHeader>;
  /**
   *
   * @type {string}
   * @memberof HealthCheckRequest
   */
  method?: HealthCheckRequestMethodEnum;
  /**
   *
   * @type {string}
   * @memberof HealthCheckRequest
   */
  path?: string;
}

export interface HealthCheckResponse {
  /**
   *
   * @type {Array<string>}
   * @memberof HealthCheckResponse
   */
  assertions?: Array<string>;
}

export interface LoadBalancer {
  /**
   *
   * @type {string}
   * @memberof LoadBalancer
   */
  type?: LoadBalancerTypeEnum;
}

export interface HttpProxy {
  /**
   *
   * @type {boolean}
   * @memberof HttpProxy
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof HttpProxy
   */
  host?: string;
  /**
   *
   * @type {string}
   * @memberof HttpProxy
   */
  password?: string;
  /**
   *
   * @type {number}
   * @memberof HttpProxy
   */
  port?: number;
  /**
   *
   * @type {string}
   * @memberof HttpProxy
   */
  type?: HttpProxyTypeEnum;
  /**
   *
   * @type {boolean}
   * @memberof HttpProxy
   */
  useSystemProxy?: boolean;
  /**
   *
   * @type {string}
   * @memberof HttpProxy
   */
  username?: string;
}

export interface HttpClientSslOptions {
  /**
   *
   * @type {boolean}
   * @memberof HttpClientSslOptions
   */
  hostnameVerifier?: boolean;
  /**
   *
   * @type {KeyStore}
   * @memberof HttpClientSslOptions
   */
  keyStore?: KeyStore;
  /**
   *
   * @type {boolean}
   * @memberof HttpClientSslOptions
   */
  trustAll?: boolean;
  /**
   *
   * @type {TrustStore}
   * @memberof HttpClientSslOptions
   */
  trustStore?: TrustStore;
}

export interface KeyStore {
  /**
   *
   * @type {string}
   * @memberof KeyStore
   */
  type?: KeyStoreTypeEnum;
}

export const enum KeyStoreTypeEnum {
  PEM = 'PEM',
  PKCS12 = 'PKCS12',
  JKS = 'JKS',
  NONE = 'None',
}

export interface TrustStore {
  /**
   *
   * @type {string}
   * @memberof TrustStore
   */
  type?: TrustStoreTypeEnum;
}

export const enum TrustStoreTypeEnum {
  PEM = 'PEM',
  PKCS12 = 'PKCS12',
  JKS = 'JKS',
  NONE = 'None',
}
export const enum HealthCheckRequestMethodEnum {
  CONNECT = 'CONNECT',
  DELETE = 'DELETE',
  GET = 'GET',
  HEAD = 'HEAD',
  OPTIONS = 'OPTIONS',
  PATCH = 'PATCH',
  POST = 'POST',
  PUT = 'PUT',
  TRACE = 'TRACE',
  OTHER = 'OTHER',
}
export const enum HttpProxyTypeEnum {
  HTTP = 'HTTP',
  SOCKS4 = 'SOCKS4',
  SOCKS5 = 'SOCKS5',
}

export const enum LoadBalancerTypeEnum {
  ROUND_ROBIN = 'ROUND_ROBIN',
  RANDOM = 'RANDOM',
  WEIGHTED_ROUND_ROBIN = 'WEIGHTED_ROUND_ROBIN',
  WEIGHTED_RANDOM = 'WEIGHTED_RANDOM',
}

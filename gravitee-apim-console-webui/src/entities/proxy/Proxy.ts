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
import { Cors } from '../cors';
import { HealthCheck } from '../health-check';
import { Logging } from '../logging';
import { Services } from '../services';

export interface Proxy {
  virtual_hosts?: ProxyVirtualHost[];
  groups?: ProxyGroup[];
  failover?: ProxyFailover;
  cors?: Cors;
  logging?: Logging;
  strip_context_path?: boolean;
  preserve_host?: boolean;
}

export interface ProxyVirtualHost {
  host?: string;
  path?: string;
  override_entrypoint?: boolean;
}

export interface ProxyConfiguration {
  proxy?: ProxyGroupProxy;
  http?: ProxyGroupHttpClientOptions;
  ssl?: ProxyGroupHttpClientSslOptions;
  headers?: Record<string, string>;
}

export interface ProxyGroup extends ProxyConfiguration {
  name?: string;
  endpoints?: ProxyGroupEndpoint[];
  load_balancing?: ProxyGroupLoadBalancer;
  services?: Services;
}

export interface ProxyGroupEndpoint extends ProxyConfiguration {
  name?: string;
  target?: string;
  weight?: number;
  backup?: boolean;
  tenants?: string[];
  type?: string;
  inherit?: boolean;
  healthcheck?: HealthCheck;
}

export enum ProxyGroupLoadBalancerEnum {
  'ROUND_ROBIN' = 'ROUND_ROBIN',
  'RANDOM' = 'RANDOM',
  'WEIGHTED_ROUND_ROBIN' = 'WEIGHTED_ROUND_ROBIN',
  'WEIGHTED_RANDOM' = 'WEIGHTED_RANDOM',
}
export type ProxyGroupLoadBalancerType = `${ProxyGroupLoadBalancerEnum}`;

export interface ProxyGroupLoadBalancer {
  type: ProxyGroupLoadBalancerType;
}

export interface ProxyGroupProxy {
  enabled: boolean;
  useSystemProxy: boolean;
  host: string;
  port: number;
  username: string;
  password: string;
  type: 'HTTP' | 'SOCKS4' | 'SOCKS5';
}

export interface ProxyGroupHttpClientOptions {
  idleTimeout?: number;
  connectTimeout?: number;
  keepAlive?: boolean;
  readTimeout?: number;
  pipelining?: boolean;
  maxConcurrentConnections?: number;
  useCompression?: boolean;
  followRedirects?: boolean;
  clearTextUpgrade?: boolean;
  version?: 'HTTP_1_1' | 'HTTP_2';
}

export interface ProxyGroupHttpClientSslOptions {
  trustAll?: boolean;
  hostnameVerifier?: boolean;
  trustStore?: {
    type?: 'PEM' | 'PKCS12' | 'JKS';
  };
  Store?: {
    type?: 'PEM' | 'PKCS12' | 'JKS';
  };
}

export interface ProxyFailover {
  maxAttempts?: number;
  retryTimeout?: number;
  cases?: 'TIMEOUT';
}

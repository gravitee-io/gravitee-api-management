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
import { Services } from '../services';

export interface Proxy {
  virtual_hosts?: ProxyVirtualHost[];
  groups?: ProxyGroup[];
  failover?: ProxyFailover;
  cors?: ProxyCors;
  logging?: ProxyLogging;
  strip_context_path?: boolean;
  preserve_host?: boolean;
}

export interface ProxyVirtualHost {
  host?: string;
  path?: string;
  override_entrypoint?: boolean;
}

export interface ProxyGroup {
  name?: string;
  endpoints?: ProxyGroupEndpoint[];
  load_balancing?: ProxyGroupLoadBalancer;
  services?: Services;
  proxy?: ProxyGroupProxy;
  http?: ProxyGroupHttpClientOptions;
  ssl?: ProxyGroupHttpClientSslOptions;
  headers?: Record<string, string>;
}

export interface ProxyGroupEndpoint {
  name?: string;
  target?: string;
  weight?: number;
  backup?: boolean;
  tenants?: string[];
  type?: string;
  inherit?: boolean;
}

export interface ProxyGroupLoadBalancer {
  type: 'ROUND_ROBIN' | 'RANDOM' | 'WEIGHTED_ROUND_ROBIN' | 'WEIGHTED_RANDOM';
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

export interface ProxyCors {
  enabled?: boolean;
  allowOrigin?: string[];
  exposeHeaders?: string[];
  maxAge?: number;
  allowCredentials?: boolean;
  allowMethods?: string[];
  allowHeaders?: string[];
  runPolicies?: boolean;
}

export interface ProxyLogging {
  mode: 'PROXY' | 'CLIENT' | 'PROXY' | 'CLIENT_PROXY';
  scope: 'NONE' | 'REQUEST' | 'RESPONSE' | 'REQUEST_RESPONSE';
  content: 'NONE' | 'HEADERS' | 'PAYLOADS' | 'HEADERS_PAYLOADS';
  condition?: string;
}

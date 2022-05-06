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
import { Endpoint, Services } from './api-endpoints';
import { Flow, FlowMode } from './api-flows';
import { MethodType, Plan, Step } from './plan';
export interface Api {
  proxy: any;
  id: string;
  contextPath: string;
  description: string;
  endpoint: string;
  name: string;
  version: string;
  state: ApiState;
  visibility: ApiVisibility;
  lifecycle_state: ApiLifecycleState;
  created_at: number;
  updated_at: number;
  owner: string;
  workflow_state: string;
  labels?: string[];
}

export interface ApiDefinition {
  id: string;
  description: string;
  proxy: ApiProxy;
  name: string;
  version: string;
  created_at: number;
  updated_at: number;
  owner: string;
}

export interface ApiProxy {
  context_path: string;
  endpoints: ApiEndpoint[];
}

export interface ApiEndpoint {
  name: string;
  target: string;
  inherit: boolean;
}

export interface PortalApi {
  id: string;
  description: string;
  name: string;
  version: string;
  running: boolean;
  public: boolean;
  draft: boolean;
  created_at: number;
  updated_at: number;
  labels: string[];
  entrypoints: string[];
  categories: string[];
}

export enum ApiErrorCodes {
  API_NOT_FOUND = 'errors.api.notFound',
}

export enum ApiVisibility {
  PUBLIC = 'PUBLIC',
  PRIVATE = 'PRIVATE',
}

export enum ApiLifecycleState {
  CREATED = 'CREATED',
  PUBLISHED = 'PUBLISHED',
  UNPUBLISHED = 'UNPUBLISHED',
  DEPRECATED = 'DEPRECATED',
  ARCHIVED = 'ARCHIVED',
}

export enum ApiWorkflowState {
  IN_REVIEW = 'IN_REVIEW',
  REVIEW_OK = 'REVIEW_OK',
}

export enum ApiState {
  STOPPED = 'STOPPED',
  STARTED = 'STARTED',
}

export enum ApiPageType {
  ASCIIDOC = 'ASCIIDOC',
  ASYNCAPI = 'ASYNCAPI',
  MARKDOWN = 'MARKDOWN',
  MARKDOWN_TEMPLATE = 'MARKDOWN_TEMPLATE',
  SWAGGER = 'SWAGGER',
  FOLDER = 'FOLDER',
  LINK = 'LINK',
  ROOT = 'ROOT',
  SYSTEM_FOLDER = 'SYSTEM_FOLDER',
  TRANSLATION = 'TRANSLATION',
}

export enum ApiMetadataFormat {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  MAIL = 'MAIN',
  URL = 'URL',
}

export interface ApiMember {
  id: string;
  reference: string;
  role: string;
}

export interface ApiQualityMetrics {
  score: number;
  metrics_passed: Record<string, boolean>;
}

export interface ApiQualityRule {
  api: string;
  quality_rule: string;
  checked: boolean;
  created_at?: number;
  updated_at?: number;
}

export interface ApiRating {
  rate: number;
}
export interface ApiRatingResponse {
  id: string;
  rate: number;
  api: string;
  user: string;
  userDisplayName: string;
  createdAt: number;
  updateAt: number;
  answers: [];
}

export interface ApiDeployment {
  deploymentLabel: string;
}

export interface Subscription {}

export enum ApiPrimaryOwnerType {
  USER = 'USER',
  GROUP = 'GROUP',
}
export interface UpdateApiEntity {
  name: string;
  version: string;
  description: string;
  contextPath?: string;
  services?: Services;
  resources?: Resource[];
  visibility: ApiVisibility;
  tags?: string[];
  picture?: string;
  categories?: string[];
  labels?: string[];
  groups?: string[];
  metadata?: any;
  background?: string;
  proxy?: Proxy;
  paths?: Rule[];
  flows?: Flow[];
  plans?: Plan[];
  properties?: PropertyEntity[];
  gravitee?: string;
  flow_mode?: FlowMode;
  picture_url?: string;
  path_mappings?: string[];
  response_templates?: {};
  lifecycle_state?: ApiLifecycleState;
  disable_membership_notifications?: boolean;
  background_url?: string;
}

export interface ResponseTemplate {
  status: number;
  headers: {};
  body: any;
}

export interface PropertyEntity {
  key: string;
  value: string;
  dynamic: boolean;
  encrypted: boolean;
  encryptable: boolean;
}

export interface Policy {
  name: string;
  configuration: string;
}

export interface Rule {
  methods: MethodType[];
  policy: Policy;
  description: string;
  enabled: boolean;
}

export interface Proxy {
  virtual_hosts: VirtualHost[];
  groups: EndpointGroup[];
  failover: Failover;
  cors: {};
  logging: {};
  strip_context_path: boolean;
  preserve_host: boolean;
}

export interface Failover {
  maxAttempts: number;
  retryTimeout: number;
  cases: FailoverCase[];
}

export enum FailoverCase {
  TIMEOUT = 'TIMEOUT',
}

export interface EndpointGroup {
  name: string;
  endpoints: Endpoint[];
  load_balancing: LoadBalancer;
  services: Services;
  proxy: HttpProxy;
  http: HttpClientOptions;
  ssl: HttpClientSslOptions;
  headers: HttpHeader[];
}

export interface HttpHeader {
  name: string;
  value: string;
}

export interface HttpClientSslOptions {
  trustAll: boolean;
  hostnameVerifier: boolean;
  trustStore: TrustStore;
  keyStore: KeyStore;
}

export interface KeyStore {
  type: KeyStoreType;
}

export interface TrustStore {
  type: TrustStoreType;
}

export enum KeyStoreType {
  PEM = 'PEM',
  PKCS12 = 'PKCS12',
  JKS = 'JKS',
  None = 'None',
}

export enum TrustStoreType {
  PEM = 'PEM',
  PKCS12 = 'PKCS12',
  JKS = 'JKS',
  None = 'None',
}

export interface HttpClientOptions {
  idleTimeout: number;
  connectTimeout: number;
  keepAlive: boolean;
  readTimeout: number;
  pipelining: boolean;
  maxConcurrentConnections: number;
  useCompression: boolean;
  followRedirects: boolean;
  clearTextUpgrade: boolean;
  version: HttpVersion;
}

export enum HttpVersion {
  HTTP_1_1 = 'HTTP_1_1',
  HTTP_2 = 'HTTP_2',
}

export interface HttpProxy {
  enabled: boolean;
  useSystemProxy: boolean;
  host: string;
  port: number;
  username: string;
  password: string;
  type: HttpProxyType;
}

export enum HttpProxyType {
  HTTP = 'HTTP',
  SOCKS4 = 'SOCKS4',
  SOCKS5 = 'SOCKS5',
}

export interface LoadBalancer {
  type: LoadBalancerType;
}

export enum LoadBalancerType {
  ROUND_ROBIN = 'ROUND_ROBIN',
  RANDOM = 'RANDOM',
  WEIGHTED_ROUND_ROBIN = 'WEIGHTED_ROUND_ROBIN',
  WEIGHTED_RANDOM = 'WEIGHTED_RANDOM',
}

export interface VirtualHost {
  host: string;
  path: string;
  override_entrypoint: boolean;
}

export interface Resource {
  name: string;
  type: string;
  configuration: string;
  enabled: boolean;
}

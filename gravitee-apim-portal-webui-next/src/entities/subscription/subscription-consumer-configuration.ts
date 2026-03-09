/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { SslKeyStore, SslTrustStore } from '../ssl';

export interface SubscriptionConsumerConfiguration {
  entrypointId: string;
  channel: string;
  entrypointConfiguration: WebhookSubscriptionConfiguration;
}

export interface WebhookSubscriptionConfiguration {
  callbackUrl: string;
  headers: Header[];
  auth: WebhookSubscriptionConfigurationAuth;
  ssl: SslOptions;
  retry: RetryConfiguration;
}

export interface Header {
  name: string;
  value: string;
}

export type WebhookSubscriptionConfigurationAuthType = 'none' | 'basic' | 'token' | 'oauth2';

export interface WebhookSubscriptionConfigurationAuth {
  type: WebhookSubscriptionConfigurationAuthType;
  basic?: BasicAuthConfiguration;
  token?: TokenAuthConfiguration;
  oauth2?: Oauth2AuthConfiguration;
}

export interface BasicAuthConfiguration {
  username: string;
  password: string;
}

export interface TokenAuthConfiguration {
  value: string;
}

export interface Oauth2AuthConfiguration {
  endpoint: string;
  clientId: string;
  clientSecret: string;
  scopes?: string[];
}

export interface SslOptions {
  hostnameVerifier?: boolean;
  trustAll?: boolean;
  trustStore?: SslTrustStore;
  keyStore?: SslKeyStore;
}

export const RetryOptions = ['No Retry', 'Retry On Fail'] as const;
export type RetryOptionsType = (typeof RetryOptions)[number];

export const RetryStrategies = ['LINEAR', 'EXPONENTIAL'];
export type RetryStrategiesType = (typeof RetryStrategies)[number];

export interface RetryConfiguration {
  retryOption: RetryOptionsType;
  retryStrategy?: RetryStrategiesType;
  maxAttempts?: number;
  initialDelaySeconds?: number;
  maxDelaySeconds?: number;
}

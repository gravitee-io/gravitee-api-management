/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const PROVIDER_LABELS: Record<string, string> = {
    A2A: 'A2A Protocol',
    'aws-api-gateway': 'AWS API Gateway',
    solace: 'Solace',
    apigee: 'Apigee',
    'azure-api-management': 'Azure API Management',
    'ibm-api-connect': 'IBM API Connect',
    'confluent-platform': 'Confluent Platform',
    mulesoft: 'MuleSoft',
    'edge-stack': 'Edge Stack',
};

export const SUPPORTED_PROVIDER_TOKENS: readonly string[] = Object.keys(PROVIDER_LABELS);

export function hasProviderLabel(provider: string): boolean {
    return Object.prototype.hasOwnProperty.call(PROVIDER_LABELS, provider);
}

export function integrationProviderLabel(provider: string): string {
    return hasProviderLabel(provider) ? PROVIDER_LABELS[provider] : provider;
}

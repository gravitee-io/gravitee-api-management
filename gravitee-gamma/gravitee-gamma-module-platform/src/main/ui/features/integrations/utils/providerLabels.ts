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
import { A2A_PROVIDER } from './integrationKind';

export type ProviderGroup = 'API gateways' | 'Event brokers';

export interface ProviderCatalogEntry {
    token: string;
    label: string;
    group: ProviderGroup;
    description: string;
    monogram: string;
}

export const PROVIDER_GROUPS_IN_ORDER: readonly ProviderGroup[] = ['API gateways', 'Event brokers'];

const UNSORTED_PROVIDER_CATALOG: readonly ProviderCatalogEntry[] = [
    { token: 'apigee', label: 'Apigee', group: 'API gateways', description: 'API proxies from a Google Cloud org', monogram: 'AP' },
    {
        token: 'aws-api-gateway',
        label: 'AWS API Gateway',
        group: 'API gateways',
        description: 'REST and HTTP APIs from an AWS account',
        monogram: 'AWS',
    },
    {
        token: 'azure-api-management',
        label: 'Azure API Management',
        group: 'API gateways',
        description: 'APIs from an Azure APIM instance',
        monogram: 'AZ',
    },
    {
        token: 'edge-stack',
        label: 'Edge Stack',
        group: 'API gateways',
        description: 'Mappings from an Edge Stack cluster',
        monogram: 'ES',
    },
    {
        token: 'ibm-api-connect',
        label: 'IBM API Connect',
        group: 'API gateways',
        description: 'APIs from an API Connect catalog',
        monogram: 'IBM',
    },
    { token: 'mulesoft', label: 'MuleSoft', group: 'API gateways', description: 'APIs from Anypoint Platform', monogram: 'MS' },
    {
        token: 'sap-api-management',
        label: 'SAP Business Technology Platform',
        group: 'API gateways',
        description: 'APIs from an SAP API Management tenant',
        monogram: 'SAP',
    },
    {
        token: 'confluent-platform',
        label: 'Confluent Platform',
        group: 'Event brokers',
        description: 'Topics from a Confluent cluster',
        monogram: 'CF',
    },
    { token: 'solace', label: 'Solace', group: 'Event brokers', description: 'Event APIs from a Solace broker', monogram: 'SO' },
];

export const PROVIDER_CATALOG: readonly ProviderCatalogEntry[] = [...UNSORTED_PROVIDER_CATALOG].sort((a, b) =>
    a.label.localeCompare(b.label),
);

export const SUPPORTED_PROVIDER_TOKENS: readonly string[] = PROVIDER_CATALOG.map(entry => entry.token);

export function findProvider(provider: string): ProviderCatalogEntry | undefined {
    return PROVIDER_CATALOG.find(entry => entry.token === provider);
}

const A2A_PROVIDER_LABEL = 'A2A Protocol';

export function hasProviderLabel(provider: string): boolean {
    return provider === A2A_PROVIDER || findProvider(provider) !== undefined;
}

export function integrationProviderLabel(provider: string): string {
    return provider === A2A_PROVIDER ? A2A_PROVIDER_LABEL : (findProvider(provider)?.label ?? provider);
}

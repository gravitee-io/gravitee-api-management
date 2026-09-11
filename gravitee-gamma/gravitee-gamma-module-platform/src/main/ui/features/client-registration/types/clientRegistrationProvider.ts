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

export type InitialAccessTokenType = 'INITIAL_ACCESS_TOKEN' | 'CLIENT_CREDENTIALS';

export type RenewClientSecretMethod = 'POST' | 'PATCH' | 'PUT';

export type StoreType = 'NONE' | 'JKS' | 'PKCS12';

export interface CryptoStore {
    type: StoreType;
    password?: string;
    path?: string | null;
    content?: string | null;
    alias?: string;
    keyPassword?: string;
}

export interface ClientRegistrationProvider {
    id: string;
    name: string;
    description?: string;
    discovery_endpoint: string;
    initial_access_token_type: InitialAccessTokenType;
    client_id?: string;
    client_secret?: string;
    scopes?: string[];
    initial_access_token?: string;
    renew_client_secret_support?: boolean;
    renew_client_secret_endpoint?: string;
    renew_client_secret_method?: string;
    software_id?: string;
    updated_at?: number;
    claim_mappings?: Record<string, string>;
    trust_store?: CryptoStore;
    key_store?: CryptoStore;
}

/** POST /providers body — Classic `create(clientRegistrationProvider)` without an id. */
export interface ClientRegistrationProviderWrite {
    name: string;
    description?: string;
    discovery_endpoint: string;
    initial_access_token_type: InitialAccessTokenType;
    client_id?: string;
    client_secret?: string;
    scopes?: string[];
    initial_access_token?: string;
    renew_client_secret_support?: boolean;
    renew_client_secret_endpoint?: string;
    renew_client_secret_method?: string;
    software_id?: string;
    claim_mappings?: Record<string, string>;
    trust_store?: CryptoStore;
    key_store?: CryptoStore;
}

/**
 * PUT body field list from Classic `ClientRegistrationProvidersService.update`.
 * Do not send `id` in the JSON body.
 */
export const CLIENT_REGISTRATION_PROVIDER_UPDATE_FIELDS = [
    'name',
    'description',
    'discovery_endpoint',
    'initial_access_token_type',
    'client_id',
    'client_secret',
    'scopes',
    'initial_access_token',
    'renew_client_secret_support',
    'renew_client_secret_endpoint',
    'renew_client_secret_method',
    'software_id',
    'claim_mappings',
    'trust_store',
    'key_store',
] as const satisfies ReadonlyArray<keyof ClientRegistrationProviderWrite>;

export function toClientRegistrationProviderUpdateBody(provider: ClientRegistrationProviderWrite): ClientRegistrationProviderWrite {
    return {
        name: provider.name,
        description: provider.description,
        discovery_endpoint: provider.discovery_endpoint,
        initial_access_token_type: provider.initial_access_token_type,
        client_id: provider.client_id,
        client_secret: provider.client_secret,
        scopes: provider.scopes,
        initial_access_token: provider.initial_access_token,
        renew_client_secret_support: provider.renew_client_secret_support,
        renew_client_secret_endpoint: provider.renew_client_secret_endpoint,
        renew_client_secret_method: provider.renew_client_secret_method,
        software_id: provider.software_id,
        claim_mappings: provider.claim_mappings,
        trust_store: provider.trust_store,
        key_store: provider.key_store,
    };
}

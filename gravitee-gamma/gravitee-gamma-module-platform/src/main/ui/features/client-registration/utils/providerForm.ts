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

import { claimMappingsToDictionary, dictionaryToClaimMappings, type ProviderForm, type StoreForm } from './validateProviderForm';
import type { ClientRegistrationProvider, ClientRegistrationProviderWrite, CryptoStore } from '../types/clientRegistrationProvider';

export const EMPTY_STORE_FORM: StoreForm = {
    type: 'NONE',
    pathOrContent: 'PATH',
    password: '',
    path: '',
    content: '',
    alias: '',
    keyPassword: '',
};

export const EMPTY_PROVIDER_FORM: ProviderForm = {
    name: '',
    description: '',
    discovery_endpoint: '',
    initial_access_token_type: '',
    client_id: '',
    client_secret: '',
    scopes: [],
    software_id: '',
    initial_access_token: '',
    renew_client_secret_support: false,
    renew_client_secret_method: '',
    renew_client_secret_endpoint: '',
    claim_mappings: [],
    trust_store: { ...EMPTY_STORE_FORM },
    key_store: { ...EMPTY_STORE_FORM },
};

export function storeToForm(store: CryptoStore | undefined): StoreForm {
    const type = store?.type ?? 'NONE';
    return {
        type,
        pathOrContent: store?.content ? 'CONTENT' : 'PATH',
        password: store?.password ?? '',
        path: store?.path ?? '',
        content: store?.content ?? '',
        alias: store?.alias ?? '',
        keyPassword: store?.keyPassword ?? '',
    };
}

export function formToStore(form: StoreForm, withKeyFields: boolean): CryptoStore {
    if (form.type === 'NONE') return { type: 'NONE' };
    return {
        type: form.type,
        password: form.password,
        path: form.pathOrContent === 'PATH' ? form.path : null,
        content: form.pathOrContent === 'CONTENT' ? form.content : null,
        ...(withKeyFields ? { alias: form.alias, keyPassword: form.keyPassword } : {}),
    };
}

export function providerToForm(provider: ClientRegistrationProvider): ProviderForm {
    return {
        name: provider.name,
        description: provider.description ?? '',
        discovery_endpoint: provider.discovery_endpoint,
        initial_access_token_type: provider.initial_access_token_type,
        client_id: provider.client_id ?? '',
        client_secret: provider.client_secret ?? '',
        scopes: [...(provider.scopes ?? [])],
        software_id: provider.software_id ?? '',
        initial_access_token: provider.initial_access_token ?? '',
        renew_client_secret_support: provider.renew_client_secret_support ?? false,
        renew_client_secret_method: (provider.renew_client_secret_method as ProviderForm['renew_client_secret_method']) || '',
        renew_client_secret_endpoint: provider.renew_client_secret_endpoint ?? '',
        claim_mappings: dictionaryToClaimMappings(provider.claim_mappings),
        trust_store: storeToForm(provider.trust_store),
        key_store: storeToForm(provider.key_store),
    };
}

export function formToWrite(form: ProviderForm): ClientRegistrationProviderWrite {
    return {
        name: form.name.trim(),
        description: form.description.trim(),
        discovery_endpoint: form.discovery_endpoint.trim(),
        initial_access_token_type: form.initial_access_token_type as ClientRegistrationProviderWrite['initial_access_token_type'],
        client_id: form.client_id.trim(),
        client_secret: form.client_secret.trim(),
        scopes: form.scopes,
        software_id: form.software_id.trim(),
        initial_access_token: form.initial_access_token.trim(),
        renew_client_secret_support: form.renew_client_secret_support,
        renew_client_secret_method: form.renew_client_secret_method,
        renew_client_secret_endpoint: form.renew_client_secret_endpoint.trim(),
        claim_mappings: claimMappingsToDictionary(form.claim_mappings),
        trust_store: formToStore(form.trust_store, false),
        key_store: formToStore(form.key_store, true),
    };
}

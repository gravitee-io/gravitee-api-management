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

export const PROVIDER_NAME_MIN = 3;
export const PROVIDER_NAME_MAX = 50;

export type PathOrContent = 'PATH' | 'CONTENT';

export interface ClaimMappingRow {
    key: string;
    value: string;
}

export interface StoreForm {
    type: 'NONE' | 'JKS' | 'PKCS12';
    pathOrContent: PathOrContent;
    password: string;
    path: string;
    content: string;
    alias: string;
    keyPassword: string;
}

export interface ProviderForm {
    name: string;
    description: string;
    discovery_endpoint: string;
    initial_access_token_type: '' | 'INITIAL_ACCESS_TOKEN' | 'CLIENT_CREDENTIALS';
    client_id: string;
    client_secret: string;
    scopes: string[];
    software_id: string;
    initial_access_token: string;
    renew_client_secret_support: boolean;
    renew_client_secret_method: '' | 'POST' | 'PATCH' | 'PUT';
    renew_client_secret_endpoint: string;
    claim_mappings: ClaimMappingRow[];
    trust_store: StoreForm;
    key_store: StoreForm;
}

export function validateProviderForm(form: ProviderForm): Record<string, string> {
    const errors: Record<string, string> = {};
    const name = form.name.trim();
    if (name === '') errors.name = 'This field is required.';
    else if (name.length < PROVIDER_NAME_MIN) errors.name = 'Name has to be more than 3 characters long';
    else if (name.length > PROVIDER_NAME_MAX) errors.name = 'Name has to be less than 50 characters long.';
    if (form.discovery_endpoint.trim() === '') errors.discovery_endpoint = 'This field is required.';
    if (form.initial_access_token_type === '') errors.initial_access_token_type = 'This field is required.';
    if (form.initial_access_token_type === 'CLIENT_CREDENTIALS') {
        if (form.client_id.trim() === '') errors.client_id = 'This field is required.';
        if (form.client_secret.trim() === '') errors.client_secret = 'This field is required.';
    }
    if (form.initial_access_token_type === 'INITIAL_ACCESS_TOKEN') {
        if (form.initial_access_token.trim() === '') errors.initial_access_token = 'This field is required.';
    }
    validateStore(form.trust_store, 'trust', errors);
    validateStore(form.key_store, 'key', errors);

    const keys = form.claim_mappings.map(row => row.key);
    if (keys.length !== new Set(keys).size) {
        errors.claim_mappings = 'Claim names must be unique';
    } else if (form.claim_mappings.some(row => row.key.trim() === '' || row.value.trim() === '')) {
        errors.claim_mappings = 'Every mapping needs both a claim name and a registration request field';
    }
    return errors;
}

function validateStore(store: StoreForm, prefix: 'trust' | 'key', errors: Record<string, string>) {
    if (store.type === 'NONE') return;
    if (store.password === '') errors[`${prefix}_password`] = 'Password is required';
    if (store.pathOrContent === 'PATH' && store.path.trim() === '') {
        errors[`${prefix}_path`] = 'This field is required.';
    }
    if (store.pathOrContent === 'CONTENT' && store.content.trim() === '') {
        errors[`${prefix}_content`] = 'This field is required.';
    }
}

export function claimMappingsToDictionary(rows: ClaimMappingRow[]): Record<string, string> {
    return rows.reduce<Record<string, string>>((acc, { key, value }) => ({ ...acc, [key]: value }), {});
}

export function dictionaryToClaimMappings(record: Record<string, string> | undefined): ClaimMappingRow[] {
    if (record === undefined) return [];
    return Object.keys(record).map(key => ({ key, value: record[key] ?? '' }));
}

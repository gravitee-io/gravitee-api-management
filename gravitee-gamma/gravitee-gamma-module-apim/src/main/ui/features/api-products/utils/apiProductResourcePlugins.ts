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

// Mirrors io.gravitee.apim.infra.domain_service.api_product.ApiProductResourceValidationDomainServiceImpl.ALLOWED_TYPES.
export const API_PRODUCT_RESOURCE_PLUGIN_IDS: ReadonlySet<string> = new Set([
    'cache',
    'cache-redis',
    'oauth2',
    'oauth2-am-resource',
    'oauth2-auth0',
    'oauth2-entra-id',
    'oauth2-keycloak-resource',
    'auth-provider-http-resource',
    'auth-provider-inline-resource',
    'auth-provider-ldap-resource',
    'content-provider-inline-resource',
]);

export function isApiProductResourcePlugin(pluginId: string): boolean {
    return API_PRODUCT_RESOURCE_PLUGIN_IDS.has(pluginId);
}

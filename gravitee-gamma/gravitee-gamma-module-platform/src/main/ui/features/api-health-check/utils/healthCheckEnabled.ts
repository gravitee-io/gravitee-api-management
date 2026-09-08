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

export interface HealthCheckServiceFlag {
    readonly enabled?: boolean;
}

export interface HealthCheckEndpoint {
    readonly services?: { readonly healthCheck?: HealthCheckServiceFlag };
}

export interface HealthCheckEndpointGroup {
    readonly services?: { readonly healthCheck?: HealthCheckServiceFlag };
    readonly endpoints?: readonly HealthCheckEndpoint[];
}

export function healthCheckEnabled(api: { readonly endpointGroups?: readonly HealthCheckEndpointGroup[] }): boolean {
    return (
        api.endpointGroups?.some(
            group => group.services?.healthCheck?.enabled || group.endpoints?.some(endpoint => endpoint.services?.healthCheck?.enabled),
        ) ?? false
    );
}

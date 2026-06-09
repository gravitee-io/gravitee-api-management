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
import { type AmConfig, moduleBaseUrl } from './am-config';
import { amManagementApi } from './am-management-api-client';
import type {
    AmConnectionRequest,
    AmConnectionTestResult,
    AmConnectionView,
    AmDomain,
    AmEnvironment,
    AmGatewayEntrypoint,
} from './am-management-api.types';

const base = (cfg: AmConfig) => moduleBaseUrl(cfg);

export const amManagementApiService = {
    listEnvironments: (cfg: AmConfig): Promise<AmEnvironment[]> => amManagementApi.get<AmEnvironment[]>(base(cfg), '/environments'),

    listDomains: (cfg: AmConfig, envId: string, q?: string): Promise<AmDomain[]> => {
        const suffix = q ? `?q=${encodeURIComponent(q)}` : '';
        return amManagementApi.get<AmDomain[]>(base(cfg), `/environments/${encodeURIComponent(envId)}/domains${suffix}`);
    },

    getDomain: (cfg: AmConfig, envId: string, domainId: string): Promise<AmDomain> =>
        amManagementApi.get<AmDomain>(base(cfg), `/environments/${encodeURIComponent(envId)}/domains/${encodeURIComponent(domainId)}`),

    listDomainEntrypoints: (cfg: AmConfig, envId: string, domainId: string): Promise<AmGatewayEntrypoint[]> =>
        amManagementApi.get<AmGatewayEntrypoint[]>(
            base(cfg),
            `/environments/${encodeURIComponent(envId)}/domains/${encodeURIComponent(domainId)}/entrypoints`,
        ),

    getAmConnection: (cfg: AmConfig): Promise<AmConnectionView> => amManagementApi.get<AmConnectionView>(base(cfg), '/am-config'),

    saveAmConnection: (cfg: AmConfig, payload: AmConnectionRequest): Promise<AmConnectionView> =>
        amManagementApi.put<AmConnectionView>(base(cfg), '/am-config', payload),

    deleteAmConnection: (cfg: AmConfig): Promise<void> => amManagementApi.delete<void>(base(cfg), '/am-config'),

    testAmConnection: (cfg: AmConfig, payload: AmConnectionRequest): Promise<AmConnectionTestResult> =>
        amManagementApi.post<AmConnectionTestResult>(base(cfg), '/am-config/_test', payload),
};

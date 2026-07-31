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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useMutation } from '@tanstack/react-query';

import { ApimApiError } from '../../../shared/api/apimClient';
import { createApiPlan, createApiProxy, publishApiPlan, startApiProxy } from '../services/apiProxy';
import { updateApiResources } from '../services/resources';
import type { ApiProxyCreated } from '../types';
import type { ApiProxyDraft } from '../types/apiCreation';
import { buildApiResources, mapFormToCreateRequest, mapFormToPlanRequest } from '../utils/apiProxyMapper';
import { apiProxyKeys } from '../utils/queryKeys';

export type { ApiProxyCreated };

async function failWith<T>(action: Promise<T>, message: string): Promise<T> {
    try {
        return await action;
    } catch (err) {
        throw new ApimApiError(err instanceof ApimApiError ? err.status : 0, message);
    }
}

export function useCreateApiProxy() {
    const env = useEnvironment();

    return useMutation<ApiProxyCreated, ApimApiError, ApiProxyDraft>({
        mutationKey: apiProxyKeys.create(),
        mutationFn: async (form: ApiProxyDraft) => {
            if (!env) throw new ApimApiError(0, 'Environment not ready');
            const { id: environmentId } = env;

            let created: ApiProxyCreated;
            try {
                created = await createApiProxy(environmentId, mapFormToCreateRequest(form));
            } catch (err) {
                throw new ApimApiError(
                    err instanceof ApimApiError ? err.status : 0,
                    err instanceof ApimApiError ? err.message : 'Failed to create the API. Please check your details and try again.',
                );
            }

            const resources = buildApiResources(form);
            if (resources.length > 0) {
                await failWith(
                    updateApiResources(environmentId, created.id, resources),
                    `API "${created.name}" was created but the OAuth2 resource could not be configured. Open the API to finish setup.`,
                );
            }

            const plan = await failWith(
                createApiPlan(environmentId, created.id, mapFormToPlanRequest(form)),
                `API "${created.name}" was created but plan creation failed. Open the API to configure a plan manually.`,
            );

            await failWith(
                publishApiPlan(environmentId, created.id, plan.id),
                `API "${created.name}" was created but the plan could not be published. Open the API to publish the plan.`,
            );

            if (form.deployImmediately) {
                await failWith(
                    startApiProxy(environmentId, created.id),
                    `API "${created.name}" was created successfully but could not be started. Start it from the API detail page.`,
                );
            }

            return created;
        },
    });
}

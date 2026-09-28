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
import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import { createApiPlan, createApiProxy, publishApiPlan, startApiProxy } from '../services/apiProxy';
import { askApiReview } from '../services/apiReview';
import { updateApiResources } from '../services/resources';
import type { ApiProxyCreated } from '../types';
import type { ApiProxyDraft } from '../types/apiCreation';
import { buildApiResources, mapFormToCreateRequest, mapFormToPlanRequest } from '../utils/apiProxyMapper';
import { apiProxyKeys } from '../utils/queryKeys';

export type { ApiProxyCreated };

export interface ApiProxyCreationResult {
    /** What `POST /apis` returned. The API exists on the server even when a later step failed. */
    readonly api: ApiProxyCreated;
    /** One entry per post-creation step that failed; empty when every step succeeded. */
    readonly warnings: string[];
}

/**
 * Runs a step that happens after the API already exists. A failure is recorded and the remaining
 * steps still run: aborting here would silently skip work the user explicitly asked for, such as
 * the review request behind "Create & ask for review".
 */
async function runStep<T>(action: Promise<T>, warnings: string[], message: string): Promise<T | undefined> {
    try {
        return await action;
    } catch (err) {
        warnings.push(`${message} (${extractErrorMessage(err, 'no reason given')})`);
        return undefined;
    }
}

export function useCreateApiProxy() {
    const env = useEnvironment();

    return useMutation<ApiProxyCreationResult, ApimApiError, ApiProxyDraft>({
        mutationKey: apiProxyKeys.create(),
        mutationFn: async (form: ApiProxyDraft): Promise<ApiProxyCreationResult> => {
            if (!env) throw new ApimApiError(0, 'Environment not ready');
            const { id: environmentId } = env;

            // Only this call may reject: until it succeeds nothing exists, so retrying is safe.
            let api: ApiProxyCreated;
            try {
                api = await createApiProxy(environmentId, mapFormToCreateRequest(form));
            } catch (err) {
                throw new ApimApiError(
                    err instanceof ApimApiError ? err.status : 0,
                    err instanceof ApimApiError ? err.message : 'Failed to create the API. Please check your details and try again.',
                );
            }

            const warnings: string[] = [];

            const resources = buildApiResources(form);
            if (resources.length > 0) {
                await runStep(
                    updateApiResources(environmentId, api.id, resources),
                    warnings,
                    'The OAuth2 resource could not be configured. Open the API to finish the resource setup.',
                );
            }

            const plan = await runStep(
                createApiPlan(environmentId, api.id, mapFormToPlanRequest(form)),
                warnings,
                'The plan could not be created. Open the API to add a plan.',
            );

            // No plan means nothing to publish, and the warning above already says so.
            if (plan) {
                await runStep(
                    publishApiPlan(environmentId, api.id, plan.id),
                    warnings,
                    'The plan could not be published. Open the API to publish the plan.',
                );
            }

            // With API Review on, the API cannot start until a reviewer accepts it, so asking replaces deploying.
            if (form.askForReview) {
                await runStep(
                    askApiReview(environmentId, api.id),
                    warnings,
                    'The review could not be requested. Ask for a review from the API General page.',
                );
            } else if (form.deployImmediately) {
                await runStep(
                    startApiProxy(environmentId, api.id),
                    warnings,
                    'The API could not be started. Start it from the API detail page.',
                );
            }

            return { api, warnings };
        },
    });
}

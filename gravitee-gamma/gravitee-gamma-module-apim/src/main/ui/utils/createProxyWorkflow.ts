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
import { buildCreateApiV4 } from './buildCreateApiV4';
import { buildCreatePlansV4 } from './buildCreatePlanV4';
import type { ApimRuntimeConfig } from '../core/context/apimRuntimeContext';
import { ApimHttpError } from '../core/http/apimFetch';
import type { ApiV4Dto, ProxyConnectorBootstrap } from '../features/apis/types/api.types';
import type { ApiCreationState } from '../features/apis/types/models';
import { createApiV4, startApi } from '../services/apis/apis';
import { createPlanV4, publishPlanV4 } from '../services/plans';
import { askForApiReview } from '../services/reviews';

export type CreateProxyWorkflowResult = {
    api: ApiV4Dto;
    deployed: boolean;
    warnings: string[];
};

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
}

export function getApimErrorMessage(err: unknown): string {
    if (err instanceof ApimHttpError) return err.message;
    if (isRecord(err) && typeof err.message === 'string') return err.message;
    if (err instanceof Error) return err.message;
    return 'An unexpected error occurred.';
}

/**
 * Orchestrates create → plans (create + publish) → optional start → optional review.
 * Matches Angular semantics: API create is blocking; later steps collect warnings.
 */
export async function runCreateProxyWorkflow(
    runtime: ApimRuntimeConfig,
    data: ApiCreationState,
    bootstrap: ProxyConnectorBootstrap,
    options?: { askForReview?: boolean },
): Promise<CreateProxyWorkflowResult> {
    const warnings: string[] = [];
    const createBody = buildCreateApiV4(data, bootstrap);
    const api = await createApiV4(runtime, createBody);

    const plans = buildCreatePlansV4(data);
    for (const plan of plans) {
        try {
            const created = await createPlanV4(runtime, api.id, plan);
            if (!created.id) {
                warnings.push(`Plan "${plan.name}" was created but returned no id.`);
                continue;
            }
            try {
                await publishPlanV4(runtime, api.id, created.id);
            } catch (e) {
                warnings.push(`Error while publishing plan "${plan.name}": ${getApimErrorMessage(e)}.`);
            }
        } catch (e) {
            warnings.push(`Error while creating plan "${plan.name}": ${getApimErrorMessage(e)}.`);
        }
    }

    let deployed = false;
    if (data.deployImmediately) {
        try {
            await startApi(runtime, api.id);
            deployed = true;
        } catch (e) {
            warnings.push(`Error while starting API: ${getApimErrorMessage(e)}.`);
        }
    }

    if (options?.askForReview) {
        try {
            await askForApiReview(runtime, api.id);
        } catch (e) {
            warnings.push(`Error while asking for review: ${getApimErrorMessage(e)}.`);
        }
    }

    return { api, deployed, warnings };
}

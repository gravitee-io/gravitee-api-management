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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { askApiReview, submitApiReview, type SubmitApiReviewInput } from '../services/apiReview';
import { apiDetailKeys, apiReviewKeys } from '../utils/queryKeys';

function useInvalidateApiReviewState(apiId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return () => {
        if (!env?.id || !apiId) return;
        void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env.id, apiId) });
        void queryClient.invalidateQueries({ queryKey: apiReviewKeys.apiQualityRules(env.id, apiId) });
    };
}

/** Author side: moves the API to IN_REVIEW. */
export function useAskApiReview(apiId: string | undefined) {
    const env = useEnvironment();
    const invalidate = useInvalidateApiReviewState(apiId);

    return useMutation({
        mutationFn: () => askApiReview(env!.id, apiId!),
        onSuccess: invalidate,
    });
}

/** Reviewer side: records the ticked manual rules, then accepts or rejects. */
export function useSubmitApiReview(apiId: string | undefined) {
    const env = useEnvironment();
    const invalidate = useInvalidateApiReviewState(apiId);

    return useMutation({
        mutationFn: (input: SubmitApiReviewInput) => submitApiReview(env!.id, apiId!, input),
        onSuccess: invalidate,
    });
}

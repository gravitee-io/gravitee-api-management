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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { createQualityRule, deleteQualityRule, updateQualityRule } from '../services/qualityRules';
import type { QualityRule, QualityRuleWrite } from '../types/qualityRule';
import { qualityRuleKeys } from '../utils/queryKeys';

function useQualityRuleMutation<TData>(mutationFn: (envId: string, data: TData) => Promise<unknown>) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TData) => mutationFn(env!.id, data),
        onSuccess: () => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: qualityRuleKeys.list(env.id) });
            }
        },
    });
}

export function useCreateQualityRule() {
    return useQualityRuleMutation<QualityRuleWrite>(createQualityRule);
}

export function useUpdateQualityRule() {
    return useQualityRuleMutation<{ rule: QualityRule; write: QualityRuleWrite }>((envId, { rule, write }) =>
        updateQualityRule(envId, rule, write),
    );
}

export function useDeleteQualityRule() {
    return useQualityRuleMutation<string>(deleteQualityRule);
}

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

import { ApimApiError } from '../../../shared/api/apimClient';
import { deleteScoringRuleset, importScoringRuleset, updateScoringRuleset } from '../services/apiScore';
import type { ImportRulesetRequest, UpdateRulesetRequest } from '../types/apiScore';
import { apiScoreKeys } from '../utils/queryKeys';

function requireEnvId(env: { id: string } | null | undefined): string {
    if (!env) throw new ApimApiError(0, 'Environment not ready');
    return env.id;
}

export function useImportRuleset() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<void, ApimApiError, ImportRulesetRequest>({
        mutationFn: request => importScoringRuleset(requireEnvId(env), request),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiScoreKeys.rulesets(env?.id ?? '') }),
    });
}

export function useUpdateRuleset() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<unknown, ApimApiError, { rulesetId: string; request: UpdateRulesetRequest }>({
        mutationFn: ({ rulesetId, request }) => updateScoringRuleset(requireEnvId(env), rulesetId, request),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiScoreKeys.rulesets(env?.id ?? '') }),
    });
}

export function useDeleteRuleset() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<void, ApimApiError, string>({
        mutationFn: rulesetId => deleteScoringRuleset(requireEnvId(env), rulesetId),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiScoreKeys.rulesets(env?.id ?? '') }),
    });
}

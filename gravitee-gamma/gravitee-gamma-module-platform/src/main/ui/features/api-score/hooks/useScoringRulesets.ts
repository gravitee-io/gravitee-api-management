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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import {
    createScoringRuleset,
    deleteScoringRuleset,
    getScoringRuleset,
    listScoringRulesets,
    updateScoringRuleset,
} from '../services/scoringRulesets';
import type { CreateRulesetRequest, EditRulesetRequest } from '../types/rulesets';
import { environmentScoringKeys } from '../utils/queryKeys';

export function useScoringRulesets() {
    const env = useEnvironment();
    const result = useQuery({
        queryKey: environmentScoringKeys.rulesets(env?.id ?? ''),
        queryFn: () => listScoringRulesets(env!.id),
        enabled: Boolean(env?.id),
    });

    return {
        rulesets: result.data ?? [],
        isLoading: result.isLoading,
        isError: result.isError,
        error: result.error,
    };
}

export function useScoringRuleset(rulesetId: string | undefined) {
    const env = useEnvironment();
    return useQuery({
        queryKey: environmentScoringKeys.ruleset(env?.id ?? '', rulesetId ?? ''),
        queryFn: () => getScoringRuleset(env!.id, rulesetId!),
        enabled: Boolean(env?.id && rulesetId),
    });
}

export function useCreateScoringRuleset() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (body: CreateRulesetRequest) => createScoringRuleset(env!.id, body),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: environmentScoringKeys.rulesets(env?.id ?? '') }),
    });
}

export function useUpdateScoringRuleset(rulesetId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (body: EditRulesetRequest) => updateScoringRuleset(env!.id, rulesetId, body),
        onSuccess: updated => {
            queryClient.setQueryData(environmentScoringKeys.ruleset(env?.id ?? '', rulesetId), updated);
            queryClient.invalidateQueries({ queryKey: environmentScoringKeys.rulesets(env?.id ?? '') });
        },
    });
}

export function useDeleteScoringRuleset() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (rulesetId: string) => deleteScoringRuleset(env!.id, rulesetId),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: environmentScoringKeys.rulesets(env?.id ?? '') }),
    });
}

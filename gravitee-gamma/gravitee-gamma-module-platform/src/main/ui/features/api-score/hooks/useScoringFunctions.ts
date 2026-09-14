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

import { createScoringFunction, deleteScoringFunction, listScoringFunctions } from '../services/scoringFunctions';
import type { CreateFunctionRequest } from '../types/rulesets';
import { environmentScoringKeys } from '../utils/queryKeys';

export function useScoringFunctions() {
    const env = useEnvironment();
    const result = useQuery({
        queryKey: environmentScoringKeys.functions(env?.id ?? ''),
        queryFn: () => listScoringFunctions(env!.id),
        enabled: Boolean(env?.id),
    });

    return {
        functions: result.data ?? [],
        isLoading: result.isLoading,
        isError: result.isError,
        error: result.error,
    };
}

export function useCreateScoringFunction() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (body: CreateFunctionRequest) => createScoringFunction(env!.id, body),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: environmentScoringKeys.functions(env?.id ?? '') }),
    });
}

export function useDeleteScoringFunction() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (functionName: string) => deleteScoringFunction(env!.id, functionName),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: environmentScoringKeys.functions(env?.id ?? '') }),
    });
}

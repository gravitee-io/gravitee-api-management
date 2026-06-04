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
import { authzApiService } from '../api/authz-api.service';
import { authzQueryKeys } from '../api/query-keys';

export function useUpdateSchema(environmentId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (schemaText: string) => authzApiService.updateSchema(environmentId, schemaText),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: authzQueryKeys.schema(environmentId) }),
    });
}

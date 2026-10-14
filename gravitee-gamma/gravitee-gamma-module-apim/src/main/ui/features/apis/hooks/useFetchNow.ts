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
import { useIsMutating } from '@tanstack/react-query';

import { useFetchApiDocumentationItem } from './useApiDocumentation';
import { notify } from '../../../shared/notify';
import type { ApiDocumentationItem } from '../types/apiDocumentation';
import { fetchOutcome } from '../utils/fetchOutcome';
import { apiDocumentationKeys } from '../utils/queryKeys';

/** Fetches an item from its source on the user's request, and tells them how it went. */
export function useFetchNow(apiId: string) {
    const env = useEnvironment();
    const fetchMutation = useFetchApiDocumentationItem(apiId);
    // Counted across every screen of the API, as a dialog may still be fetching after it closed.
    const runningFetches = useIsMutating({ mutationKey: apiDocumentationKeys.fetch(env?.id ?? '', apiId) });

    async function fetchNow(item: Pick<ApiDocumentationItem, 'id' | 'title'>) {
        try {
            const outcome = fetchOutcome(item.title, await fetchMutation.mutateAsync(item.id));
            if (outcome.level === 'success') notify.success(outcome.message);
            else if (outcome.level === 'warning') notify.warning(outcome.message);
            else notify.error(outcome.message);
        } catch (error) {
            notify.error(error, `Failed to fetch '${item.title}'`);
        }
    }

    return { fetchNow, isFetching: runningFetches > 0 };
}

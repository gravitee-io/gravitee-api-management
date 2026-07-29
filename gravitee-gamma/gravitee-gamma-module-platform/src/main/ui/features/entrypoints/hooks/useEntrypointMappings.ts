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

import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { listEntrypoints } from '../services/entrypoints';
import { listOrgEnvironments } from '../services/environments';
import { listOrgTags } from '../services/tags';
import { toEntrypointMappingRows } from '../utils/entrypointMappings';
import { entrypointKeys, orgEnvironmentKeys, orgTagKeys } from '../utils/queryKeys';

export function useEntrypointMappings() {
    const entrypointsQuery = useQuery({
        queryKey: entrypointKeys.list(),
        queryFn: listEntrypoints,
    });

    const environmentsQuery = useQuery({
        queryKey: orgEnvironmentKeys.list(),
        queryFn: listOrgEnvironments,
    });

    const tagsQuery = useQuery({
        queryKey: orgTagKeys.list(),
        queryFn: listOrgTags,
    });

    const rows = useMemo(() => {
        if (!entrypointsQuery.data) return [];
        return toEntrypointMappingRows(entrypointsQuery.data, environmentsQuery.data ?? [], tagsQuery.data ?? []);
    }, [entrypointsQuery.data, environmentsQuery.data, tagsQuery.data]);

    return {
        rows,
        tags: tagsQuery.data ?? [],
        environments: environmentsQuery.data ?? [],
        isLoading: entrypointsQuery.isLoading || environmentsQuery.isLoading || tagsQuery.isLoading,
        isError: entrypointsQuery.isError,
        isNameResolutionError: environmentsQuery.isError || tagsQuery.isError,
    };
}

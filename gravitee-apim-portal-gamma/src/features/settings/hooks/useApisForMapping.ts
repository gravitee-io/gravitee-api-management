/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useEffect, useState } from 'react';

import { searchApis } from '../../editor/services/api.service';
import type { MappedApi } from '../types';

interface UseApisForMappingOptions {
    /** When false, skips fetching. Defaults to true. */
    readonly enabled?: boolean;
    /** Optional search query sent to the mock API catalog. */
    readonly query?: string;
}

/**
 * Loads APIs for category / subscription-form mapping from the POC mock catalog.
 */
export function useApisForMapping({ enabled = true, query = '' }: UseApisForMappingOptions = {}): {
    apis: MappedApi[];
    loading: boolean;
} {
    const [apis, setApis] = useState<MappedApi[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!enabled) {
            setApis([]);
            setLoading(false);
            return;
        }

        let cancelled = false;
        setLoading(true);
        const trimmedQuery = query.trim();

        void searchApis({ q: trimmedQuery, size: 50 })
            .then(response => {
                if (cancelled) {
                    return;
                }
                const mapped = (response.data ?? []).map(api => ({
                    id: api.id,
                    name: api.name?.trim() || api.id,
                }));
                setApis(mapped);
            })
            .catch(() => {
                if (!cancelled) {
                    setApis([]);
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [enabled, query]);

    return { apis, loading };
}

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
import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

import { parseCatalogSearchParams, serializeCatalogSearchParams, type CatalogParams } from './catalog-params';

export function useCatalogParams() {
    const [searchParams, setSearchParams] = useSearchParams();
    const params = useMemo(() => parseCatalogSearchParams(searchParams), [searchParams]);

    const update = useCallback(
        (patch: Partial<CatalogParams>) => {
            setSearchParams(
                prev => {
                    const current = parseCatalogSearchParams(prev);
                    const merged: CatalogParams = {
                        ...current,
                        ...patch,
                        page: patch.page ?? 1,
                    };
                    return serializeCatalogSearchParams(merged);
                },
                { replace: true },
            );
        },
        [setSearchParams],
    );

    return { params, update };
}

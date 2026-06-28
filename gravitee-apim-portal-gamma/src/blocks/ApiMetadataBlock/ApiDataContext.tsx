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
import { useQuery } from '@tanstack/react-query';
import { createContext, useContext, type ReactNode } from 'react';

import type { Api } from '../../features/editor/entities/api';
import { getApiById } from '../../features/editor/services/api.service';
import { usePortalPageOptional } from '../../features/portal-shell/context/PortalPageContext';

const ApiDataContext = createContext<Api | null>(null);

interface ApiDataProviderProps {
    readonly api: Api | null;
    readonly children: ReactNode;
}

export function ApiDataProvider({ api, children }: ApiDataProviderProps) {
    return <ApiDataContext.Provider value={api}>{children}</ApiDataContext.Provider>;
}

export function useApiData(): Api | null {
    return useContext(ApiDataContext);
}

interface ApiDataProviderFromPortalProps {
    readonly children: ReactNode;
}

/** Resolves API data from the current portal page's API nav ancestor. */
export function ApiDataProviderFromPortal({ children }: ApiDataProviderFromPortalProps) {
    const portalPage = usePortalPageOptional();
    const apiId = portalPage?.apiNavItem?.apiId;

    const { data: api } = useQuery({
        queryKey: ['api', apiId],
        queryFn: () => getApiById(apiId!),
        enabled: Boolean(apiId),
    });

    return <ApiDataProvider api={api ?? null}>{children}</ApiDataProvider>;
}

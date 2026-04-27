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
import { useEffect, useMemo, useState } from 'react';

import type { ProxyConfig } from '../../../../domain/apiCreation/models';
import { useApimRuntime } from '../context/apimRuntimeContext';
import { verifyPaths } from '../services/apis';

const PATH_PATTERN = /^\/[/.a-zA-Z0-9-_]*$/;

function useDebounced<T>(value: T, ms: number): T {
    const [debounced, setDebounced] = useState(value);
    useEffect(() => {
        const t = setTimeout(() => setDebounced(value), ms);
        return () => clearTimeout(t);
    }, [value, ms]);
    return debounced;
}

function pathsToVerify(proxy: ProxyConfig): Array<{ path: string; host?: string }> {
    if (proxy.enableVirtualHosts) {
        const rows = proxy.virtualHosts?.length ? proxy.virtualHosts : [];
        return rows
            .filter((v) => v.path?.trim() && v.host?.trim())
            .map((v) => ({ path: v.path.trim(), host: v.host.trim() }));
    }
    const p = proxy.contextPath?.trim() ?? '';
    if (!p || !PATH_PATTERN.test(p)) return [];
    return [{ path: p.startsWith('/') ? p : `/${p}` }];
}

/**
 * Debounced server-side path verification (`POST .../v2/apis/_verify/paths`), matching Console async validators.
 */
export function useDebouncedVerifyPaths(proxy: ProxyConfig, enabled: boolean) {
    const runtime = useApimRuntime();
    const paths = useMemo(() => pathsToVerify(proxy), [proxy]);
    const debouncedPaths = useDebounced(paths, 350);

    const allVirtualHostsFilled =
        !proxy.enableVirtualHosts ||
        (proxy.virtualHosts?.every((v) => Boolean(v.host?.trim()) && Boolean(v.path?.trim())) ?? false);

    const canRun = enabled && debouncedPaths.length > 0 && allVirtualHostsFilled;

    return useQuery({
        queryKey: ['verify-paths', JSON.stringify(debouncedPaths)],
        queryFn: () => verifyPaths(runtime, undefined, debouncedPaths),
        enabled: canRun,
        staleTime: 30_000,
    });
}

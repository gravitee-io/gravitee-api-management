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
import { useCallback, useEffect, useRef, useState } from 'react';
import { authzApiService, DEFAULT_PER_PAGE } from '../api/authz-api.service';
import type { EntityRequest, EntityResponse, PagedResponse } from '../api/authz-api.types';

export interface UseEntitiesResult {
    readonly data: PagedResponse<EntityResponse> | null;
    readonly isLoading: boolean;
    readonly error: string | undefined;
    readonly page: number;
    readonly perPage: number;
    readonly setPage: (page: number) => void;
    readonly setPerPage: (perPage: number) => void;
    readonly create: (request: EntityRequest) => Promise<EntityResponse>;
    readonly update: (id: string, request: EntityRequest) => Promise<EntityResponse>;
    readonly remove: (id: string) => Promise<void>;
    readonly reload: () => void;
}

export function useEntities(environmentId: string, initialPerPage = DEFAULT_PER_PAGE): UseEntitiesResult {
    const mountedRef = useRef(true);
    const [page, setPage] = useState(1);
    // Resync perPage from prop on every change so callers can drive it as a controlled value.
    const [perPage, setPerPage] = useState(initialPerPage);
    useEffect(() => {
        setPerPage(initialPerPage);
    }, [initialPerPage]);
    const [data, setData] = useState<PagedResponse<EntityResponse> | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | undefined>(undefined);
    const [nonce, setNonce] = useState(0);

    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
        };
    }, []);

    useEffect(() => {
        let cancelled = false;
        // Gate setState to break cascading renders when nothing actually
        // changes (isLoading already true / error already undefined).
        if (!isLoading) setIsLoading(true);
        if (error !== undefined) setError(undefined);

        authzApiService
            .listEntities(environmentId, { page, perPage })
            .then(res => {
                if (cancelled) return;
                setData(res);
            })
            .catch(e => {
                if (cancelled) return;
                setError(e instanceof Error ? e.message : 'Failed to load entities');
                setData(null);
            })
            .finally(() => {
                if (!cancelled) setIsLoading(false);
            });

        return () => {
            cancelled = true;
        };
        // isLoading/error intentionally excluded — they are read for gating
        // setState; including them would re-trigger the effect.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [environmentId, page, perPage, nonce]);

    const reload = useCallback(() => setNonce(n => n + 1), []);

    const create = useCallback(
        async (request: EntityRequest) => {
            const created = await authzApiService.createEntity(environmentId, request);
            if (!mountedRef.current) return created;
            reload();
            return created;
        },
        [environmentId, reload],
    );

    const update = useCallback(
        async (id: string, request: EntityRequest) => {
            const updated = await authzApiService.updateEntity(environmentId, id, request);
            if (!mountedRef.current) return updated;
            reload();
            return updated;
        },
        [environmentId, reload],
    );

    const remove = useCallback(
        async (id: string) => {
            await authzApiService.deleteEntity(environmentId, id);
            if (!mountedRef.current) return;
            reload();
        },
        [environmentId, reload],
    );

    return { data, isLoading, error, page, perPage, setPage, setPerPage, create, update, remove, reload };
}

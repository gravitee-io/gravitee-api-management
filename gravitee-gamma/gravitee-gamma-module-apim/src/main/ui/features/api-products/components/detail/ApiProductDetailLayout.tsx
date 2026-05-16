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
import { Badge, Button, Skeleton } from '@gravitee/graphene-core';
import { Navigate, Outlet, useNavigate, useParams } from 'react-router-dom';

import { ApiProductDetailContext } from '../../context/ApiProductDetailContext';
import { useApiProductDetail } from '../../hooks/useApiProductDetail';
import { useApiProductPermissions } from '../../hooks/useApiProductPermissions';
import type { ApiProductListItem } from '../../types/apiProduct';
import { SyncStatusBadge } from '../SyncStatusBadge';
import { ApiProductSidebarNav } from './ApiProductSidebarNav';
import { useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';

function ProductInfoHeader({ product, isLoading }: { product: ApiProductListItem | null; isLoading: boolean }) {
    if (isLoading) {
        return (
            <div className="px-3 pt-4 pb-4 border-b space-y-3">
                <div className="flex items-start gap-2.5">
                    <Skeleton className="size-8 rounded-lg shrink-0" />
                    <div className="space-y-1.5 min-w-0 flex-1">
                        <Skeleton className="h-3.5 w-32 rounded" />
                        <Skeleton className="h-3 w-16 rounded" />
                    </div>
                </div>
                <div className="flex gap-1.5">
                    <Skeleton className="h-5 w-14 rounded-md" />
                </div>
            </div>
        );
    }
    if (!product) return null;

    return (
        <div className="px-3 pt-4 pb-4 border-b space-y-2">
            <div className="space-y-1.5">
                <p
                    className="text-sm font-semibold text-foreground leading-snug"
                    style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={product.name}
                >
                    {product.name}
                </p>
                {product.deploymentState ? <SyncStatusBadge state={product.deploymentState} compact /> : null}
            </div>

            {product.description ? (
                <p
                    className="text-xs text-muted-foreground"
                    style={{
                        lineHeight: '1.625',
                        wordBreak: 'break-all',
                        overflow: 'hidden',
                        display: '-webkit-box',
                        WebkitBoxOrient: 'vertical',
                        WebkitLineClamp: 2,
                    }}
                >
                    {product.description}
                </p>
            ) : null}

            <div className="flex flex-wrap items-center gap-1">
                <Badge variant="outline" className="text-xs font-mono px-1.5 py-0 h-5">
                    {product.version}
                </Badge>
                <Badge variant="secondary" className="text-xs px-1.5 py-0 h-5">
                    {product.apiIds?.length ?? 0} APIs
                </Badge>
            </div>
        </div>
    );
}

export function ApiProductDetailLayout() {
    const { productId } = useParams<{ productId: string }>();
    const navigate = useNavigate();
    const basePath = useDetailBasePath('api-products', productId);
    const { data: product, isLoading, isError } = useApiProductDetail(productId);
    const { permissionsReady } = useApiProductPermissions(productId);

    if (isError) {
        return (
            <div className="flex flex-col items-center justify-center gap-4 p-8">
                <p className="text-sm text-muted-foreground">
                    Failed to load API product. It may have been deleted or you may not have access.
                </p>
                <Button variant="outline" size="sm" onClick={() => navigate('..')}>
                    Back to API Products
                </Button>
            </div>
        );
    }

    return (
        <ApiProductDetailContext.Provider value={{ product: product ?? null, isLoading, permissionsReady }}>
            <div className="flex" style={{ height: 'calc(100dvh - 5rem)' }}>
                <aside className="shrink-0 min-w-0 w-56 overflow-y-auto overflow-x-hidden pb-4" style={{ maxWidth: '14rem' }}>
                    <ProductInfoHeader product={product ?? null} isLoading={isLoading} />
                    <ApiProductSidebarNav basePath={basePath} />
                </aside>
                <div className="w-px bg-border shrink-0" />
                <main className="min-w-0 flex-1 pl-6 overflow-y-auto">
                    <Outlet />
                </main>
            </div>
        </ApiProductDetailContext.Provider>
    );
}

export function ApiProductIndexRedirect() {
    return <Navigate to="overview" replace />;
}

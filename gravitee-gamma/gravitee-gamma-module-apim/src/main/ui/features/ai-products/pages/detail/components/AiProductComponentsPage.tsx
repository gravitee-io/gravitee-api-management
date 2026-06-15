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
import { Button, Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import {
    BotIcon,
    BrainCircuitIcon,
    ChevronDownIcon,
    ChevronRightIcon,
    PlusIcon,
    ServerIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../../../shared/components';
import { notify } from '../../../../../shared/notify';
import { useApiProductDetailContext } from '../../../../api-products/context/ApiProductDetailContext';
import { useApiProductApis } from '../../../../api-products/hooks/useApiProductApis';
import { useUpdateApiProduct } from '../../../../api-products/hooks/useUpdateApiProduct';
import type { ApiListItem } from '../../../../apis/types';
import { AddComponentSheet } from '../../../components/components/AddComponentSheet';
import { ComponentModelsList } from '../../../components/components/ComponentModelsList';
import { ProductModelsCard } from '../../../components/components/ProductModelsCard';
import { ComponentTypeBadge } from '../../../components/ComponentTypeBadge';

function TypeStatCard({ icon: Icon, label, value }: { icon: typeof BrainCircuitIcon; label: string; value: number | null }) {
    return (
        <Card style={{ flex: 1 }}>
            <CardContent className="pt-5 pb-4">
                <div className="flex items-center gap-3">
                    <div className="rounded-md bg-primary/10 p-2 shrink-0">
                        <Icon className="size-4 text-primary" aria-hidden />
                    </div>
                    <div>
                        {value === null ? (
                            <Skeleton className="h-6 w-8 rounded" />
                        ) : (
                            <p className="text-xl font-semibold leading-tight">{value}</p>
                        )}
                        <p className="text-sm text-muted-foreground">{label}</p>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}

function ComponentRow({ api, onRequestRemove }: { api: ApiListItem; onRequestRemove: () => void }) {
    const [expanded, setExpanded] = useState(false);
    const path = api.listeners?.find(l => l.type === 'HTTP')?.paths?.[0]?.path ?? '';
    const isLlmProxy = api.type === 'LLM_PROXY';

    return (
        <div className="rounded-lg border">
            <div className="flex items-center gap-3 px-3 py-2.5">
                <button
                    type="button"
                    className="flex flex-1 items-center gap-3 text-left"
                    onClick={() => isLlmProxy && setExpanded(v => !v)}
                    aria-expanded={isLlmProxy ? expanded : undefined}
                >
                    {isLlmProxy ? (
                        expanded ? (
                            <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        ) : (
                            <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        )
                    ) : (
                        <span className="size-4 shrink-0" />
                    )}
                    <div className="rounded-md bg-primary/10 p-1.5 shrink-0">
                        <BrainCircuitIcon className="size-3.5 text-primary" aria-hidden />
                    </div>
                    <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium">{api.name}</p>
                        {path ? <p className="text-xs text-muted-foreground font-mono">{path}</p> : null}
                    </div>
                </button>
                <ComponentTypeBadge type={api.type} />
                <Button variant="ghost" size="icon" aria-label={`Remove ${api.name}`} onClick={onRequestRemove}>
                    <Trash2Icon className="size-4 text-muted-foreground" aria-hidden />
                </Button>
            </div>
            {expanded && isLlmProxy ? (
                <div className="border-t px-12 py-3">
                    <ComponentModelsList apiId={api.id} />
                </div>
            ) : null}
        </div>
    );
}

export function AiProductComponentsPage() {
    const { productId } = useParams<{ productId: string }>();
    const { product } = useApiProductDetailContext();
    const [sheetOpen, setSheetOpen] = useState(false);
    const [componentToRemove, setComponentToRemove] = useState<ApiListItem | null>(null);

    // Component lists stay small for a product — one page of 50 covers the PoC scope.
    const { data: apisData, isLoading, isError } = useApiProductApis(productId, 1, 50);
    const components = apisData?.data ?? [];

    const { mutate: updateProduct, isPending: isUpdating } = useUpdateApiProduct(productId ?? '');

    const llmCount = components.filter(api => api.type === 'LLM_PROXY').length;
    const mcpCount = components.filter(api => api.type === 'MCP_PROXY').length;
    const otherCount = components.length - llmCount - mcpCount;

    function handleAddComponents(newIds: string[]) {
        if (!product) return;
        const merged = [...new Set([...(product.apiIds ?? []), ...newIds])];
        updateProduct(
            { name: product.name, version: product.version, description: product.description, apiIds: merged },
            {
                onSuccess: () => {
                    notify.success('Components added — deploy the product to apply changes');
                    setSheetOpen(false);
                },
                onError: error => notify.error(error, 'Failed to add components to the product.'),
            },
        );
    }

    function handleConfirmRemove() {
        if (!product || !componentToRemove) return;
        const updated = (product.apiIds ?? []).filter(id => id !== componentToRemove.id);
        updateProduct(
            { name: product.name, version: product.version, description: product.description, apiIds: updated },
            {
                onSuccess: () => {
                    notify.success('Component removed — deploy the product to apply changes');
                    setComponentToRemove(null);
                },
                onError: error => notify.error(error, 'Failed to remove the component.'),
            },
        );
    }

    return (
        <div className="space-y-6 p-6">
            <div className="flex items-center justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Components</h1>
                    <p className="text-sm text-muted-foreground">LLM proxies, MCP proxies, and Agents bundled in this product.</p>
                </div>
                <Button onClick={() => setSheetOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add component
                </Button>
            </div>

            <div className="flex gap-4">
                <TypeStatCard icon={BrainCircuitIcon} label="LLM Proxies" value={isLoading ? null : llmCount} />
                <TypeStatCard icon={ServerIcon} label="MCP Proxies" value={isLoading ? null : mcpCount} />
                <TypeStatCard icon={BotIcon} label="Other components" value={isLoading ? null : otherCount} />
            </div>

            <ProductModelsCard apiIds={product?.apiIds ?? []} />

            {isError ? (
                <p className="text-sm text-destructive">Failed to load components for this product. Please refresh and try again.</p>
            ) : isLoading ? (
                <div className="space-y-2">
                    {Array.from({ length: 3 }).map((_, i) => (
                        <Skeleton key={i} className="h-14 rounded-lg" />
                    ))}
                </div>
            ) : components.length === 0 ? (
                <Card className="border-dashed">
                    <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
                        <BrainCircuitIcon className="size-6 text-muted-foreground opacity-60" aria-hidden />
                        <div className="space-y-1">
                            <p className="text-sm font-medium">No components in this product yet</p>
                            <p className="text-xs text-muted-foreground max-w-md">
                                Add an LLM proxy to expose its models through this product. Plans on this product will secure and meter
                                every component you add.
                            </p>
                        </div>
                        <Button size="sm" onClick={() => setSheetOpen(true)}>
                            <PlusIcon className="size-4" aria-hidden />
                            Add component
                        </Button>
                    </CardContent>
                </Card>
            ) : (
                <div className="space-y-3">
                    <p className="text-sm font-medium text-muted-foreground">
                        Bundled components <span className="font-normal">— {components.length} in this product</span>
                    </p>
                    <div className="space-y-2">
                        {components.map(api => (
                            <ComponentRow key={api.id} api={api} onRequestRemove={() => setComponentToRemove(api)} />
                        ))}
                    </div>
                </div>
            )}

            <AddComponentSheet
                open={sheetOpen}
                existingApiIds={product?.apiIds ?? []}
                onClose={() => setSheetOpen(false)}
                onAdd={handleAddComponents}
                isAdding={isUpdating}
            />

            <ConfirmDialog
                open={componentToRemove !== null}
                onOpenChange={open => !open && setComponentToRemove(null)}
                title="Remove component"
                description="Once this component is removed from the AI Product, consumers will lose access to it through product plans."
                confirmLabel="Remove"
                pendingLabel="Removing…"
                destructive
                isPending={isUpdating}
                onConfirm={handleConfirmRemove}
            />
        </div>
    );
}

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
import {
    Badge,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    DataTable,
    DataTableEmptyState,
    type DataTableProps,
    Input,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    BoxesIcon,
    CircleCheckIcon,
    LayoutGridIcon,
    PlusIcon,
    SearchIcon,
    Trash2Icon,
    UsersRoundIcon,
} from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../../../shared/components';
import { notify } from '../../../../../shared/notify';
import { ApiAvatar } from '../../../../apis/components/ApiAvatar';
import type { ApiListItem } from '../../../../apis/types';
import { AddApiToProduct } from '../../../components/apis/AddApiToProduct';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductApis } from '../../../hooks/useApiProductApis';
import { useUpdateApiProduct } from '../../../hooks/useUpdateApiProduct';

type ColCell<T> = { row: { original: T } };

function buildColumns(onRequestRemove: (api: ApiListItem) => void): DataTableProps<ApiListItem>['columns'] {
    return [
        {
            id: 'API Name',
            accessorFn: (row: ApiListItem) => row.name,
            header: 'API Name',
            enableSorting: false,
            cell: ({ row }: ColCell<ApiListItem>) => {
                const api = row.original;
                const path = api.listeners?.find(l => l.type === 'HTTP')?.paths?.[0]?.path ?? '';
                return (
                    <div className="flex items-center gap-2">
                        <ApiAvatar src={api._links?.pictureUrl} name={api.name} />
                        <div>
                            <p className="text-sm font-medium">{api.name}</p>
                            {path ? <p className="text-xs text-muted-foreground font-mono">{path}</p> : null}
                        </div>
                    </div>
                );
            },
        },
        {
            id: 'Version',
            accessorFn: (row: ApiListItem) => row.apiVersion,
            header: 'Version',
            enableSorting: false,
            cell: ({ row }: ColCell<ApiListItem>) => (
                <Badge variant="outline" className="font-mono text-xs">
                    {row.original.apiVersion}
                </Badge>
            ),
        },
        {
            id: 'Owner',
            accessorFn: (row: ApiListItem) => row.primaryOwner?.displayName ?? '',
            header: 'Owner',
            enableSorting: false,
            cell: ({ row }: ColCell<ApiListItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.primaryOwner?.displayName ?? '—'}</span>
            ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<ApiListItem>) => (
                <div className="flex justify-end">
                    <Button
                        variant="ghost"
                        size="icon"
                        className="text-destructive hover:text-destructive hover:bg-destructive/10"
                        aria-label={`Remove ${row.original.name} from product`}
                        title="Remove from product"
                        onClick={() => onRequestRemove(row.original)}
                    >
                        <Trash2Icon className="size-4" aria-hidden />
                    </Button>
                </div>
            ),
        },
    ];
}

function DiagramNode({
    icon: Icon,
    label,
    sub,
    truncateSub,
    iconClass,
    iconStyle,
}: {
    icon: React.ComponentType<{ className?: string; style?: React.CSSProperties }>;
    label: string;
    sub: string;
    truncateSub?: boolean;
    iconClass?: string;
    iconStyle?: React.CSSProperties;
}) {
    return (
        <div className="flex flex-col items-center text-center" style={{ width: '140px' }}>
            <div className="rounded-lg border bg-card p-2 shadow-sm">
                <Icon className={iconClass ?? 'size-5'} style={iconStyle} aria-hidden />
            </div>
            <p className="mt-1.5 text-xs font-medium">{label}</p>
            {truncateSub ? (
                <p
                    className="text-xs text-muted-foreground w-full overflow-hidden"
                    style={{ textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={sub}
                >
                    {sub}
                </p>
            ) : (
                <p className="text-xs text-muted-foreground break-words">{sub}</p>
            )}
        </div>
    );
}

const EMPTY_TIPS = [
    'Plans for this product grant access to every API you include; changing this list changes what clients can call under those plans.',
    'Removing a proxy detaches it from the bundle—review active subscriptions and dependents before you remove it in production.',
    'When the list grows, use search and pagination to find and review APIs in this product.',
];

function EmptyApisLanding({ productName }: { productName?: string }) {
    return (
        <Card className="border-dashed">
            <CardHeader className="pb-2">
                <CardTitle className="text-lg">No APIs in this product yet</CardTitle>
            </CardHeader>
            <CardContent className="space-y-8">
                {/* "How it fits together" diagram — border-primary/20 bg-primary/5 matches ApiAlertsPage pattern */}
                <div className="rounded-xl border border-primary/20 bg-primary/5 p-5">
                    <p className="text-center text-xs font-medium text-primary">How it fits together</p>
                    <div className="mt-4 flex flex-row items-start justify-center gap-4">
                        {/* violet-600 for the API Product node — text-violet-600 is not compiled in MF */}
                        <DiagramNode
                            icon={LayoutGridIcon}
                            label="API Product"
                            sub={productName ?? 'This product'}
                            truncateSub
                            iconClass="size-5"
                            iconStyle={{ color: '#7c3aed' }}
                        />
                        <ArrowRightIcon className="size-5 shrink-0 mt-2.5 text-primary/60" aria-hidden />
                        <DiagramNode
                            icon={BoxesIcon}
                            label="Included APIs"
                            sub="Proxies that belong to this bundle"
                            iconClass="size-5 text-primary"
                        />
                        <ArrowRightIcon className="size-5 shrink-0 mt-2.5 text-primary/60" aria-hidden />
                        {/* amber-600 for Plans & consumers — text-amber-600 is not compiled in MF */}
                        <DiagramNode
                            icon={UsersRoundIcon}
                            label="Plans & consumers"
                            sub="Plans expose this bundle; consumers subscribe to a plan."
                            iconClass="size-5"
                            iconStyle={{ color: '#d97706' }}
                        />
                    </div>
                </div>

                {/* Explanatory columns */}
                <div className="mx-auto grid grid-cols-2 gap-10" style={{ maxWidth: '40rem' }}>
                    <div className="space-y-2 text-center">
                        <p className="text-sm font-semibold">Why add APIs?</p>
                        <p className="text-xs leading-relaxed text-muted-foreground">
                            A product is the unit you attach plans to. Each linked proxy shares the same access and metering
                            boundary—subscribers on a plan inherit every route you include, and operators manage one cohesive bundle instead
                            of unrelated proxies.
                        </p>
                    </div>
                    <div className="space-y-2 text-center">
                        <p className="text-sm font-semibold">What gets linked?</p>
                        <p className="text-xs leading-relaxed text-muted-foreground">
                            HTTP API proxies from your environment catalog. Use <span className="font-medium text-foreground">Add API</span>{' '}
                            to attach proxies that are not already in this product. Prefer stable, production-ready endpoints for anything
                            tied to live plans.
                        </p>
                    </div>
                </div>

                {/* Tips list */}
                <ul className="space-y-2.5 border-t pt-6 text-sm text-muted-foreground">
                    {EMPTY_TIPS.map(tip => (
                        <li key={tip} className="flex gap-2">
                            <CircleCheckIcon className="mt-0.5 size-4 shrink-0 text-success" aria-hidden />
                            <span className="leading-snug">{tip}</span>
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
}

export function ApiProductApisPage() {
    useLayoutConfig({ contentVariant: 'wide' }, []);
    const { productId } = useParams<{ productId: string }>();
    const { product } = useApiProductDetailContext();
    const [dialogOpen, setDialogOpen] = useState(false);
    const [apiToRemove, setApiToRemove] = useState<ApiListItem | null>(null);

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(10);

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(search);
            setPage(1);
        }, 300);
        return () => clearTimeout(timer);
    }, [search]);

    const { data: apisData, isLoading, isError: isApisError } = useApiProductApis(productId, page, perPage, debouncedSearch || undefined);
    const apis = apisData?.data ?? [];
    const totalApiCount = apisData?.pagination?.totalCount ?? 0;

    const { mutate: updateProduct, isPending: isUpdating } = useUpdateApiProduct(productId ?? '');

    function handleAddApis(newIds: string[]) {
        if (!product) return;
        const merged = [...new Set([...(product.apiIds ?? []), ...newIds])];
        updateProduct(
            { name: product.name, version: product.version, description: product.description, apiIds: merged },
            {
                onSuccess: () => {
                    notify.success('APIs added to the product');
                    setDialogOpen(false);
                },
                onError: error => notify.error(error, 'Failed to add APIs to the product.'),
            },
        );
    }

    function handleConfirmRemove() {
        if (!product || !apiToRemove) return;
        const updated = (product.apiIds ?? []).filter(id => id !== apiToRemove.id);
        updateProduct(
            { name: product.name, version: product.version, description: product.description, apiIds: updated },
            {
                onSuccess: () => {
                    notify.success('API removed from the product');
                    setApiToRemove(null);
                },
                onError: error => notify.error(error, 'Failed to remove API from the product.'),
            },
        );
    }

    const columns = buildColumns(api => setApiToRemove(api));

    const hasNoApisAssigned = (product?.apiIds?.length ?? 0) === 0;
    const isFirstUse = !isLoading && !debouncedSearch && (hasNoApisAssigned || (!isApisError && totalApiCount === 0));

    return (
        <div className="space-y-6 p-6">
            <div className="flex items-center justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">APIs</h1>
                    <p className="text-sm text-muted-foreground">Manage APIs grouped together in your API Product.</p>
                </div>
                <Button onClick={() => setDialogOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add API
                </Button>
            </div>

            {isFirstUse ? (
                <EmptyApisLanding productName={product?.name} />
            ) : isApisError ? (
                <p className="text-sm text-destructive">Failed to load APIs for this product. Please refresh and try again.</p>
            ) : (
                <DataTable
                    aria-label="Product APIs"
                    columns={columns}
                    data={apis}
                    loading={isLoading}
                    skeletonCount={perPage}
                    serverSide
                    pagination={{
                        page,
                        pageSize: perPage,
                        totalCount: totalApiCount,
                        pageSizeOptions: [10, 25, 50, 100],
                        onPageChange: setPage,
                        onPageSizeChange: p => {
                            setPerPage(p);
                            setPage(1);
                        },
                    }}
                    emptyMessage={
                        <DataTableEmptyState
                            variant="no-results"
                            icon={<SearchIcon />}
                            title="No APIs match your search"
                            description="Try adjusting your search terms."
                            action={
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={() => {
                                        setSearch('');
                                        setPage(1);
                                    }}
                                >
                                    Clear search
                                </Button>
                            }
                        />
                    }
                    toolbar={
                        <Input
                            placeholder="Search by name"
                            aria-label="Search APIs"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            className="h-8 w-64"
                        />
                    }
                />
            )}

            <AddApiToProduct
                open={dialogOpen}
                existingApiIds={product?.apiIds ?? []}
                onClose={() => setDialogOpen(false)}
                onAdd={handleAddApis}
                isAdding={isUpdating}
            />

            <ConfirmDialog
                open={apiToRemove !== null}
                onOpenChange={open => !open && setApiToRemove(null)}
                title="Remove API"
                description="Please note that once your API is removed from this API Product, consumers will lose access to this API."
                confirmLabel="Remove"
                pendingLabel="Removing…"
                destructive
                isPending={isUpdating}
                onConfirm={handleConfirmRemove}
            />
        </div>
    );
}

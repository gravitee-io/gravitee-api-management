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
    DataTablePagination,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Input,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    BoxesIcon,
    CircleCheckIcon,
    GlobeIcon,
    LayoutGridIcon,
    MoreHorizontalIcon,
    PlusIcon,
    SearchIcon,
    UsersRoundIcon,
} from '@gravitee/graphene-core/icons';
import { useEffect, useId, useState } from 'react';
import { useParams } from 'react-router-dom';

import type { ApiListItem } from '../../../../apis/types';
import { AddApiToProductDialog } from '../../../components/apis/AddApiToProductDialog';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductApis } from '../../../hooks/useApiProductApis';
import { useUpdateApiProduct } from '../../../hooks/useUpdateApiProduct';

function ApiRow({ api, onRequestRemove }: { api: ApiListItem; onRequestRemove: () => void }) {
    const path = api.listeners?.find(l => l.type === 'HTTP')?.paths?.[0]?.path ?? '';
    return (
        <TableRow>
            <TableCell>
                <div className="flex items-center gap-2">
                    <div className="rounded-md bg-primary/10 p-1 shrink-0">
                        <GlobeIcon className="size-3.5 text-primary" aria-hidden />
                    </div>
                    <div>
                        <p className="text-sm font-medium">{api.name}</p>
                        {path ? <p className="text-xs text-muted-foreground font-mono">{path}</p> : null}
                    </div>
                </div>
            </TableCell>
            <TableCell>
                <Badge variant="outline" className="font-mono text-xs">
                    {api.apiVersion}
                </Badge>
            </TableCell>
            <TableCell className="text-sm text-muted-foreground">{api.primaryOwner?.displayName ?? '—'}</TableCell>
            <TableCell className="text-right">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" aria-label={`Actions for ${api.name}`}>
                            <MoreHorizontalIcon className="size-4" aria-hidden />
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-auto min-w-[12rem]">
                        <DropdownMenuItem onSelect={onRequestRemove} className="text-destructive focus:text-destructive">
                            Remove from product
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </TableCell>
        </TableRow>
    );
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
    const { productId } = useParams<{ productId: string }>();
    const { product } = useApiProductDetailContext();
    const [dialogOpen, setDialogOpen] = useState(false);
    const [apiToRemove, setApiToRemove] = useState<ApiListItem | null>(null);

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(10);
    const searchInputId = useId();

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

    const { mutate: updateProduct, isPending: isUpdating, error: updateError } = useUpdateApiProduct(productId ?? '');

    function handleAddApis(newIds: string[]) {
        if (!product) return;
        const merged = [...new Set([...(product.apiIds ?? []), ...newIds])];
        updateProduct(
            { name: product.name, version: product.version, description: product.description, apiIds: merged },
            { onSuccess: () => setDialogOpen(false) },
        );
    }

    function handleConfirmRemove() {
        if (!product || !apiToRemove) return;
        const updated = (product.apiIds ?? []).filter(id => id !== apiToRemove.id);
        updateProduct(
            { name: product.name, version: product.version, description: product.description, apiIds: updated },
            { onSuccess: () => setApiToRemove(null) },
        );
    }

    // Truly empty: no APIs at all (not a filtered-out result)
    const isEmpty = !isLoading && !isApisError && totalApiCount === 0 && !debouncedSearch;

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

            {updateError ? <p className="text-sm text-destructive">{updateError.message}</p> : null}

            {isApisError ? (
                <p className="text-sm text-destructive">Failed to load APIs for this product. Please refresh and try again.</p>
            ) : isLoading && !apisData ? (
                <div className="space-y-2">
                    {Array.from({ length: 3 }).map((_, i) => (
                        <Skeleton key={i} className="h-14 rounded-lg" />
                    ))}
                </div>
            ) : isEmpty ? (
                <EmptyApisLanding productName={product?.name} />
            ) : (
                <>
                    {/* Search + pagination row */}
                    <div className="flex items-center justify-between gap-4">
                        <div className="relative w-72 shrink-0">
                            <SearchIcon
                                className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                                aria-hidden
                            />
                            <label htmlFor={searchInputId} className="sr-only">
                                Search APIs
                            </label>
                            <Input
                                id={searchInputId}
                                placeholder="Search by name"
                                value={search}
                                onChange={e => setSearch(e.target.value)}
                                className="pl-9"
                            />
                        </div>
                        <DataTablePagination
                            page={page}
                            pageSize={perPage}
                            totalCount={totalApiCount}
                            pageSizeOptions={[10, 25, 50]}
                            onPageChange={setPage}
                            onPageSizeChange={p => {
                                setPerPage(p);
                                setPage(1);
                            }}
                        />
                    </div>

                    {/* Table */}
                    <div className="rounded-lg border">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>API Name</TableHead>
                                    <TableHead>Version</TableHead>
                                    <TableHead>Owner</TableHead>
                                    <TableHead className="w-10 text-right">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {isLoading ? (
                                    Array.from({ length: perPage }).map((_, i) => (
                                        <TableRow key={i}>
                                            <TableCell colSpan={4}>
                                                <Skeleton className="h-8 w-full rounded" />
                                            </TableCell>
                                        </TableRow>
                                    ))
                                ) : apis.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={4} className="py-10 text-center text-sm text-muted-foreground">
                                            No APIs match your search.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    apis.map(api => <ApiRow key={api.id} api={api} onRequestRemove={() => setApiToRemove(api)} />)
                                )}
                            </TableBody>
                        </Table>
                    </div>

                    {/* Bottom pagination */}
                    <div className="flex justify-end">
                        <DataTablePagination
                            page={page}
                            pageSize={perPage}
                            totalCount={totalApiCount}
                            pageSizeOptions={[10, 25, 50]}
                            onPageChange={setPage}
                            onPageSizeChange={p => {
                                setPerPage(p);
                                setPage(1);
                            }}
                        />
                    </div>
                </>
            )}

            <AddApiToProductDialog
                open={dialogOpen}
                existingApiIds={product?.apiIds ?? []}
                onClose={() => setDialogOpen(false)}
                onAdd={handleAddApis}
                isAdding={isUpdating}
            />

            <Dialog
                open={apiToRemove !== null}
                onOpenChange={open => {
                    if (!open) setApiToRemove(null);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Remove API</DialogTitle>
                        <DialogDescription>
                            Please note that once your API is removed from this API Product, consumers will lose access to this API.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setApiToRemove(null)} disabled={isUpdating}>
                            Cancel
                        </Button>
                        <Button variant="destructive" onClick={handleConfirmRemove} disabled={isUpdating}>
                            {isUpdating ? 'Removing…' : 'Remove'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}

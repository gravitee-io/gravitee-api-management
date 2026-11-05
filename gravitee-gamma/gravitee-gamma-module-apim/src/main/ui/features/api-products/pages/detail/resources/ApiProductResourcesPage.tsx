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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Badge,
    Button,
    Card,
    CardContent,
    DataTablePagination,
    Input,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, PlusIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ApiProductResourceRowActions } from './ApiProductResourceRowActions';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { ConfirmDialog } from '../../../../../shared/components/ConfirmDialog';
import { notify } from '../../../../../shared/notify';
import { PluginIcon } from '../../../../apis/components/PluginIcon';
import { useResourcePlugins } from '../../../../apis/hooks/useApiResources';
import type { ApiResource } from '../../../../apis/types/resource';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';
import { PRODUCT_RESOURCE_PAGE_SIZE_OPTIONS, useProductResourceTableState } from '../../../hooks/useProductResourceTableState';
import { useUpdateApiProductResources } from '../../../hooks/useUpdateApiProductResources';
import type { ApiProductResourcesApplyFn } from '../../../services/apiProduct';
import { removeProductResourceApply, toggleProductResourceApply } from '../../../utils/apiProductResourceListMutations';
import { apiProductKeys } from '../../../utils/queryKeys';

export function ApiProductResourcesPage() {
    const { productId } = useParams<{ productId: string }>();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { product, isLoading } = useApiProductDetailContext();
    const { canRead, canUpdate, isLoading: permsLoading } = useApiProductResourcePermissions(productId, 'DEFINITION');
    const { data: plugins = [] } = useResourcePlugins();
    const mutation = useUpdateApiProductResources(productId);

    const resources = useMemo<ApiResource[]>(() => product?.resources ?? [], [product?.resources]);

    const pluginById = useMemo(() => new Map(plugins.map(p => [p.id, p])), [plugins]);

    const matchesSearch = useCallback(
        (resource: ApiResource, query: string) => {
            const typeName = pluginById.get(resource.type)?.name ?? resource.type;
            return resource.name.toLowerCase().includes(query) || typeName.toLowerCase().includes(query);
        },
        [pluginById],
    );

    const { page, pageSize, totalCount, pageData, handleSearchChange, setPage, handlePageSizeChange, search } =
        useProductResourceTableState(resources, matchesSearch);

    const [removeResourceName, setRemoveResourceName] = useState<string | null>(null);

    const persist = (apply: ApiProductResourcesApplyFn, successMessage: string) => {
        mutation.mutate(apply, {
            onSuccess: () => {
                notify.success(successMessage);
                setRemoveResourceName(null);
            },
            onError: error => {
                notify.error(error, 'Failed to save resources.');
                if (error instanceof ApimApiError && error.status === 409 && env?.id && productId) {
                    void queryClient.invalidateQueries({ queryKey: apiProductKeys.detail(env.id, productId) });
                }
            },
        });
    };

    const handleToggle = (resourceName: string) => {
        const cached = resources.find(r => r.name === resourceName);
        const wasEnabled = cached?.enabled !== false;
        persist(toggleProductResourceApply(resourceName), wasEnabled ? 'Resource disabled.' : 'Resource enabled.');
    };

    const handleRemove = () => {
        if (!removeResourceName) return;
        persist(removeProductResourceApply(removeResourceName), 'Resource removed.');
    };

    const paginationBar = (
        <DataTablePagination
            page={page}
            pageSize={pageSize}
            totalCount={totalCount}
            pageSizeOptions={[...PRODUCT_RESOURCE_PAGE_SIZE_OPTIONS]}
            onPageChange={setPage}
            onPageSizeChange={handlePageSizeChange}
        />
    );

    if (isLoading || permsLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-48 rounded" />
                <Skeleton className="h-48 w-full rounded-xl" />
            </div>
        );
    }

    if (!canRead) {
        return (
            <Card>
                <CardContent className="pt-4 pb-4">
                    <p className="text-sm text-muted-foreground">You do not have permission to view API Product resources.</p>
                </CardContent>
            </Card>
        );
    }

    const removeTarget = removeResourceName ? resources.find(r => r.name === removeResourceName) : null;

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Resources</h1>
                    <p className="text-sm text-muted-foreground">
                        Resources can be referenced by policies on this API Product&apos;s plans. Changes are saved with the API Product and
                        take effect after deploy.
                    </p>
                </div>
                {canUpdate ? (
                    <Button size="sm" onClick={() => navigate('new')} data-testid="api_product_resources_add_button">
                        <PlusIcon className="size-4" />
                        Add resource
                    </Button>
                ) : null}
            </div>

            {resources.length === 0 ? (
                <Card>
                    <CardContent className="py-10 text-center text-sm text-muted-foreground" data-testid="api_product_resources_empty">
                        No resources
                    </CardContent>
                </Card>
            ) : (
                <>
                    <div className="relative">
                        <SearchIcon
                            className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground pointer-events-none"
                            aria-hidden
                        />
                        <Input
                            placeholder="Search resources…"
                            value={search}
                            onChange={e => handleSearchChange(e.target.value)}
                            className="pl-9"
                        />
                    </div>

                    <Card>
                        <CardContent className="p-0">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead style={{ width: '45%' }}>Name</TableHead>
                                        <TableHead>Resource type</TableHead>
                                        <TableHead className="w-14 text-right">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {pageData.map(resource => {
                                        const plugin = pluginById.get(resource.type);
                                        const enabled = resource.enabled !== false;
                                        return (
                                            <TableRow key={resource.name}>
                                                <TableCell>
                                                    <span data-testid="api_product_resources_name" className="font-medium">
                                                        {resource.name}
                                                    </span>
                                                    <Badge
                                                        variant={enabled ? 'success' : 'outline'}
                                                        className="ml-2 gap-1"
                                                        data-testid="api_product_resources_enabled_state"
                                                    >
                                                        {enabled ? (
                                                            <>
                                                                <CircleCheckIcon className="size-3" />
                                                                Enabled
                                                            </>
                                                        ) : (
                                                            <>
                                                                <CircleXIcon className="size-3" />
                                                                Disabled
                                                            </>
                                                        )}
                                                    </Badge>
                                                </TableCell>
                                                <TableCell data-testid="api_product_resources_type">
                                                    <div className="flex items-center gap-2">
                                                        <PluginIcon icon={plugin?.icon} className="size-5" />
                                                        <span>{plugin?.name ?? resource.type}</span>
                                                    </div>
                                                </TableCell>
                                                <TableCell className="text-right" onClick={e => e.stopPropagation()}>
                                                    {canUpdate ? (
                                                        <ApiProductResourceRowActions
                                                            resource={resource}
                                                            enabled={enabled}
                                                            isPending={mutation.isPending}
                                                            onEdit={name => navigate(`${encodeURIComponent(name)}/edit`)}
                                                            onToggle={handleToggle}
                                                            onRemove={setRemoveResourceName}
                                                        />
                                                    ) : null}
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })}
                                    {pageData.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={3} className="py-10 text-center text-muted-foreground">
                                                No resources match your search.
                                            </TableCell>
                                        </TableRow>
                                    ) : null}
                                </TableBody>
                            </Table>
                        </CardContent>
                    </Card>

                    {paginationBar}
                </>
            )}

            <ConfirmDialog
                open={removeResourceName !== null}
                onOpenChange={open => !open && setRemoveResourceName(null)}
                title="Remove resource"
                description={removeTarget ? 'Are you sure you want to remove this resource?' : undefined}
                confirmLabel="Remove"
                destructive
                isPending={mutation.isPending}
                icon={<Trash2Icon className="size-4" />}
                onConfirm={handleRemove}
            />
        </div>
    );
}

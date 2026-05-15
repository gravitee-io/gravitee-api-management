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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import type { ApiProductListItem } from '../../types/apiProduct';
import { SyncStatusBadge } from '../SyncStatusBadge';

function SkeletonRow() {
    return (
        <TableRow>
            <TableCell>
                <Skeleton className="h-4 w-36 rounded" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-4 w-10 rounded" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-5 w-20 rounded-full" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-4 w-16 rounded" />
            </TableCell>
            <TableCell>
                <Skeleton className="h-4 w-24 rounded" />
            </TableCell>
            <TableCell />
        </TableRow>
    );
}

function ProductActionsMenu({ productId, onNavigate }: { productId: string; onNavigate: (path: string) => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Product actions" onClick={e => e.stopPropagation()}>
                    <MoreHorizontalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-[12rem]">
                <DropdownMenuItem onSelect={() => onNavigate(`${productId}/overview`)}>View Details</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${productId}/general`)}>Edit Configuration</DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

interface ApiProductListTableProps {
    products: ApiProductListItem[];
    isLoading: boolean;
    skeletonRowCount?: number;
}

export function ApiProductListTable({ products, isLoading, skeletonRowCount = 5 }: ApiProductListTableProps) {
    const navigate = useNavigate();

    return (
        <div className="rounded-lg border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Product Name</TableHead>
                        <TableHead>Total APIs</TableHead>
                        <TableHead>Sync Status</TableHead>
                        <TableHead>Version</TableHead>
                        <TableHead>Owner</TableHead>
                        <TableHead className="w-10 text-right" />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {isLoading ? (
                        Array.from({ length: skeletonRowCount }).map((_, i) => <SkeletonRow key={i} />)
                    ) : products.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={6} className="py-10 text-center text-sm text-muted-foreground">
                                No API products found.
                            </TableCell>
                        </TableRow>
                    ) : (
                        products.map(product => (
                            <TableRow
                                key={product.id}
                                className="cursor-pointer hover:bg-accent"
                                role="button"
                                tabIndex={0}
                                onClick={() => navigate(`${product.id}/overview`)}
                                onKeyDown={e => {
                                    if (e.key === 'Enter' || e.key === ' ') {
                                        e.preventDefault();
                                        navigate(`${product.id}/overview`);
                                    }
                                }}
                            >
                                <TableCell className="font-medium">{product.name}</TableCell>
                                <TableCell>
                                    <Badge variant="secondary" className="text-xs tabular-nums">
                                        {product.apiIds?.length ?? 0}
                                    </Badge>
                                </TableCell>
                                <TableCell>
                                    <SyncStatusBadge state={product.deploymentState} />
                                </TableCell>
                                <TableCell>
                                    <Badge variant="outline" className="font-mono text-xs">
                                        {product.version}
                                    </Badge>
                                </TableCell>
                                <TableCell className="text-sm text-muted-foreground">{product.primaryOwner?.displayName ?? '—'}</TableCell>
                                <TableCell className="text-right">
                                    <ProductActionsMenu productId={product.id} onNavigate={navigate} />
                                </TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    );
}

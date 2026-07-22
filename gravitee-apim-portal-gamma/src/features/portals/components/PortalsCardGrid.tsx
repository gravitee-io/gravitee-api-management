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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { ChevronLeftIcon, ChevronRightIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { DeveloperPortal } from '../types';
import { PortalCard } from './PortalCard';

const PAGE_SIZE = 6;

interface PortalsCardGridProps {
    readonly portals: readonly DeveloperPortal[];
    readonly loading: boolean;
}

function buildPageItems(currentPage: number, totalPages: number): readonly (number | 'ellipsis')[] {
    if (totalPages <= 7) {
        return Array.from({ length: totalPages }, (_, index) => index + 1);
    }

    const pages = new Set<number>([1, totalPages, currentPage - 1, currentPage, currentPage + 1]);
    if (currentPage <= 3) {
        pages.add(2);
        pages.add(3);
        pages.add(4);
    }
    if (currentPage >= totalPages - 2) {
        pages.add(totalPages - 1);
        pages.add(totalPages - 2);
        pages.add(totalPages - 3);
    }

    const sorted = [...pages].filter(page => page >= 1 && page <= totalPages).sort((a, b) => a - b);
    const result: (number | 'ellipsis')[] = [];
    for (const page of sorted) {
        const previous = result[result.length - 1];
        if (typeof previous === 'number' && page - previous > 1) {
            result.push('ellipsis');
        }
        result.push(page);
    }
    return result;
}

function LoadingCards() {
    return (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true">
            {Array.from({ length: 3 }, (_, index) => (
                <div key={index} className="rounded-xl border p-5">
                    <div className="flex items-start justify-between gap-3">
                        <Skeleton className="h-5 w-40 rounded" />
                        <Skeleton className="h-5 w-20 rounded" />
                    </div>
                    <Skeleton className="mt-6 h-8 w-20 rounded" />
                    <Skeleton className="mt-2 h-4 w-28 rounded" />
                    <div className="mt-6 space-y-3 border-t pt-3">
                        <Skeleton className="h-4 w-full rounded" />
                        <Skeleton className="h-4 w-full rounded" />
                        <Skeleton className="h-4 w-full rounded" />
                    </div>
                    <div className="mt-5 flex gap-2">
                        <Skeleton className="h-8 flex-1 rounded" />
                        <Skeleton className="h-8 flex-1 rounded" />
                    </div>
                </div>
            ))}
        </div>
    );
}

export function PortalsCardGrid({ portals, loading }: PortalsCardGridProps) {
    const [currentPage, setCurrentPage] = useState(1);
    const portalIdsKey = portals.map(portal => portal.id).join(',');
    const totalPages = Math.max(1, Math.ceil(portals.length / PAGE_SIZE));
    const safePage = Math.min(currentPage, totalPages);

    useEffect(() => {
        setCurrentPage(1);
    }, [portalIdsKey]);

    useEffect(() => {
        if (currentPage !== safePage) {
            setCurrentPage(safePage);
        }
    }, [currentPage, safePage]);

    const pagePortals = useMemo(() => {
        const start = (safePage - 1) * PAGE_SIZE;
        return portals.slice(start, start + PAGE_SIZE);
    }, [portals, safePage]);

    const pageItems = useMemo(() => buildPageItems(safePage, totalPages), [safePage, totalPages]);

    if (loading) {
        return <LoadingCards />;
    }

    if (portals.length === 0) {
        return (
            <div className="rounded-lg border px-4 py-8 text-center text-sm text-muted-foreground">
                No portals match your filters.
            </div>
        );
    }

    const showPagination = portals.length > PAGE_SIZE;

    return (
        <div className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {pagePortals.map(portal => (
                    <PortalCard key={portal.id} portal={portal} />
                ))}
            </div>

            {showPagination && (
                <nav className="flex items-center justify-center gap-1" aria-label="Portals pagination">
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8"
                        aria-label="Previous page"
                        disabled={safePage <= 1}
                        onClick={() => setCurrentPage(page => Math.max(1, page - 1))}
                    >
                        <ChevronLeftIcon className="size-4" aria-hidden="true" />
                    </Button>

                    {pageItems.map((item, index) =>
                        item === 'ellipsis' ? (
                            <span
                                key={`ellipsis-${index}`}
                                className="px-2 text-sm text-muted-foreground"
                                aria-hidden="true"
                            >
                                …
                            </span>
                        ) : (
                            <Button
                                key={item}
                                type="button"
                                variant={item === safePage ? 'outline' : 'ghost'}
                                size="icon"
                                className="size-8"
                                aria-label={`Page ${item}`}
                                aria-current={item === safePage ? 'page' : undefined}
                                onClick={() => setCurrentPage(item)}
                            >
                                {item}
                            </Button>
                        ),
                    )}

                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8"
                        aria-label="Next page"
                        disabled={safePage >= totalPages}
                        onClick={() => setCurrentPage(page => Math.min(totalPages, page + 1))}
                    >
                        <ChevronRightIcon className="size-4" aria-hidden="true" />
                    </Button>
                </nav>
            )}
        </div>
    );
}

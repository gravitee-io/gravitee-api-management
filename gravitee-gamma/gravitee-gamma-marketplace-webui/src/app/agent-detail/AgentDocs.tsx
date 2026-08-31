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
import { Alert, AlertDescription, Button, cn, Spinner } from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { markdownToSafeHtml } from './markdown';
import { containsPage, findFirstPageId, mapToPageTree, type PageTreeNode } from './page-tree';
import { getApiPage, listApiPages } from '../../api/agent';
import type { Page } from '../../api/types';

const PAGES_ERROR = 'Unable to load documentation. Please try again.';
const CONTENT_ERROR = 'Unable to load this page. Please try again.';

export function AgentDocs({ apiId }: { apiId: string }) {
    const [searchParams, setSearchParams] = useSearchParams();
    const [pages, setPages] = useState<Page[]>([]);
    const [loadingPages, setLoadingPages] = useState(true);
    const [pagesError, setPagesError] = useState<string | null>(null);
    const [page, setPage] = useState<Page | null>(null);
    const [loadingContent, setLoadingContent] = useState(false);
    const [contentError, setContentError] = useState<string | null>(null);

    const tree = useMemo(() => mapToPageTree(pages), [pages]);
    const firstPageId = findFirstPageId(tree);
    const selectedId = searchParams.get('page') ?? firstPageId;

    useEffect(() => {
        let cancelled = false;
        setLoadingPages(true);
        setPagesError(null);

        listApiPages(apiId)
            .then(response => {
                if (!cancelled) {
                    setPages(response.data ?? []);
                    setLoadingPages(false);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setPages([]);
                    setLoadingPages(false);
                    setPagesError(PAGES_ERROR);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [apiId]);

    useEffect(() => {
        if (!selectedId) {
            setPage(null);
            setLoadingContent(false);
            setContentError(null);
            return;
        }

        let cancelled = false;
        setLoadingContent(true);
        setContentError(null);

        getApiPage(apiId, selectedId)
            .then(loaded => {
                if (!cancelled) {
                    setPage(loaded);
                    setLoadingContent(false);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setPage(null);
                    setLoadingContent(false);
                    setContentError(CONTENT_ERROR);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [apiId, selectedId]);

    const selectPage = (pageId: string) => {
        setSearchParams({ page: pageId }, { replace: true });
    };

    if (loadingPages) {
        return (
            <div className="flex justify-center py-12">
                <Spinner className="size-8" aria-label="Loading documentation" />
            </div>
        );
    }

    if (pagesError) {
        return (
            <Alert variant="destructive" role="alert">
                <AlertDescription>{pagesError}</AlertDescription>
            </Alert>
        );
    }

    if (tree.length === 0) {
        return <p className="text-sm text-muted-foreground">No documentation has been published for this agent.</p>;
    }

    return (
        <div className="flex flex-col gap-6 md:flex-row">
            <nav className="w-full shrink-0 md:w-64" aria-label="Documentation">
                <PageTreeList nodes={tree} selectedId={selectedId} onSelect={selectPage} />
            </nav>
            <article className="min-w-0 flex-1">
                {loadingContent ? (
                    <div className="flex justify-center py-8">
                        <Spinner className="size-6" aria-label="Loading page" />
                    </div>
                ) : null}
                {contentError ? (
                    <Alert variant="destructive" role="alert">
                        <AlertDescription>{contentError}</AlertDescription>
                    </Alert>
                ) : null}
                {!loadingContent && !contentError && page ? <PageContent page={page} /> : null}
            </article>
        </div>
    );
}

function PageTreeList({
    nodes,
    selectedId,
    onSelect,
}: {
    nodes: readonly PageTreeNode[];
    selectedId: string | null;
    onSelect: (pageId: string) => void;
}) {
    return (
        <ul className="space-y-1 text-sm">
            {nodes.map(node => (
                <li key={node.id}>
                    {node.isFolder ? (
                        <details open={selectedId ? containsPage(node, selectedId) : true}>
                            <summary className="cursor-pointer text-muted-foreground">{node.name}</summary>
                            <div className="pl-3 pt-1">
                                <PageTreeList nodes={node.children} selectedId={selectedId} onSelect={onSelect} />
                            </div>
                        </details>
                    ) : (
                        <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className={cn('h-auto w-full justify-start px-2 py-1 font-normal', selectedId === node.id && 'bg-accent')}
                            aria-current={selectedId === node.id ? 'page' : undefined}
                            onClick={() => onSelect(node.id)}
                        >
                            {node.name}
                        </Button>
                    )}
                </li>
            ))}
        </ul>
    );
}

function PageContent({ page }: { page: Page }) {
    if (page.type === 'MARKDOWN') {
        return (
            <div
                className="space-y-3 text-sm [&_h1]:text-lg [&_h1]:font-semibold [&_h2]:text-base [&_h2]:font-semibold"
                dangerouslySetInnerHTML={{ __html: markdownToSafeHtml(page.content ?? '') }}
            />
        );
    }
    if (page.content) {
        return <pre className="overflow-auto rounded-md border bg-muted/30 p-3 text-sm">{page.content}</pre>;
    }
    return <p className="text-sm text-muted-foreground">This page type cannot be previewed.</p>;
}

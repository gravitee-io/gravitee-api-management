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
import { Button } from '@gravitee/graphene-core';
import { Component, lazy, type ComponentType, type ErrorInfo, type ReactNode, Suspense, useState } from 'react';

import { RouteContentSkeleton } from './RouteContentSkeleton';
import { SectionError } from './SectionError';

type PageModule = { readonly default: ComponentType };

interface PageLoadBoundaryProps {
    readonly onRetry: () => void;
    readonly children: ReactNode;
}

class PageLoadBoundary extends Component<PageLoadBoundaryProps, { failed: boolean }> {
    override state = { failed: false };

    static getDerivedStateFromError(): { failed: boolean } {
        return { failed: true };
    }

    override componentDidCatch(error: unknown, _info: ErrorInfo): void {
        console.error('A lazily loaded page failed to load', error);
    }

    override render(): ReactNode {
        if (!this.state.failed) return this.props.children;
        return (
            <div className="space-y-3">
                <SectionError message="This page could not be loaded." />
                <Button type="button" variant="outline" onClick={this.props.onRetry}>
                    Retry
                </Button>
            </div>
        );
    }
}

/**
 * A page whose code is loaded on its route only. A failed load (a redeploy during the session) is reported here,
 * so the module's other pages stay usable, and Retry loads the code again: React keeps a failed lazy component failed,
 * so each attempt gets a new one.
 */
export function LazyPage({ load }: Readonly<{ load: () => Promise<PageModule> }>) {
    const [attempt, setAttempt] = useState(() => ({ count: 0, Page: lazy(load) }));
    const retry = () => setAttempt(previous => ({ count: previous.count + 1, Page: lazy(load) }));
    const { Page } = attempt;

    return (
        <PageLoadBoundary key={attempt.count} onRetry={retry}>
            <Suspense fallback={<RouteContentSkeleton />}>
                <Page />
            </Suspense>
        </PageLoadBoundary>
    );
}

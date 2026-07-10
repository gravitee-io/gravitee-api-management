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
import './monaco-setup';
import { Spinner, ThemeProvider, TooltipProvider } from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode, Suspense } from 'react';
import * as ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { App } from './app/app';
import { PortalAppProvider } from './app/PortalAppContext';
import { runApplicationBootstrap } from './bootstrap-initialize';
import constants from './constants.json';
import { ErrorBoundary } from './shared/components/ErrorBoundary';
import { PortalToaster } from './shared/components/PortalToaster';

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            retry: 1,
        },
    },
});

const appBasePath = constants.appBasePath ?? '/portal-editor';

runApplicationBootstrap().then(() => {
    const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);
    root.render(
        <StrictMode>
            <QueryClientProvider client={queryClient}>
                <BrowserRouter basename={appBasePath}>
                    <ThemeProvider defaultMode="system">
                        <TooltipProvider delayDuration={200}>
                            <PortalToaster />
                            <ErrorBoundary
                            fallback={(error, retry) => (
                                <div>
                                    <h2>Bootstrap Failed</h2>
                                    <p>{error.message}</p>
                                    <button type="button" onClick={retry}>
                                        Retry
                                    </button>
                                </div>
                            )}
                        >
                            <Suspense
                                fallback={
                                    <div className="flex min-h-screen items-center justify-center">
                                        <Spinner className="size-8" aria-label="Loading application" />
                                    </div>
                                }
                            >
                                <PortalAppProvider>
                                    <App />
                                </PortalAppProvider>
                            </Suspense>
                        </ErrorBoundary>
                        </TooltipProvider>
                    </ThemeProvider>
                </BrowserRouter>
            </QueryClientProvider>
        </StrictMode>,
    );
});

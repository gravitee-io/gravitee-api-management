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
import { ThemeProvider } from '@gravitee/graphene';
import { StrictMode, Suspense } from 'react';
import * as ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { AppRoutes } from './app/AppRoutes';
import { useAuthStore } from './features/auth';
import { useEnvironmentStore } from './features/environment';
import { ErrorBoundary } from './shared/components/ErrorBoundary';
import { useBootstrapStore } from './shared/config/bootstrap.store';

async function initialize() {
    await useBootstrapStore.getState().initialize();

    const config = useBootstrapStore.getState().config!;
    useEnvironmentStore.getState().setEnvironment(config.organizationId, 'DEFAULT');

    await useAuthStore.getState().initialize();
}

initialize().then(() => {
    const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);
    root.render(
        <StrictMode>
            <BrowserRouter>
                <ThemeProvider defaultMode="system">
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
                        <Suspense fallback={<div>Loading…</div>}>
                            <AppRoutes />
                        </Suspense>
                    </ErrorBoundary>
                </ThemeProvider>
            </BrowserRouter>
        </StrictMode>,
    );
});

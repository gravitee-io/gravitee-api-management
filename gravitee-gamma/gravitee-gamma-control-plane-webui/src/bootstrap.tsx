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
import { StrictMode, Suspense } from 'react';
import * as ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import App from './app/app';
import { AuthProvider } from './auth/auth-context';
import { BootstrapProvider } from './bootstrap/bootstrap-context';
import { EnvironmentProvider } from './bootstrap/environment-context';
import { ErrorBoundary } from './error-boundary';

const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);
root.render(
    <StrictMode>
        <BrowserRouter>
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
                    <BootstrapProvider>
                        <AuthProvider>
                            <EnvironmentProvider>
                                <App />
                            </EnvironmentProvider>
                        </AuthProvider>
                    </BootstrapProvider>
                </Suspense>
            </ErrorBoundary>
        </BrowserRouter>
    </StrictMode>,
);

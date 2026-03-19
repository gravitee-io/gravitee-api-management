import { StrictMode, Suspense } from 'react';
import * as ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import App from './app/app';
import { BootstrapProvider } from './bootstrap/bootstrap-context';
import { AuthProvider } from './auth/auth-context';
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

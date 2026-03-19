import React from 'react';
import { act, render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/auth-context';
import { BootstrapProvider } from '../bootstrap/bootstrap-context';

import App from './app';

async function renderApp() {
    await act(async () => {
        render(
            <BrowserRouter>
                <React.Suspense fallback={<p>Loading…</p>}>
                    <BootstrapProvider>
                        <AuthProvider>
                            <App />
                        </AuthProvider>
                    </BootstrapProvider>
                </React.Suspense>
            </BrowserRouter>,
        );
    });
}

describe('App', () => {
    it('should render successfully', async () => {
        await renderApp();
        expect(screen.getByText(/Welcome to Gravitee Gamma/i)).toBeTruthy();
    });
});

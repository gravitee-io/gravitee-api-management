import '@gravitee/graphene-core/fonts';
import '@gravitee/graphene-core/styles';

import { ThemeProvider } from '@gravitee/graphene-core';
import { StrictMode } from 'react';
import * as ReactDOM from 'react-dom/client';

import LocalDevRoot from './app/LocalDevRoot';

const rootElement = document.getElementById('root');
if (!rootElement) {
    throw new Error('Root element #root not found');
}

const root = ReactDOM.createRoot(rootElement);
root.render(
    <StrictMode>
        <ThemeProvider defaultMode="system">
            <LocalDevRoot />
        </ThemeProvider>
    </StrictMode>,
);

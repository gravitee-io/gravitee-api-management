import '@gravitee/graphene-core/fonts';
import '@gravitee/graphene-core/styles';
import { ThemeProvider } from '@gravitee/graphene-core';
import React from 'react';
import ReactDOM from 'react-dom/client';
/** Local dev entry (`nx serve`); mounts the full tree with router + dev shell. Not used in Module Federation. */
import LocalDevRoot from './app/LocalDevRoot';

const rootElement = document.getElementById('root');
if (!rootElement) {
    throw new Error('Root element #root not found');
}

const root = ReactDOM.createRoot(rootElement);
root.render(
    <React.StrictMode>
        <ThemeProvider defaultMode="system">
            <LocalDevRoot />
        </ThemeProvider>
    </React.StrictMode>,
);

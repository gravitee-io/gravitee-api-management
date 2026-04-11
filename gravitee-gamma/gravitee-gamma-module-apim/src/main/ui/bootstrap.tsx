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
import '@gravitee/graphene/fonts';
import '@gravitee/graphene/styles';

import { ThemeProvider } from '@gravitee/graphene';
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

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
import { join } from 'path';

import { NxModuleFederationPlugin } from '@nx/module-federation/rspack.js';
import { NxAppRspackPlugin } from '@nx/rspack/app-plugin.js';
import { NxReactRspackPlugin } from '@nx/rspack/react-plugin.js';

import config from './module-federation.config';

const APP_BASE_PATH = '/portal-editor';

const backendEnv = process.env['BACKEND_ENV'];
const backendTarget = backendEnv ? `https://${backendEnv}` : 'http://localhost:8083';

// BlockNote/TipTap pull different prosemirror-model copies; dedupe so Fragment checks work.
const prosemirrorPackages = [
    'prosemirror-model',
    'prosemirror-state',
    'prosemirror-view',
    'prosemirror-transform',
    'prosemirror-commands',
    'prosemirror-keymap',
    'prosemirror-history',
    'prosemirror-inputrules',
    'prosemirror-gapcursor',
    'prosemirror-dropcursor',
    'prosemirror-schema-list',
    'prosemirror-schema-basic',
    'prosemirror-tables',
    'prosemirror-markdown',
];

const prosemirrorAliases = Object.fromEntries(
    prosemirrorPackages.map(pkg => [pkg, join(__dirname, '../node_modules', pkg)]),
);

export default {
    output: {
        path: join(__dirname, './dist'),
        publicPath: `${APP_BASE_PATH}/`,
        uniqueName: 'portal-gamma',
    },
    experiments: {
        css: false,
    },
    devServer: {
        port: 4103,
        allowedHosts: 'all',
        headers: {
            'Access-Control-Allow-Origin': '*',
        },
        devMiddleware: {
            publicPath: `${APP_BASE_PATH}/`,
        },
        proxy: [
            {
                context: (pathname: string) => pathname === '/portal' || pathname.startsWith('/portal/'),
                target: backendTarget,
                changeOrigin: true,
                secure: false,
            },
        ],
        historyApiFallback: {
            rewrites: [{ from: new RegExp(`^${APP_BASE_PATH}($|/.*)`), to: `${APP_BASE_PATH}/index.html` }],
            disableDotRule: true,
            htmlAcceptHeaders: ['text/html', 'application/xhtml+xml'],
        },
    },
    plugins: [
        new NxAppRspackPlugin({
            tsConfig: './tsconfig.app.json',
            main: './src/main.ts',
            index: './src/index.html',
            baseHref: `${APP_BASE_PATH}/`,
            assets: ['./src/favicon.ico', './src/assets', './src/constants.json'],
            styles: [],
            outputHashing: process.env['NODE_ENV'] === 'production' ? 'all' : 'none',
            optimization: process.env['NODE_ENV'] === 'production',
        }),
        new NxReactRspackPlugin({}),
        new NxModuleFederationPlugin({ config }, { dts: false }),
    ],
    resolve: {
        alias: prosemirrorAliases,
    },
};

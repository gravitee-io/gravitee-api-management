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
import { join } from 'path';

import { NxModuleFederationPlugin } from '@nx/module-federation/rspack.js';
import { NxAppRspackPlugin } from '@nx/rspack/app-plugin.js';
import { NxReactRspackPlugin } from '@nx/rspack/react-plugin.js';
import { DefinePlugin } from '@rspack/core';

import config from './module-federation.config';

export default {
    output: {
        path: join(__dirname, './dist'),
        publicPath: 'auto',
    },
    experiments: {
        css: false,
    },
    devServer: {
        port: 4200,
        proxy: [
            {
                context: ['/management'],
                target: 'http://localhost:8083',
                changeOrigin: true,
            },
            {
                context: ['/gamma'],
                target: 'http://localhost:8083',
                changeOrigin: true,
            },
        ],
        historyApiFallback: {
            index: '/index.html',
            disableDotRule: true,
            htmlAcceptHeaders: ['text/html', 'application/xhtml+xml'],
        },
    },
    plugins: [
        new DefinePlugin({
            'process.env.DEV_MODULE_ENTRIES': JSON.stringify(process.env.DEV_MODULE_ENTRIES ?? ''),
        }),
        new NxAppRspackPlugin({
            tsConfig: './tsconfig.app.json',
            main: './src/main.ts',
            index: './src/index.html',
            baseHref: '/',
            assets: ['./src/assets', './src/constants.json'],
            styles: [],
            outputHashing: process.env['NODE_ENV'] === 'production' ? 'all' : 'none',
            optimization: process.env['NODE_ENV'] === 'production',
        }),
        new NxReactRspackPlugin({}),
        new NxModuleFederationPlugin({ config }, { dts: false }),
    ],
};

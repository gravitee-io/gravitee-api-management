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
import { NxAppRspackPlugin } from '@nx/rspack/app-plugin.js';
import { NxReactRspackPlugin } from '@nx/rspack/react-plugin.js';
import { NxModuleFederationPlugin } from '@nx/module-federation/rspack.js';
import { join } from 'path';

import config from './module-federation.config';

export default {
    output: {
        path: join(__dirname, './target/classes/ui'),
        publicPath: 'auto',
        uniqueName: 'gravitee-gamma-module-apim',
    },
    experiments: {
        css: false,
    },
    devServer: {
        port: 3001,
    },
    plugins: [
        new NxAppRspackPlugin({
            tsConfig: './tsconfig.app.json',
            main: './src/main/ui/index.tsx',
            index: './src/main/ui/index.html',
            baseHref: '/',
            outputHashing: process.env['NODE_ENV'] === 'production' ? 'all' : 'none',
            optimization: process.env['NODE_ENV'] === 'production',
        }),
        new NxReactRspackPlugin({}),
        new NxModuleFederationPlugin({ config }, { dts: false }),
    ],
};

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

import type { StorybookConfig } from '@storybook/react-webpack5';

/**
 * The host app aliases `@gravitee/gamma-modules-sdk` to a local file so the real
 * implementation is used instead of the SDK stub. Mirror that here so stories
 * resolve modules-sdk exports the same way the running app does.
 */
const gammaModulesSdkEntry = join(__dirname, '..', 'src/shared/gamma-modules-sdk.ts');

const config: StorybookConfig = {
    framework: {
        name: '@storybook/react-webpack5',
        options: {},
    },
    stories: ['../src/**/*.stories.@(ts|tsx|mdx)'],
    addons: ['@storybook/addon-docs', '@storybook/addon-links'],
    typescript: {
        // Speeds up startup; type-safety is already enforced by `nx tsc` / IDE.
        check: false,
        reactDocgen: 'react-docgen-typescript',
    },
    webpackFinal: async webpackConfig => {
        webpackConfig.resolve = webpackConfig.resolve ?? {};
        webpackConfig.resolve.alias = {
            ...(webpackConfig.resolve.alias ?? {}),
            '@gravitee/gamma-modules-sdk': gammaModulesSdkEntry,
        };

        // Storybook 9's @storybook/react-webpack5 ships no TS loader by default.
        // Wire babel-loader so .ts/.tsx are transpiled (the deps are already in node_modules
        // as transitive deps of @nx/react / rspack).
        webpackConfig.module = webpackConfig.module ?? {};
        webpackConfig.module.rules = webpackConfig.module.rules ?? [];
        webpackConfig.module.rules.push({
            test: /\.(ts|tsx)$/,
            exclude: /node_modules/,
            use: {
                loader: require.resolve('babel-loader'),
                options: {
                    babelrc: false,
                    configFile: false,
                    presets: [
                        [require.resolve('@babel/preset-env'), { targets: { esmodules: true } }],
                        [require.resolve('@babel/preset-react'), { runtime: 'automatic' }],
                        [require.resolve('@babel/preset-typescript'), { allowDeclareFields: true }],
                    ],
                },
            },
        });

        return webpackConfig;
    },
};

export default config;

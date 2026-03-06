import { NxAppRspackPlugin } from '@nx/rspack/app-plugin.js';
import { NxReactRspackPlugin } from '@nx/rspack/react-plugin.js';
import { NxModuleFederationPlugin, NxModuleFederationDevServerPlugin } from '@nx/module-federation/rspack.js';
import { join } from 'path';

import config from './module-federation.config';

export default {
    output: {
        path: join(__dirname, '../dist/gravitee-gamma'),
        publicPath: 'auto',
    },
    experiments: {
        css: false,
    },
    resolve: {
        alias: {
            '@baros': join(__dirname, '../gravitee-apim-baros/src'),
        },
    },
    devServer: {
        port: 4200,
        proxy: [
            {
                context: ['/build.json', '/constants.json', '/i18n'],
                target: 'http://localhost:4000',
            },
            {
                context: ['/management'],
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
        new NxAppRspackPlugin({
            tsConfig: './tsconfig.app.json',
            main: './src/main.ts',
            index: './src/index.html',
            baseHref: '/',
            assets: [
                './src/favicon.ico',
                './src/assets',
                {
                    glob: '**/*',
                    input: '../node_modules/@gravitee/ui-particles-angular/assets',
                    output: 'assets/',
                },
                {
                    glob: '**/*',
                    input: '../node_modules/monaco-editor',
                    output: 'assets/monaco-editor',
                },
                {
                    glob: '**/*',
                    input: '../gravitee-apim-webui-libs/gravitee-markdown/src/lib/assets/homepage',
                    output: 'assets/homepage',
                },
            ],
            styles: ['./src/styles.css'],
            outputHashing: process.env['NODE_ENV'] === 'production' ? 'all' : 'none',
            optimization: process.env['NODE_ENV'] === 'production',
        }),
        {
            apply(compiler) {
                compiler.hooks.afterPlugins.tap('TailwindPostCSSPlugin', () => {
                    const tailwindPostcss = require('@tailwindcss/postcss');
                    for (const rule of compiler.options.module?.rules || []) {
                        if (!rule.oneOf) continue;
                        for (const subRule of rule.oneOf) {
                            if (!subRule.use) continue;
                            for (const loader of subRule.use) {
                                if (typeof loader === 'object' && loader.loader && loader.loader.includes('postcss-loader') && loader.options?.postcssOptions) {
                                    const origFactory = loader.options.postcssOptions;
                                    loader.options.postcssOptions = (loaderContext) => {
                                        const opts = typeof origFactory === 'function' ? origFactory(loaderContext) : origFactory;
                                        opts.plugins = [tailwindPostcss(), ...(opts.plugins || [])];
                                        return opts;
                                    };
                                }
                            }
                        }
                    }
                });
            },
        },
        new NxReactRspackPlugin({
            // Uncomment this line if you don't want to use SVGR
            // See: https://react-svgr.com/
            // svgr: false
        }),
        new NxModuleFederationPlugin({ config }, { dts: false }),
        new NxModuleFederationDevServerPlugin({ config }),
    ],
};

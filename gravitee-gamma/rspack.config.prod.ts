import { NxAppRspackPlugin } from '@nx/rspack/app-plugin.js';
import { NxReactRspackPlugin } from '@nx/rspack/react-plugin.js';
import { NxModuleFederationPlugin, NxModuleFederationDevServerPlugin } from '@nx/module-federation/rspack.js';
import { ModuleFederationConfig } from '@nx/module-federation';
import { join } from 'path';

import baseConfig from './module-federation.config';

const prodConfig: ModuleFederationConfig = {
    ...baseConfig,
    /*
     * Remote overrides for production.
     * Each entry is a pair of a unique name and the URL where it is deployed.
     *
     * e.g.
     * remotes: [
     *   ['app1', 'http://app1.example.com'],
     *   ['app2', 'http://app2.example.com'],
     * ]
     *
     * You can also use a full path to the remoteEntry.js file if desired.
     *
     * remotes: [
     *   ['app1', 'http://example.com/path/to/app1/remoteEntry.js'],
     *   ['app2', 'http://example.com/path/to/app2/remoteEntry.js'],
     * ]
     */
    remotes: [['app-alpha', 'http://localhost:4201/']],
};

export default {
    output: {
        path: join(__dirname, '../dist/gravitee-gamma'),
        publicPath: 'auto',
    },
    devServer: {
        port: 4200,
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
            assets: ['./src/favicon.ico', './src/assets'],
            styles: ['./src/styles.css'],
            outputHashing: process.env['NODE_ENV'] === 'production' ? 'all' : 'none',
            optimization: process.env['NODE_ENV'] === 'production',
        }),
        new NxReactRspackPlugin({
            // Uncomment this line if you don't want to use SVGR
            // See: https://react-svgr.com/
            // svgr: false
        }),
        new NxModuleFederationPlugin({ config: prodConfig }, { dts: false }),
        new NxModuleFederationDevServerPlugin({ config: prodConfig }),
    ],
};

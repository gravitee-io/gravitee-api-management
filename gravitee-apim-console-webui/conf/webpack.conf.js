/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
const path = require('path');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

const env = process.env.BACKEND_ENV;

module.exports = {
  mode: 'development',
  module: {
    rules: [
      {
        test: /\.(scss)$/,
        include: path.resolve(__dirname, '..', 'src', 'index.scss'),
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              webpackImporter: false,
              sassOptions: {
                includePaths: ['node_modules'],
              },
            },
          },
        ],
      },
      {
        test: /\.(scss)$/,
        exclude: path.resolve(__dirname, '..', 'src', 'index.scss'),
        use: ['to-string-loader', 'css-loader', 'sass-loader'],
      },
      { test: /\.css$/, use: ['style-loader', 'css-loader'] },
      {
        test: /\.ts$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'ng-annotate-loader',
            options: {
              ngAnnotate: 'ng-annotate-patched',
              es6: true,
              explicitOnly: false,
            },
          },
          'ts-loader',
        ],
      },
      {
        test: /\.mjs$/,
        include: /node_modules/,
        type: 'javascript/auto',
        resolve: { mainFields: ['es2015', 'browser', 'module', 'main'] },
      },
      {
        test: /\.html$/i,
        use: ['ignore-loader'],
        include: /node_modules\/codemirror/,
      },
      {
        test: /.html$/,
        use: ['html-loader'],
      },
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        use: ['file-loader?hash=sha512&digest=hex&name=[hash].[ext]'],
      },
      {
        test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        use: ['url-loader?limit=10000&minetype=application/font-woff'],
      },
      {
        test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        use: ['file-loader'],
      },
    ],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, '..', 'src', 'index.html'),
    }),
    new CopyWebpackPlugin({
      patterns: [
        {
          from: './constants.json',
          to: '',
        },
        {
          from: './build.json',
          to: '',
        },
        {
          from: './themes',
          to: './themes',
        },
        {
          from: './docs',
          to: './docs',
        },
        {
          from: './node_modules/@webcomponents/webcomponentsjs/webcomponents-loader.js',
          to: 'webcomponents/webcomponents-loader.js',
        },
        {
          from: './node_modules/@gravitee/ui-components/assets/css',
          to: 'css',
        },
        {
          from: './node_modules/@gravitee/ui-components/assets/i18n',
          to: 'i18n',
        },
        {
          from: './node_modules/@gravitee/ui-components/assets/icons',
          to: 'icons',
        },
        {
          from: './node_modules/@gravitee/ui-particles-angular/assets',
          to: 'assets',
        },
        {
          from: './src/assets',
          to: 'assets',
        },
        {
          from: './src/libraries',
          to: 'libraries',
        },
        {
          from: './src/favicon.ico',
          to: '',
        },
      ],
    }),
  ],
  devtool: 'inline-source-map',
  output: {
    path: path.join(process.cwd(), '.tmp'),
    filename: '[name].js',
  },
  resolve: {
    extensions: ['.webpack.js', '.web.js', '.mjs', '.js', '.ts', '.json'],
    mainFields: ['es2015_ivy_ngcc', 'module_ivy_ngcc', 'main_ivy_ngcc', 'es2015', 'browser', 'module', 'main'],
  },
  entry: `./${path.join('src', 'index')}`,
  devServer: {
    port: 3000,
    proxy: {
      '/management': {
        target: env ? `https://${env}` : 'http://localhost:8083',
        secure: false,
        changeOrigin: true,
        onProxyReq: function (proxyReq, req, res) {
          proxyReq.setHeader('origin', 'https://apim-master-console.team-apim.gravitee.xyz');
        },
      },
    },
    client: {
      overlay: {
        errors: true,
        warnings: false,
      },
    },
  },
};

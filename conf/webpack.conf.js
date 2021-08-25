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
const webpack = require('webpack');
const path = require('path');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const autoprefixer = require('autoprefixer');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');

const env = process.env.BACKEND_ENV;

module.exports = {
  mode: 'development',
  module: {
    rules: [
      {
        test: /\.(scss)$/,
        include: [path.resolve(__dirname, '../src/index.scss')],
        use: ['style-loader', 'css-loader', 'sass-loader'],
      },
      {
        test: /\.(scss)$/,
        exclude: [path.resolve(__dirname, '../src/index.scss')],
        use: ['to-string-loader', 'css-loader', 'sass-loader'],
      },
      { test: /\.css$/, use: ['style-loader', 'css-loader'] },
      {
        test: /\.ts$/,
        exclude: /node_modules/,
        use: ['ng-annotate-loader', 'ts-loader'],
      },
      {
        test: /\.html$/i,
        loader: 'ignore-loader',
        include: /node_modules\/codemirror/,
      },
      {
        test: /.html$/,
        loader: 'html-loader',
      },
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        loader: 'file-loader?hash=sha512&digest=hex&name=[hash].[ext]',
      },
      {
        test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        loader: 'url-loader?limit=10000&minetype=application/font-woff',
      },
      {
        test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        loader: 'file-loader',
      },
    ],
  },
  plugins: [
    new ForkTsCheckerWebpackPlugin(),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, '..', 'src', 'index.html'),
    }),
    new webpack.LoaderOptionsPlugin({
      options: {
        postcss: () => [autoprefixer],
        resolve: {},
        ts: {
          configFileName: 'tsconfig.json',
        },
      },
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
    extensions: ['.webpack.js', '.web.js', '.js', '.ts', '.json'],
  },
  entry: `./${path.join('src', 'index')}`,
  node: {
    fs: 'empty',
    module: 'empty',
  },
  devServer: {
    port: 3000,
    proxy: {
      '/management': {
        target: env ? `https://${env}.gravitee.io` : 'http://localhost:8083',
        changeOrigin: !!env,
        secure: false,
      },
    },
  },
};

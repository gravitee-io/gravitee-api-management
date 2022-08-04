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
'use strict';

const webpack = require('webpack');
const path = require('path');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

module.exports = {
  mode: 'production',
  module: {
    rules: [
      {
        test: /\.(scss)$/,
        use: [
          MiniCssExtractPlugin.loader,
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
        include: path.resolve(__dirname, '..', 'src', 'index.scss'),
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
          'ts-loader?transpileOnly=true',
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
        loader: 'html-loader',
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
    new CleanWebpackPlugin(),
    new webpack.ProvidePlugin({
      $: 'jquery',
      jQuery: 'jquery',
      moment: 'moment',
      tinycolor: 'tinycolor2',
      Highcharts: 'highcharts',
    }),
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, '..', 'src', 'index.html'),
    }),
    new MiniCssExtractPlugin(),
    new CopyWebpackPlugin({
      patterns: [
        {
          from: `./constants.prod.json`,
          to: 'constants.json',
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
        { from: './src/swagger-oauth2-redirect.html', to: './swagger-oauth2-redirect.html' },
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
  output: {
    path: path.join(process.cwd(), 'dist'),
    filename: '[name]-[hash].js',
  },
  resolve: {
    extensions: ['.webpack.js', '.web.js', '.mjs', '.js', '.ts', '.json'],
    mainFields: ['es2015_ivy_ngcc', 'module_ivy_ngcc', 'main_ivy_ngcc', 'es2015', 'browser', 'module', 'main'],
  },
  entry: {
    app: `./${path.join('src', 'index')}`,
  },
  externals: [{ 'api-console': {}, unicode: {} }],
  optimization: {
    minimize: true,
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        default: false,
        // Merge all the CSS into one file
        styles: {
          name: 'styles',
          test: /\.s?css$/,
          chunks: 'all',
          minChunks: 1,
          reuseExistingChunk: true,
          enforce: true,
        },
      },
    },
  },
};

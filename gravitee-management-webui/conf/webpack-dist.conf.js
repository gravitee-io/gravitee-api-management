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
const conf = require('./gulp.conf');
const path = require('path');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const autoprefixer = require('autoprefixer');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
  mode: 'production',
  module: {
    rules: [
      {
        test: /\.ts$/,
        exclude: /node_modules/,
        use: 'tslint-loader',
        enforce: 'pre',
      },
      {
        test: /\.(scss)$/,
        loaders: ExtractTextPlugin.extract({
          fallback: 'style-loader',
          use: 'css-loader!sass-loader!postcss-loader',
        }),
        include: [path.resolve(__dirname, '..') + '/src/index.scss'],
      },
      { test: /\.css$/, loaders: ['style-loader', 'css-loader'] },
      {
        test: /\.ts$/,
        exclude: /node_modules/,
        loaders: ['ng-annotate-loader', 'ts-loader?transpileOnly=true'],
      },
      {
        test: /\.html$/,
        exclude: /node_modules/,
        loader: 'html-loader',
        options: {
          minimize: true,
          removeComments: true,
          collapseWhitespace: true,
          removeAttributeQuotes: true,
        },
      },
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        loaders: ['file-loader?hash=sha512&digest=hex&name=[hash].[ext]'],
      },
      {
        test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        use: 'url-loader?limit=10000&minetype=application/font-woff',
      },
      {
        test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        use: 'file-loader',
      },
    ],
  },
  plugins: [
    new webpack.ProvidePlugin({
      $: 'jquery',
      jQuery: 'jquery',
      moment: 'moment',
      tinycolor: 'tinycolor2',
      Highcharts: 'highcharts',
    }),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.NoEmitOnErrorsPlugin(),
    new HtmlWebpackPlugin({
      template: conf.path.src('index.html'),
    }),
    new ExtractTextPlugin('index-[hash].css'),
    new webpack.LoaderOptionsPlugin({
      options: {
        postcss: () => [autoprefixer],
        resolve: {},
        ts: {
          configFileName: 'tsconfig.json',
        },
        tslint: {
          configuration: require('../tslint.json'),
        },
      },
    }),
    new CopyWebpackPlugin(
      [
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
        { from: './src/swagger-oauth2-redirect.html', to: './swagger-oauth2-redirect.html' },
      ],
      {
        copyUnmodified: true,
      },
    ),
  ],
  output: {
    path: path.join(process.cwd(), conf.paths.dist),
    filename: '[name]-[hash].js',
  },
  resolve: {
    alias: {
      'read-more': 'read-more/js/directives/readmore.js',
    },
    extensions: ['.webpack.js', '.web.js', '.js', '.ts', '.json'],
  },
  entry: {
    app: `./${conf.path.src('index')}`,
  },
  node: {
    fs: 'empty',
    module: 'empty',
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

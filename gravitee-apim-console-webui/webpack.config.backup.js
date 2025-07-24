const path = require('path');

/**
 * Custom webpack configuration for Angular application
 * This file allows you to customize the webpack build process
 * 
 * Usage:
 * - Add custom loaders, plugins, or resolve configurations
 * - Modify the build process for specific requirements
 * - Add custom webpack rules for special file types
 */

module.exports = (config, options) => {
  // Add Monaco Editor CSS handling
  config.module.rules.push({
    test: /node_modules\/monaco-editor\/.*\.css$/,
    use: ['style-loader', 'css-loader'],
    type: 'javascript/auto'
  });

  return config;
};

/**
 * To use this webpack configuration with Angular CLI:
 * 
 * 1. Install @angular-builders/custom-webpack:
 *    yarn add -D @angular-builders/custom-webpack
 * 
 * 2. Update angular.json to use the custom webpack builder:
 *    "builder": "@angular-builders/custom-webpack:browser"
 *    "options": {
 *      "customWebpackConfig": {
 *        "path": "./webpack.config.js"
 *      }
 *    }
 * 
 * 3. For the dev server:
 *    "builder": "@angular-builders/custom-webpack:dev-server"
 *    "options": {
 *      "customWebpackConfig": {
 *        "path": "./webpack.config.js"
 *      }
 *    }
 */ 
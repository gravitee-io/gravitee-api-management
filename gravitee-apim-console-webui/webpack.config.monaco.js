const path = require('path');

/**
 * Monaco Editor specific webpack configuration
 * This configuration handles Monaco Editor's CSS, workers, and other requirements
 */

module.exports = (config, options) => {
  console.log('Monaco Editor webpack config is running...');

  // Handle Monaco Editor CSS files
  config.module.rules.push({
    test: /node_modules\/monaco-editor\/.*\.css$/,
    use: ['style-loader', 'css-loader'],
    type: 'javascript/auto'
  });

  // Handle Monaco Editor worker files
  config.module.rules.push({
    test: /monaco-editor\/esm\/vs\/editor\/editor\.worker\.js$/,
    use: { loader: 'worker-loader' },
    type: 'javascript/auto'
  });

  // Handle Monaco Editor's web workers
  config.module.rules.push({
    test: /node_modules\/monaco-editor\/esm\/vs\/.*\.worker\.js$/,
    use: { loader: 'worker-loader' },
    type: 'javascript/auto'
  });

  // Add Monaco Editor to externals if needed (for CDN usage)
  // config.externals = {
  //   ...config.externals,
  //   'monaco-editor': 'monaco'
  // };

  // Configure Monaco Editor's public path
  config.output = {
    ...config.output,
    globalObject: 'self'
  };

  // Add Monaco Editor specific plugins
  config.plugins.push(
    // Define Monaco Editor environment
    new (require('webpack')).DefinePlugin({
      'process.env.MONACO_EDITOR_PUBLIC_PATH': JSON.stringify('/assets/monaco-editor/')
    })
  );

  // Handle Monaco Editor's language files
  config.module.rules.push({
    test: /node_modules\/monaco-editor\/esm\/vs\/language\/.*\.js$/,
    use: 'babel-loader',
    type: 'javascript/auto'
  });

  // Handle Monaco Editor's theme files
  config.module.rules.push({
    test: /node_modules\/monaco-editor\/esm\/vs\/editor\/standalone\/browser\/.*\.js$/,
    use: 'babel-loader',
    type: 'javascript/auto'
  });

  return config;
};

/**
 * Usage:
 * 
 * 1. Install additional dependencies if needed:
 *    yarn add -D worker-loader babel-loader
 * 
 * 2. Update angular.json to use this config:
 *    "customWebpackConfig": {
 *      "path": "./webpack.config.monaco.js"
 *    }
 * 
 * 3. If using Monaco Editor with workers, you may need to configure
 *    the Monaco Editor instance in your TypeScript code:
 * 
 *    import * as monaco from 'monaco-editor';
 *    
 *    // Configure Monaco Editor
 *    self.MonacoEnvironment = {
 *      getWorkerUrl: function (moduleId, label) {
 *        if (label === 'json') {
 *          return '/assets/monaco-editor/json.worker.js';
 *        }
 *        if (label === 'css' || label === 'scss' || label === 'less') {
 *          return '/assets/monaco-editor/css.worker.js';
 *        }
 *        if (label === 'html' || label === 'handlebars' || label === 'razor') {
 *          return '/assets/monaco-editor/html.worker.js';
 *        }
 *        if (label === 'typescript' || label === 'javascript') {
 *          return '/assets/monaco-editor/ts.worker.js';
 *        }
 *        return '/assets/monaco-editor/editor.worker.js';
 *      }
 *    };
 */ 
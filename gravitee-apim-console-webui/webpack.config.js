const path = require('path');

/**
 * Webpack configuration that Angular CLI will automatically detect
 * This handles Monaco Editor CSS files
 */

module.exports = {
  module: {
    rules: [
      {
        test: /node_modules\/monaco-editor\/.*\.css$/,
        use: ['style-loader', 'css-loader'],
        type: 'javascript/auto'
      }
    ]
  }
}; 
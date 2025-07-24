const path = require('path');

/**
 * Simple webpack configuration for Monaco Editor CSS handling
 * This can be used with the standard Angular builder
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
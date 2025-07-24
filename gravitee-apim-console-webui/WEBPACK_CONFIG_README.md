# Webpack Configuration for Angular Application

This directory contains webpack configuration files that allow you to customize the build process for your Angular application.

## Files Overview

- `webpack.config.js` - Basic webpack configuration template with Monaco Editor CSS support
- `webpack.config.monaco.js` - Specialized configuration for Monaco Editor (CSS, workers, etc.)
- `WEBPACK_CONFIG_README.md` - This documentation file

## Setup Instructions

### 1. Install Required Dependencies

```bash
yarn add -D @angular-builders/custom-webpack
```

### 2. Update angular.json

To use the basic webpack configuration:

```json
{
  "architect": {
    "build": {
      "builder": "@angular-builders/custom-webpack:browser",
      "options": {
        "customWebpackConfig": {
          "path": "./webpack.config.js"
        }
      }
    },
    "serve": {
      "builder": "@angular-builders/custom-webpack:dev-server",
      "options": {
        "customWebpackConfig": {
          "path": "./webpack.config.js"
        }
      }
    }
  }
}
```

To use the Monaco Editor specific configuration:

```json
{
  "architect": {
    "build": {
      "builder": "@angular-builders/custom-webpack:browser",
      "options": {
        "customWebpackConfig": {
          "path": "./webpack.config.monaco.js"
        }
      }
    },
    "serve": {
      "builder": "@angular-builders/custom-webpack:dev-server",
      "options": {
        "customWebpackConfig": {
          "path": "./webpack.config.monaco.js"
        }
      }
    }
  }
}
```

## Common Use Cases

### 1. Monaco Editor CSS Support

If you're using Monaco Editor and encountering CSS parsing errors, use the Monaco-specific configuration:

```javascript
// In webpack.config.monaco.js
config.module.rules.push({
  test: /node_modules\/monaco-editor\/.*\.css$/,
  use: ['style-loader', 'css-loader'],
  type: 'javascript/auto'
});
```

### 2. Adding Path Aliases

In `webpack.config.advanced.js`, uncomment and modify the alias section:

```javascript
config.resolve.alias = {
  ...config.resolve.alias,
  '@shared': path.resolve(__dirname, 'src/shared'),
  '@components': path.resolve(__dirname, 'src/components'),
  '@services': path.resolve(__dirname, 'src/services'),
  '@utils': path.resolve(__dirname, 'src/util'),
};
```

### 2. Adding Environment Variables

```javascript
config.plugins.push(
  new webpack.DefinePlugin({
    'process.env.API_URL': JSON.stringify('https://api.example.com'),
    'process.env.ENVIRONMENT': JSON.stringify('production'),
  })
);
```

### 3. Adding Custom Loaders

```javascript
config.module.rules.push({
  test: /\.custom$/,
  use: 'custom-loader',
  type: 'javascript/auto'
});
```

### 4. Adding External Dependencies

```javascript
config.externals = {
  ...config.externals,
  'jquery': 'jQuery',
  'lodash': '_',
};
```

### 5. Custom Chunk Splitting

```javascript
config.optimization.splitChunks = {
  ...config.optimization.splitChunks,
  cacheGroups: {
    ...config.optimization.splitChunks.cacheGroups,
    vendor: {
      test: /[\\/]node_modules[\\/]/,
      name: 'vendors',
      chunks: 'all',
    },
  },
};
```

### 6. Adding Custom Headers (Development)

```javascript
if (options.devServer) {
  options.devServer.headers = {
    'Access-Control-Allow-Origin': '*',
    'X-Custom-Header': 'custom-value',
  };
}
```

## Testing Your Configuration

1. **Development Server:**
   ```bash
   yarn start
   ```

2. **Production Build:**
   ```bash
   yarn build
   ```

3. **Check for Errors:**
   - Monitor the console output for any webpack configuration errors
   - Check the browser console for runtime errors
   - Verify that your custom configurations are working as expected

## Troubleshooting

### Common Issues

1. **Module Not Found Errors:**
   - Check that path aliases are correctly configured
   - Verify that the paths exist in your project structure

2. **Build Failures:**
   - Check the webpack configuration syntax
   - Ensure all required dependencies are installed
   - Verify that the custom webpack builder is properly configured in `angular.json`

3. **Runtime Errors:**
   - Check that external dependencies are properly configured
   - Verify that environment variables are correctly defined
   - Ensure that custom loaders are compatible with your file types

### Debugging Tips

1. **Enable Webpack Debug Logging:**
   ```bash
   yarn start --verbose
   ```

2. **Check Webpack Configuration:**
   - Add `console.log(config)` in your webpack config to see the full configuration
   - Use webpack-bundle-analyzer to analyze your bundle

3. **Verify Configuration Loading:**
   - Add a `console.log('Webpack config loaded')` at the top of your config file
   - Check that the message appears when building

## Best Practices

1. **Start Simple:** Begin with the basic configuration and add complexity as needed
2. **Test Incrementally:** Add one configuration at a time and test thoroughly
3. **Document Changes:** Keep track of what configurations you've added and why
4. **Use Comments:** Document your custom configurations for future reference
5. **Version Control:** Commit your webpack configurations to version control

## Additional Resources

- [Angular Custom Webpack Builder Documentation](https://github.com/just-jeb/angular-builders)
- [Webpack Documentation](https://webpack.js.org/concepts/)
- [Angular CLI Build Configuration](https://angular.io/cli/build)

## Support

If you encounter issues with the webpack configuration:

1. Check the Angular CLI and webpack documentation
2. Review the console output for specific error messages
3. Verify that all dependencies are compatible with your Angular version
4. Consider using the basic configuration first before moving to the advanced one 
const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
  framework: '@storybook/angular',
  stories: ['../src/**/*.stories.@(ts|mdx)'],
  addons: ['storybook-addon-designs', '@storybook/addon-essentials'],
  features: {
    previewCsfV3: true,
    storyStoreV7: true,
  },
  angularOptions: {
    enableIvy: true,
  },
  webpackFinal: (config) => {
    // First remove some rules we don't want anymore
    const filteredDefaultRules = config.module.rules.filter(
      (rule) => rule.test.toString() !== /\.html$/.toString() && rule.test.toString() !== /\.s(c|a)ss$/.toString(),
    );
    config.module.rules = [
      ...filteredDefaultRules,
      {
        test: /\.(scss)$/,
        include: [path.resolve(__dirname, '../src/index.scss')],
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
        exclude: [path.resolve(__dirname, '../src/index.scss')],
        use: [
          'to-string-loader',
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
        test: /\.html$/,
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
    ];

    config.plugins.push(
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
    );

    return config;
  },
  previewHead: (head) =>
    `${head}
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">`,
};

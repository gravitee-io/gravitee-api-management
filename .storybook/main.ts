const path = require('path');

module.exports = {
  stories: ['../**/*.stories.@(ts|mdx)'],
  addons: ['storybook-addon-designs','@storybook/addon-essentials'],
  features: {
    previewCsfV3: true,
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
        use: ['to-string-loader', 'css-loader', {
          loader: 'sass-loader',
            options: {
              webpackImporter: false,
              sassOptions: {
                includePaths: ['node_modules'],
              },
          },
        }],
      },
      { test: /\.css$/, use: ['style-loader', 'css-loader'] },
      {
        test: /\.ts$/,
        exclude: /node_modules/,
        use: ['ng-annotate-loader', 'ts-loader'],
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

    return config;
  },
};

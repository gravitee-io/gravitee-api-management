const path = require('path');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const CopyPlugin = require('copy-webpack-plugin');
const glob = require('glob');

module.exports = {
  mode: 'production',
  entry: glob
    .sync('./src/**/*test*.ts', { realpath: false })
    // Transform the entries to keep the tree structure of our tests
    .reduce((acc, value) => {
      const key = `${path.dirname(value)}/${path.parse(value).name}`.replace('./', '');
      acc[key] = value;
      return acc;
    }, {}),
  output: {
    path: path.join(__dirname, 'dist'),
    libraryTarget: 'commonjs',
    filename: '[name].js',
  },
  resolve: {
    extensions: ['.ts', '.js'],
    alias: {
      '@env': path.resolve(process.cwd(), './src/env'),
      '@helpers': path.resolve(process.cwd(), './src/lib/helpers'),
      '@models': path.resolve(process.cwd(), './src/lib/models'),
      '@fixtures': path.resolve(process.cwd(), './src/lib/fixtures'),
      '@clients': path.resolve(process.cwd(), './src/lib/clients'),
    },
  },
  module: {
    rules: [
      {
        test: /\.([tj])s$/,
        use: 'babel-loader',
        exclude: {
          and: [/node_modules/],
        },
      },
    ],
  },
  target: 'web',
  externals: /^(k6|https?\:\/\/)(\/.*)?/,
  // Generate map files for compiled scripts
  devtool: 'source-map',
  stats: {
    colors: true,
  },
  plugins: [
    new CleanWebpackPlugin(),
    // Copy assets to the destination folder
    new CopyPlugin({
      patterns: [
        {
          from: path.resolve(__dirname, 'assets'),
          noErrorOnMissing: true,
        },
      ],
    }),
  ],
  optimization: {
    // Don't minimize, as it's not used in the browser
    minimize: false,
  },
};

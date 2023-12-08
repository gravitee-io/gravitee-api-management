module.exports = {
  framework: '@storybook/angular',
  stories: ['../src/**/*.stories.@(ts|mdx)'],
  addons: ['storybook-addon-designs', '@storybook/addon-essentials'],
  features: {
    previewCsfV3: true,
    storyStoreV7: true,
    postcss: false,
  },
  angularOptions: {
    enableIvy: true,
  },
  core: {
    builder: 'webpack5',
  },
  webpackFinal: async (config) => {
    config.optimization.minimize = false;
    return config;
  },
  previewHead: (head) =>
    `${head}
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">`,
};

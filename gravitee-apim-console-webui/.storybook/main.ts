module.exports = {
  framework: {
    name: '@storybook/angular',

    options: {
      enableIvy: true,
    },
  },

  staticDirs: [{ from: '../node_modules/monaco-editor', to: '/assets/monaco-editor' }],

  stories: ['../src/**/*.stories.@(ts|mdx)'],
  addons: ['@storybook/addon-essentials'],

  features: {
    previewCsfV3: true,
    storyStoreV7: true,
    postcss: false,
  },

  webpackFinal: async (config) => {
    config.optimization.minimize = false;
    return config;
  },

  previewHead: (head) =>
    `${head}
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">`,
};

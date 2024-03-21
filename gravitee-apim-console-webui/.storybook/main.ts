import { StorybookConfig } from '@storybook/angular';

const config: StorybookConfig = {
  framework: {
    name: '@storybook/angular',

    options: {
      enableIvy: true,
    },
  },
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
  staticDirs: [
    {
      from: '../node_modules/@gravitee/ui-particles-angular/assets',
      to: '/assets',
    },
  ],
};

export default config;

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
  features: {},
  webpackFinal: async (config) => {
    config.optimization.minimize = false;
    return config;
  },
  staticDirs: [
    {
      from: '../../node_modules/@gravitee/ui-particles-angular/assets',
      to: '/assets',
    },
    { from: '../../node_modules/monaco-editor', to: '/assets/monaco-editor' },
  ],
};

export default config;

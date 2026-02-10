import type { StorybookConfig } from '@storybook/angular';

const config: StorybookConfig = {
  stories: ['../src/**/*.mdx', '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)'],
  addons: ['@storybook/addon-links', '@storybook/addon-docs'],
  framework: {
    name: '@storybook/angular',
    options: {},
  },
  docs: {},
  staticDirs: [
    { from: '../../../node_modules/monaco-editor', to: '/assets/monaco-editor' },
    { from: '../src/lib/assets/homepage', to: 'assets/homepage' },
  ],
};

export default config;

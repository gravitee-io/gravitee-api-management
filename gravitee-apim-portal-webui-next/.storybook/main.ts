import type { StorybookConfig } from '@storybook/angular';

const config: StorybookConfig = {
  stories: [
    '../src/**/*.mdx',
    '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)',
    '../projects/gravitee-markdown/src/**/*.stories.@(js|jsx|ts|tsx|mdx)',
  ],
  addons: [
    '@storybook/addon-links',
    '@storybook/addon-essentials',
    '@storybook/addon-interactions'
  ],
  framework: {
    name: '@storybook/angular',
    options: {},
  },
  docs: {},
  staticDirs: [
    { from: '../node_modules/monaco-editor', to: '/assets/monaco-editor' },

  ],
  // webpackFinal: async (config:any) => {
  //   config.module.rules.push({
  //     test: /node_modules\/monaco-editor\/.*\.css$/,
  //     use: ['style-loader', 'css-loader'],
  //   });
  //   return config;
  // },
};

export default config;

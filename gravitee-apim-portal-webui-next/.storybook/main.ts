import type { StorybookConfig } from '@storybook/angular';

const config: StorybookConfig = {
  stories: [
    '../src/**/*.mdx',
    '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)',
    '../projects/gravitee-markdown/src/**/*.stories.@(js|jsx|ts|tsx|mdx)',
    '../projects/gravitee-dashboard/src/**/*.stories.@(js|jsx|ts|tsx|mdx)',
  ],
  addons: ['@storybook/addon-links', '@storybook/addon-essentials', '@chromatic-com/storybook', '@storybook/addon-interactions'],
  framework: {
    name: '@storybook/angular',
    options: {},
  },
  docs: {},
  staticDirs: [
    { from: '../src/assets', to: 'assets' },
    { from: '../node_modules/monaco-editor', to: '/assets/monaco-editor' },
    { from: '../projects/gravitee-markdown/src/lib/assets/homepage', to: 'assets/homepage' },
  ],
};
export default config;

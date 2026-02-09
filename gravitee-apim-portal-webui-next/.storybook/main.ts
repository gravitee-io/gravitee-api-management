import type { StorybookConfig } from '@storybook/angular';

const config: StorybookConfig = {
  stories: [
    '../src/**/*.mdx',
    '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)',
    // TODO(tar): Improve storybook with lib
    // '../../gravitee-apim-webui-libs/gravitee-markdown/src/**/*.stories.@(js|jsx|ts|tsx|mdx)',
    // '../../gravitee-apim-webui-libs/gravitee-dashboard/src/**/*.stories.@(js|jsx|ts|tsx|mdx)',
  ],
  addons: ['@storybook/addon-links', '@storybook/addon-docs'],
  framework: {
    name: '@storybook/angular',
    options: {},
  },
  docs: {},
  staticDirs: [
    { from: '../src/assets', to: 'assets' },
    { from: '../../node_modules/monaco-editor', to: '/assets/monaco-editor' },
    { from: '../../gravitee-apim-webui-libs/gravitee-markdown/src/lib/assets/homepage', to: 'assets/homepage' },
  ],
};
export default config;

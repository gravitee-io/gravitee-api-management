import type { StorybookConfig } from '@storybook/angular';

// This is a composition-only Storybook that aggregates all project Storybooks via refs.
// It has no stories of its own — the preview build is skipped using --preview-url in the
// build command (see project.json), which avoids requiring an Angular browserTarget.
const config: StorybookConfig = {
  framework: {
    name: '@storybook/angular',
    options: {},
  },
  stories: [],
  core: {
    disableTelemetry: true,
  },
  refs: {
    console: { title: 'Console', url: './console' },
    'portal-next': { title: 'Portal Next', url: './portal-next' },
    markdown: { title: 'Markdown', url: './markdown' },
    dashboard: { title: 'Dashboard', url: './dashboard' },
    'kafka-explorer': { title: 'Kafka Explorer', url: './kafka-explorer' },
  },
};

export default config;

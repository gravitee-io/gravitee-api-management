import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { applicationConfig } from '@storybook/angular';
import type { Preview } from '@storybook/angular';
import 'chart.js/auto';

const preview: Preview = {
  decorators: [
    applicationConfig({
      providers: [provideAnimationsAsync()],
    }),
  ],
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
};

export default preview;

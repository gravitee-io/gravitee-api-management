// Import all styles of the app
import { withDesign } from 'storybook-addon-designs';
import { moduleMetadata } from '@storybook/angular';
import { GioMatConfigModule } from '@gravitee/ui-particles-angular';

// Set material classes to the Storybook root div
window.document.body.parentElement.classList.add('mat');
window.document.body.classList.add('mat-typography');
window.document.body.classList.add('mat-app-background');

export const decorators = [withDesign, moduleMetadata({ imports: [GioMatConfigModule] })];

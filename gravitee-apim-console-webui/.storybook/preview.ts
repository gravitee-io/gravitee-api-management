// Import all styles of the app
import '../src/index.scss';
import { withDesign } from 'storybook-addon-designs';

// Set material classes to the Storybook root div
window.document.body.classList.add('mat');
window.document.body.classList.add('mat-typography');
window.document.body.classList.add('mat-app-background');

export const decorators = [withDesign];

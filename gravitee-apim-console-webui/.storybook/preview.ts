// Import all styles of the app
import '../src/index.scss';
import {withDesign} from "storybook-addon-designs";

// Set material classes to the Storybook root div
window.document.getElementById('root').classList.add('mat', 'mat-typography');

export const decorators = [withDesign]

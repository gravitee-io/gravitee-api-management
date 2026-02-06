// Import all styles of the app
import { applicationConfig } from '@storybook/angular';
import { importProvidersFrom } from '@angular/core';
import { provideAnimations} from '@angular/platform-browser/animations';
import { provideHttpClient} from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { CONSTANTS_TESTING } from '../src/shared/testing';
import { Constants } from '../src/entities/Constants';
import { MatSnackBarModule } from '@angular/material/snack-bar';

// Set material classes to the Storybook root div
window.document.body.parentElement.classList.add('mat');
window.document.body.classList.add('mat-typography');
window.document.body.classList.add('mat-app-background');

export const decorators = [
  applicationConfig({
    providers: [
      provideAnimations(),
      provideHttpClient(),
      provideRouter([]),
      importProvidersFrom(MatSnackBarModule),
      {
        provide: Constants,
        useValue: CONSTANTS_TESTING,
      },
    ],
  }),
];

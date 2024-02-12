// Import all styles of the app
import { applicationConfig, moduleMetadata } from '@storybook/angular';
import { GioMatConfigModule } from '@gravitee/ui-particles-angular';
import { importProvidersFrom } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { CONSTANTS_TESTING } from '../src/shared/testing';
import { MatLegacySnackBarModule } from '@angular/material/legacy-snack-bar';
import { RouterTestingModule } from '@angular/router/testing';

// Set material classes to the Storybook root div
window.document.body.parentElement.classList.add('mat');
window.document.body.classList.add('mat-typography');
window.document.body.classList.add('mat-app-background');

export const decorators = [
  moduleMetadata({ imports: [GioMatConfigModule, RouterTestingModule] }),
  applicationConfig({
    providers: [
      importProvidersFrom(BrowserAnimationsModule),
      importProvidersFrom(HttpClientModule),
      importProvidersFrom(MatLegacySnackBarModule),
      {
        provide: 'Constants',
        useValue: CONSTANTS_TESTING,
      },
    ],
  }),
];

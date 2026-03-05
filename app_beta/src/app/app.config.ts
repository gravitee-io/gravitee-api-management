import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { provideHttpClient } from '@angular/common/http';
import { PortalConstants, GioPermissionService } from '@gravitee/gravitee-portal';

import { appRoutes } from './app.routes';
import { AppBetaGioPermissionService } from '../shared/services/gio-permission.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(appRoutes),
    provideAnimationsAsync(),
    importProvidersFrom(GioIconsModule),
    provideHttpClient(),
    {
      provide: PortalConstants,
      useValue: {
        baseURL: '/management',
        env: {
          baseURL: '/management/organizations/DEFAULT/environments/DEFAULT',
          v2BaseURL: '/management/v2/environments/DEFAULT',
        },
      },
    },
    { provide: GioPermissionService, useExisting: AppBetaGioPermissionService },
  ],
};

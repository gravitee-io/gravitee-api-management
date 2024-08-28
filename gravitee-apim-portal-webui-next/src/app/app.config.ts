/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { APP_INITIALIZER, ApplicationConfig } from '@angular/core';
import { MAT_RIPPLE_GLOBAL_OPTIONS } from '@angular/material/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withComponentInputBinding, withRouterConfig } from '@angular/router';
import { combineLatest, Observable, switchMap } from 'rxjs';

import { routes } from './app.routes';
import { csrfInterceptor } from '../interceptors/csrf.interceptor';
import { httpRequestInterceptor } from '../interceptors/http-request.interceptor';
import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';
import { PortalMenuLinksService } from '../services/portal-menu-links.service';
import { ThemeService } from '../services/theme.service';

function initApp(
  configService: ConfigService,
  themeService: ThemeService,
  currentUserService: CurrentUserService,
  portalMenuLinksService: PortalMenuLinksService,
): () => Observable<unknown> {
  return () =>
    configService
      .initBaseURL()
      .pipe(
        switchMap(_ =>
          combineLatest([
            themeService.loadTheme(),
            currentUserService.loadUser(),
            configService.loadConfiguration(),
            portalMenuLinksService.loadCustomLinks(),
          ]),
        ),
      );
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding(), withRouterConfig({ paramsInheritanceStrategy: 'always' })),
    provideHttpClient(withInterceptors([httpRequestInterceptor, csrfInterceptor])),
    provideAnimations(),
    {
      provide: APP_INITIALIZER,
      useFactory: initApp,
      deps: [ConfigService, ThemeService, CurrentUserService, PortalMenuLinksService],
      multi: true,
    },
    // Ripple does not work with hsl mixing
    {
      provide: MAT_RIPPLE_GLOBAL_OPTIONS,
      useValue: {
        disabled: true,
      },
    },
  ],
};

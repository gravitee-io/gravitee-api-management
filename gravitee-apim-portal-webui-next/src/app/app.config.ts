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
import { DATE_PIPE_DEFAULT_OPTIONS } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { MAT_RIPPLE_GLOBAL_OPTIONS } from '@angular/material/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, withComponentInputBinding, withRouterConfig } from '@angular/router';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { catchError, combineLatest, Observable, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { routes } from './app.routes';
import { csrfInterceptor } from '../interceptors/csrf.interceptor';
import { httpRequestInterceptor } from '../interceptors/http-request.interceptor';
import { AuthService } from '../services/auth.service';
import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';
import { PortalMenuLinksService } from '../services/portal-menu-links.service';
import { ThemeService } from '../services/theme.service';

function initApp(
  authService: AuthService,
  configService: ConfigService,
  themeService: ThemeService,
  currentUserService: CurrentUserService,
  portalMenuLinksService: PortalMenuLinksService,
  router: Router,
): () => Observable<unknown> {
  return () =>
    configService.initBaseURL().pipe(
      switchMap(_ => authService.load()),
      switchMap(_ =>
        combineLatest([
          themeService.loadTheme(),
          currentUserService.loadUser(),
          configService.loadConfiguration(),
          portalMenuLinksService.loadCustomLinks(),
        ]),
      ),
      catchError(error => {
        router.navigate(['/503'], { state: error });
        return of({});
      }),
    );
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding(), withRouterConfig({ paramsInheritanceStrategy: 'always' })),
    provideHttpClient(withInterceptors([httpRequestInterceptor, csrfInterceptor])),
    provideAnimations(),
    provideOAuthClient(),
    provideAppInitializer(() => {
      const initializerFn = initApp(
        inject(AuthService),
        inject(ConfigService),
        inject(ThemeService),
        inject(CurrentUserService),
        inject(PortalMenuLinksService),
        inject(Router),
      );
      return initializerFn();
    }),
    // Ripple does not work with hsl mixing
    {
      provide: MAT_RIPPLE_GLOBAL_OPTIONS,
      useValue: {
        disabled: true,
      },
    },
    {
      provide: DATE_PIPE_DEFAULT_OPTIONS,
      useValue: { dateFormat: 'YYYY-MM-dd HH:mm:ss.SSS' },
    },
    provideCharts(withDefaultRegisterables()),
  ],
};

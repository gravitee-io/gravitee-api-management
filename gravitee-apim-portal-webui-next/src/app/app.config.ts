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
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withComponentInputBinding, withRouterConfig } from '@angular/router';
import { Observable, switchMap } from 'rxjs';

import { routes } from './app.routes';
import { httpRequestInterceptor } from '../interceptors/http-request.interceptor';
import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';

function initApp(configService: ConfigService, currentUserService: CurrentUserService): () => Observable<unknown> {
  return () => configService.initBaseURL().pipe(switchMap(_ => currentUserService.loadUser()));
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding(), withRouterConfig({ paramsInheritanceStrategy: 'always' })),
    provideHttpClient(withInterceptors([httpRequestInterceptor])),
    provideAnimations(),
    {
      provide: APP_INITIALIZER,
      useFactory: initApp,
      deps: [ConfigService, CurrentUserService],
      multi: true,
    },
  ],
};

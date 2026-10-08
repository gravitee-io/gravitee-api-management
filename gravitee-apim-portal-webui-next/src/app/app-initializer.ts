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
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, combineLatest, Observable, of, switchMap } from 'rxjs';

import { ServiceUnavailableState } from './service-unavailable/service-unavailable.component';
import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';
import { PortalMenuLinksService } from '../services/portal-menu-links.service';
import { ThemeService } from '../services/theme.service';

export function initApp(
  configService: ConfigService,
  themeService: ThemeService,
  currentUserService: CurrentUserService,
  portalMenuLinksService: PortalMenuLinksService,
  router: Router,
): () => Observable<unknown> {
  return () =>
    configService.initBaseURL().pipe(
      switchMap(_ =>
        combineLatest([
          themeService.loadTheme(),
          currentUserService.loadUser(),
          configService.loadConfiguration(),
          portalMenuLinksService.loadCustomLinks(),
        ]),
      ),
      catchError((error: unknown) => {
        router.navigate(['/503'], { state: toServiceUnavailableState(error) });
        return of({});
      }),
    );
}

// The router stores the state with history.pushState, which cannot clone a raw HttpErrorResponse:
// a request failing before CORS applies (e.g. rejected by the API firewall) carries a ProgressEvent as body.
function toServiceUnavailableState(error: unknown): ServiceUnavailableState {
  if (!(error instanceof HttpErrorResponse)) {
    return {};
  }
  const errors = error.error?.errors;
  return Array.isArray(errors) ? { status: error.status, errors } : { status: error.status };
}

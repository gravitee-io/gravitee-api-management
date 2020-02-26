/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

import { finalize, tap } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CurrentUserService } from '../services/current-user.service';
import { NotificationService } from '../services/notification.service';
import { LoaderService } from '../services/loader.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ConfigurationService } from '../services/configuration.service';

export const SILENT_CODES = ['errors.rating.disabled'];

@Injectable()
export class ApiRequestInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private currentUserService: CurrentUserService,
    private notificationService: NotificationService,
    private loaderService: LoaderService,
    private configService: ConfigurationService,
  ) {
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    this.loaderService.show();
    request = request.clone({
      setHeaders: {
        'X-Requested-With': 'XMLHttpRequest'
      },
      withCredentials: true
    });

    return next.handle(request).pipe(tap(
      () => {
      },
      (err: any) => {
        if (err.status === 404) {
          this.router.navigate(['/404']);
        }
        if (err instanceof HttpErrorResponse) {
          if (err.status === 0) {
            this.notificationService.error(i18n('errors.server.unavailable'));
          } else if (err.status === 401) {
            this.currentUserService.revokeUser();
          }
        }
        if (err.error && err.error.errors) {
          const error = err.error.errors[0];
          if (!SILENT_CODES.includes(error.code)) {
            this.notificationService.error(error.code, error.parameters, error.message);
          }

          if (error.status === '503') {
            // configuration has been updated, we have to reload the configuration
            this.configService.load();
          }
        }
      }
    ), finalize(() => this.loaderService.hide()));
  }
}

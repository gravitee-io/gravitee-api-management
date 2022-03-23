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
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse,
  HttpResponse,
  HttpResponseBase,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

import { tap } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CurrentUserService } from '../services/current-user.service';
import { NotificationService } from '../services/notification.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ConfigurationService } from '../services/configuration.service';
import { ReCaptchaService } from '../services/recaptcha.service';

export const SILENT_CODES = ['errors.rating.disabled', 'errors.analytics.calculate', 'errors.identityProvider.notFound'];
export const SILENT_URLS = ['/user/notifications'];

export class Future {
  private timeouts = [];
  private delay: number;

  constructor(delay = 0) {
    this.delay = delay;
  }

  push(fn) {
    this.timeouts.push(setTimeout(() => fn(), this.delay));
  }

  cancel() {
    this.timeouts.forEach(timeout => clearTimeout(timeout));
    this.timeouts = [];
  }
}

@Injectable()
export class ApiRequestInterceptor implements HttpInterceptor {
  private xsrfToken: string;

  constructor(
    private router: Router,
    private currentUserService: CurrentUserService,
    private notificationService: NotificationService,
    private configService: ConfigurationService,
    private reCaptchaService: ReCaptchaService,
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const headers = {
      'X-Requested-With': 'XMLHttpRequest',
    };

    if (this.xsrfToken) {
      headers['X-Xsrf-Token'] = this.xsrfToken;
    }

    const currentReCaptchaToken = this.reCaptchaService.getCurrentToken();
    if (currentReCaptchaToken) {
      headers['X-Recaptcha-Token'] = currentReCaptchaToken;
    }

    request = request.clone({
      setHeaders: headers,
      withCredentials: true,
    });

    return next.handle(request).pipe(
      tap(
        (event: HttpEvent<any>) => {
          if (request.url.endsWith('_export')) {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore hack because of the sdk client limitations
            request['responseType'] = 'text';
          }

          if (event instanceof HttpResponse) {
            this.saveXsrfToken(event);
          }
        },
        (err: any) => {
          const interceptorFuture = new Future();
          const silentCall = SILENT_URLS.find(silentUrl => err.url.includes(silentUrl));
          if (err instanceof HttpErrorResponse) {
            this.saveXsrfToken(err);

            if (err.status === 0) {
              if (!silentCall) {
                this.notificationService.error(i18n('errors.server.unavailable'));
              }
            } else if (err.status === 401) {
              this.currentUserService.revokeUser();
            } else if (err.status === 404) {
              const error = err.error.errors[0];
              if (!SILENT_CODES.includes(error.code) && !silentCall) {
                interceptorFuture.push(() => this.router.navigate(['/404']));
              }
            }

            if (err.error && err.error.errors) {
              const error = err.error.errors[0];
              if (!SILENT_CODES.includes(error.code) && !silentCall) {
                interceptorFuture.push(() => this.notificationService.error(error.code, error.parameters, error.message));
              }

              if (error.status === '503') {
                // configuration has been updated, we have to reload the configuration
                this.configService.load();
              }
            }
          }
          if (interceptorFuture) {
            err.interceptorFuture = interceptorFuture;
          }
        },
      ),
    );
  }

  private saveXsrfToken(response: HttpResponseBase) {
    const xsrfTokenHeader = response.headers.get('X-Xsrf-Token');

    if (xsrfTokenHeader !== null) {
      this.xsrfToken = xsrfTokenHeader;
    }
  }
}

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
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
@Injectable()
export class CsrfInterceptor implements HttpInterceptor {
  static get xsrfToken(): string {
    return localStorage.getItem('XSRF-TOKEN');
  }

  static set xsrfToken(value: string) {
    localStorage.setItem('XSRF-TOKEN', value);
  }
  public static readonly xsrfTokenHeaderName = 'X-Xsrf-Token';

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const reqWithToken = CsrfInterceptor.xsrfToken
      ? req.clone({
          headers: req.headers.set(CsrfInterceptor.xsrfTokenHeaderName, CsrfInterceptor.xsrfToken),
        })
      : req;

    return next.handle(reqWithToken).pipe(
      tap(
        event => {
          if (event instanceof HttpResponse && event.headers?.has(CsrfInterceptor.xsrfTokenHeaderName)) {
            CsrfInterceptor.xsrfToken = event.headers.get(CsrfInterceptor.xsrfTokenHeaderName);
          }
        },
        error => {
          if (error.headers?.has(CsrfInterceptor.xsrfTokenHeaderName)) {
            CsrfInterceptor.xsrfToken = error.headers.get(CsrfInterceptor.xsrfTokenHeaderName);
          }
        },
      ),
    );
  }
}

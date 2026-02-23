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
import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs/operators';

const CsrfInterceptor: {
  xsrfTokenHeaderName: string;
  xsrfToken: string | null;
} = {
  xsrfTokenHeaderName: 'X-Xsrf-Token',
  xsrfToken: null,
};

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  const reqWithToken = CsrfInterceptor.xsrfToken
    ? req.clone({
        headers: req.headers.set(CsrfInterceptor.xsrfTokenHeaderName, CsrfInterceptor.xsrfToken),
      })
    : req;

  return next(reqWithToken).pipe(
    tap({
      next: event => {
        if (event instanceof HttpResponse && event.headers?.has(CsrfInterceptor.xsrfTokenHeaderName)) {
          CsrfInterceptor.xsrfToken = event.headers.get(CsrfInterceptor.xsrfTokenHeaderName);
        }
      },
      error: error => {
        if (error.headers?.has(CsrfInterceptor.xsrfTokenHeaderName)) {
          CsrfInterceptor.xsrfToken = error.headers.get(CsrfInterceptor.xsrfTokenHeaderName);
        }
      },
    }),
  );
};

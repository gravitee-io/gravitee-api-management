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
import { map } from 'rxjs/operators';

/**
 * Add the If-Match header to the request if the response contains an ETag header for specific resources.
 * Only for Put and Post methods.
 */
@Injectable({
  providedIn: 'platform',
})
export class IfMatchEtagInterceptor implements HttpInterceptor {
  private activatedFor: {
    key: 'api';
    urlToMatch: RegExp;
  }[] = [
    {
      key: 'api',
      urlToMatch: new RegExp(`/management/organizations/(.*)/environments/(.*)/apis`),
    },
  ];
  public lastEtag: Record<string, string> = {};

  public updateLastEtag(key: string, etag: string) {
    this.lastEtag[key] = etag;
  }

  public getLastEtag(key: string): string {
    return this.lastEtag[key];
  }

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    req = req.clone();

    if (req.method === 'PUT' || req.method === 'POST') {
      const activated = this.activatedFor.find((a) => req.url.match(a.urlToMatch));

      if (activated) {
        const etag = this.getLastEtag(activated.key);
        if (etag) {
          req = req.clone({
            headers: req.headers.set('If-Match', etag),
          });
        }
      }
    }

    return next.handle(req).pipe(
      map((event) => {
        if (event instanceof HttpResponse) {
          const activated = this.activatedFor.find((a) => event.url.match(a.urlToMatch));
          if (activated) {
            if (event.headers.has('etag')) {
              this.updateLastEtag(activated.key, event.headers.get('etag'));
            }
          }
        }
        return event;
      }),
    );
  }
}

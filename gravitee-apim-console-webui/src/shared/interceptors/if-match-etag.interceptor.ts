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
import { isEmpty } from 'lodash';

/**
 * Add the If-Match header to the request if the response contains an ETag header for specific resources.
 * Only for Put and Post methods.
 */
@Injectable({
  providedIn: 'platform',
})
export class IfMatchEtagInterceptor implements HttpInterceptor {
  public static ETAG_HEADER = 'etag';
  public static ETAG_HEADER_IF_MATCH = 'If-Match';

  private urlToKeyMatchers: ((url: string) => string | undefined)[] = [
    (url) => {
      const matchArray = url.match(new RegExp(`/management/organizations/(.*)/environments/(.*)/apis/(.*)`));
      if (matchArray) {
        return `api-${matchArray[3]}`;
      }
    },
    (url) => {
      const matchArray = url.match(new RegExp(`/management/v2/environments/(.*)/apis/(.*)`));
      if (matchArray) {
        return `api-${matchArray[2]}`;
      }
    },
  ];
  public lastEtag: Record<string, string> = {};

  interceptRequest(method: string, url: string, setIfMatchHeaderFn: (etagValue: string) => void): void {
    if (method === 'PUT' || method === 'POST') {
      const matchingKey = this.getMatchingKey(url);
      if (matchingKey) {
        const etag = this.lastEtag[matchingKey];
        if (etag) {
          setIfMatchHeaderFn(etag);
        }
      }
    }
  }

  interceptResponse(url: string, etagHeaderValue: string): void {
    const matchingKey = this.getMatchingKey(url);
    if (matchingKey) {
      if (!isEmpty(etagHeaderValue)) {
        this.lastEtag[matchingKey] = etagHeaderValue;
      }
    }
  }

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    req = req.clone();

    this.interceptRequest(req.method, req.url, (etagValue) => {
      req = req.clone({
        headers: req.headers.set(IfMatchEtagInterceptor.ETAG_HEADER_IF_MATCH, etagValue),
      });
    });

    return next.handle(req).pipe(
      map((event) => {
        if (event instanceof HttpResponse) {
          this.interceptResponse(event.url, event.headers.get(IfMatchEtagInterceptor.ETAG_HEADER));
        }
        return event;
      }),
    );
  }

  private getMatchingKey(url: string): string | null {
    for (const matcher of this.urlToKeyMatchers) {
      const key = matcher(url);
      if (key) {
        return key;
      }
    }
    return null;
  }
}

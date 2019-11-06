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
import {Router} from '@angular/router';

import {tap} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CurrentUserService } from '../services/current-user.service';

@Injectable()
export class APIRequestInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private currentUserService: CurrentUserService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    request = request.clone({
      setHeaders: {
        'X-Requested-With': 'XMLHttpRequest' // avoid browser to prompt for credentials if 401
      }
    });

    return next.handle(request).pipe(tap(
      () => {},
      (err: any) => {
        if (err instanceof HttpErrorResponse) {
          if (err.status !== 401) {
            return;
          }

          this.currentUserService.revokeUser();
          this.router.navigate(['login']);
        }
      }
    ));
  }
}

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
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { Constants } from '../../entities/Constants';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  constructor(private readonly snackBarService: SnackBarService, @Inject('Constants') private readonly constants: Constants) {}
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (this.constants.org?.baseURL && req.url.startsWith(`${this.constants.org.baseURL}/user`)) {
      return next.handle(req);
    }

    return next.handle(req).pipe(
      catchError((error) => {
        if (error) {
          switch (error.status) {
            case 400:
              this.snackBarService.error(error?.error?.message ?? 'Bad request!');
              break;
            case 401:
              this.snackBarService.error(error?.error?.message ?? 'Unauthorized, please login again!');
              // TODO: Impl silent login refresh
              break;
            case 403:
              this.snackBarService.error(error?.error?.message ?? 'Forbidden!');
              break;
            case 404:
              this.snackBarService.error(error?.error?.message ?? 'Backend service not found!');
              break;
            case 500:
              this.snackBarService.error(error?.error?.message ?? 'Internal server error!');
              break;
            default:
              this.snackBarService.error(error?.error?.message ?? 'Something went wrong!');
              break;
          }
        }
        return throwError(error);
      }),
    );
  }
}

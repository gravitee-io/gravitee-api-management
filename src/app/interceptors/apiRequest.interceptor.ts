import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Router} from '@angular/router';

import {tap} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CurrentUserService } from '../services/currentUser.service';

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

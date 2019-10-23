import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Router} from '@angular/router';

import {tap} from 'rxjs/operators';
import { CurrentUserService } from './currentUser.service';

export class UnauthorizedInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private currentUserService: CurrentUserService
  ) {}
  
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    
    return next.handle(request).pipe( tap(() => {},
      (err: any) => {
      if (err instanceof HttpErrorResponse) {
        if (err.status !== 401) {
         return;
        }

        this.currentUserService.revokeUser();
        this.router.navigate(['/login']);
      }
    }));
  }
}
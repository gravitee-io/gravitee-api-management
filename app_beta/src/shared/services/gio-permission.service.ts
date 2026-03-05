import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { GioPermissionService } from '@gravitee/gravitee-portal';

@Injectable({ providedIn: 'root' })
export class AppBetaGioPermissionService extends GioPermissionService {
  hasAnyMatching(_permissions: string[]): boolean {
    return true;
  }

  hasAllMatching(_permissions: string[]): boolean {
    return true;
  }

  getPermissionsByRoleScope(_role: 'API' | 'APPLICATION' | 'CLUSTER', _id: string): Observable<string[]> {
    return of([]);
  }
}

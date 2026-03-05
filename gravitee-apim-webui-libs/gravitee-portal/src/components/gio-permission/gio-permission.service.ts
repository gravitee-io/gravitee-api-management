import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable()
export abstract class GioPermissionService {
  abstract hasAnyMatching(permissions: string[]): boolean;
  abstract hasAllMatching(permissions: string[]): boolean;
  abstract getPermissionsByRoleScope(role: 'API' | 'APPLICATION' | 'CLUSTER', id: string): Observable<string[]>;
}

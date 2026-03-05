import { DestroyRef, Directive, inject, Input, TemplateRef, ViewContainerRef } from '@angular/core';
import { Observable, of, Subject } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { intersection, isEqual } from 'lodash';

import { GioPermissionService } from './gio-permission.service';

export interface GioPermissionCheckOptions {
  anyOf?: string[];
  noneOf?: string[];
  allOf?: string[];
}

export type GioPermissionRoleContext = { role: 'API' | 'APPLICATION' | 'CLUSTER'; id: string };

@Directive({
  selector: '[gioPermission]',
  standalone: false,
})
export class GioPermissionDirective {
  private permissionCheckOptions: GioPermissionCheckOptions | null = null;
  private elseTemplateRef: TemplateRef<any> | null = null;
  private roleContext: GioPermissionRoleContext | null = null;

  private readonly changes$ = new Subject<void>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissionService = inject(GioPermissionService);
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);

  @Input()
  set gioPermission(permissionCheckOptions: GioPermissionCheckOptions) {
    if (this.permissionCheckOptions && isEqual(this.permissionCheckOptions, permissionCheckOptions)) {
      return;
    }
    this.permissionCheckOptions = permissionCheckOptions;

    if (
      [this.permissionCheckOptions.anyOf, this.permissionCheckOptions.noneOf, this.permissionCheckOptions.allOf].filter(val => !!val)
        .length > 1
    ) {
      throw new Error('You should only set one of `anyOf`, `noneOf`, or `allOf`, but not more than one.');
    }

    this.changes$.next();
  }

  @Input()
  set gioPermissionElse(templateRef: TemplateRef<any> | null) {
    if (this.elseTemplateRef && this.elseTemplateRef === templateRef) {
      return;
    }
    this.elseTemplateRef = templateRef;
    this.changes$.next();
  }

  @Input()
  set gioPermissionRoleContext(roleContext: GioPermissionRoleContext) {
    if (this.roleContext && isEqual(this.roleContext, roleContext)) {
      return;
    }
    this.roleContext = roleContext;
    this.changes$.next();
  }

  constructor() {
    this.changes$
      .pipe(
        switchMap(() => this.resolveRoleContextIfNeeded()),
        map(roleContextPermissions => this.shouldRender(roleContextPermissions)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: shouldRender => {
          if (shouldRender) {
            this.renderMain();
          } else {
            this.renderElse();
          }
        },
        error: () => this.renderElse(),
      });
  }

  private resolveRoleContextIfNeeded(): Observable<string[]> {
    if (!this.roleContext) {
      return of([]);
    }

    const { role, id } = this.roleContext;
    if (!id) {
      return of([]);
    }

    return this.permissionService.getPermissionsByRoleScope(role, id);
  }

  private shouldRender(roleContextPermissions: string[]): boolean {
    if (!this.permissionCheckOptions) {
      return false;
    }
    const { anyOf, noneOf, allOf } = this.permissionCheckOptions;

    return (
      (anyOf && (this.permissionService.hasAnyMatching(anyOf) || hasAnyMatching(anyOf, roleContextPermissions))) ||
      (noneOf && !this.permissionService.hasAnyMatching(noneOf) && !hasAnyMatching(noneOf, roleContextPermissions)) ||
      (allOf && (this.permissionService.hasAllMatching(allOf) || hasAllMatching(allOf, roleContextPermissions))) ||
      false
    );
  }

  private renderMain(): void {
    this.viewContainer.clear();
    this.viewContainer.createEmbeddedView(this.templateRef);
  }

  private renderElse(): void {
    this.viewContainer.clear();
    if (this.elseTemplateRef) {
      this.viewContainer.createEmbeddedView(this.elseTemplateRef);
    }
  }
}

const hasAnyMatching = (has: string[], permissions: string[]): boolean => {
  if (!has || has.length === 0) {
    return false;
  }
  return intersection(permissions, has).length > 0;
};

const hasAllMatching = (has: string[], permissions: string[]): boolean => {
  if (!has || has.length === 0) {
    return false;
  }
  return has.every(permission => permissions.includes(permission));
};

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
      return;
    }
    const { anyOf, noneOf, allOf } = this.permissionCheckOptions;

    return (
      // Check if global permissions or role context permissions match the criteria
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

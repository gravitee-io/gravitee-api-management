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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { RoleService } from '../../../services-ngx/role.service';
import { MembershipListItem } from '../../../entities/role/membershipListItem';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

@Component({
  selector: 'org-settings-role-members',
  template: require('./org-settings-role-members.component.html'),
  styles: [require('./org-settings-role-members.component.scss')],
})
export class OrgSettingsRoleMembersComponent implements OnInit, OnDestroy {
  membershipsTableDisplayedColumns = ['displayName', 'actions'];

  roleScope: string;
  role: string;
  memberships: MembershipListItem[];
  filteredMemberships: MembershipListItem[];

  private readonly unsubscribe$ = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: { roleScope: string; role: string },
    private readonly roleService: RoleService,
  ) {}

  ngOnInit(): void {
    this.roleScope = this.ajsStateParams.roleScope;
    this.role = this.ajsStateParams.role;

    this.roleService
      .listMemberships(this.roleScope, this.role)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((memberships) => {
          this.memberships = memberships;
          this.filteredMemberships = memberships;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onMembershipFiltersChanged(filters: GioTableWrapperFilters) {
    this.filteredMemberships = gioTableFilterCollection(this.memberships, filters);
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onAddMemberClicked() {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function,@typescript-eslint/no-unused-vars
  onDeleteMemberClicked(member) {}
}

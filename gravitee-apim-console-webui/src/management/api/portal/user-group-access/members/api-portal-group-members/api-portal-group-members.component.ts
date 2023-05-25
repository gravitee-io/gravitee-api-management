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
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { UsersService } from '../../../../../../services-ngx/users.service';
import { GioTableWrapperFilters } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GroupV2Service } from '../../../../../../services-ngx/group-v2.service';

interface MemberDataSource {
  id: string;
  role: string;
  displayName: string;
  picture: string;
}

@Component({
  selector: 'api-portal-group-members',
  template: require('./api-portal-group-members.component.html'),
  styles: [require('./api-portal-group-members.component.scss')],
})
export class ApiPortalGroupMembersComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  dataSourceGroup: {
    groupName: string;
    memberTotalCount: number;
    dataSource: MemberDataSource[];
  };

  displayedColumns = ['picture', 'displayName', 'role'];

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  @Input()
  groupId: string;

  @Output()
  destroy = new EventEmitter<void>();

  constructor(private readonly groupService: GroupV2Service, private readonly userService: UsersService) {}

  ngOnInit(): void {
    if (!this.groupId || this.groupId.length === 0) {
      this.destroy.emit();
      return;
    }
    this.getGroupMembersPage();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  onFiltersChanged($event: GioTableWrapperFilters) {
    // Only refresh data if not all data is shown or requested page size is less than total count
    if (
      this.dataSourceGroup.dataSource.length < this.dataSourceGroup.memberTotalCount ||
      $event.pagination.size <= this.dataSourceGroup.memberTotalCount
    ) {
      this.getGroupMembersPage($event.pagination.index, $event.pagination.size);
    }
  }

  private getGroupMembersPage(page = 1, perPage = 10): void {
    this.groupService
      .getMembers(this.groupId, page, perPage)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((membersResponse) => {
        if (!membersResponse.pagination.totalCount || membersResponse.pagination.totalCount === 0) {
          this.destroy.emit();
          return;
        }
        this.dataSourceGroup = {
          groupName: membersResponse.metadata?.groupName,
          memberTotalCount: membersResponse.pagination.totalCount,
          dataSource: membersResponse.data.map((member) => {
            return {
              id: member.id,
              role: member.roles.find((role) => role.scope === 'API')?.name,
              displayName: member.displayName,
              picture: this.userService.getUserAvatar(member.id),
            };
          }),
        };
      });
  }
}

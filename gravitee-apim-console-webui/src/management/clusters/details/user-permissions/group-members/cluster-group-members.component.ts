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
import { Component, DestroyRef, inject, Input, OnInit } from '@angular/core';
import { isEqual } from 'lodash';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { GioAvatarModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { NgIf } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { GioTableWrapperModule } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { UsersService } from '../../../../../services-ngx/users.service';

export type Group = {
  id: string;
  name: string;
};

class MemberDataSource {
  id: string;
  role: string;
  displayName: string;
  picture: string;
}

@Component({
  selector: 'cluster-group-members',
  templateUrl: './cluster-group-members.component.html',
  styleUrls: ['./cluster-group-members.component.scss'],
  imports: [MatCardModule, GioTableWrapperModule, MatTableModule, GioAvatarModule, GioLoaderModule, NgIf],
})
export class ClusterGroupMembersComponent implements OnInit {
  destroyRef = inject(DestroyRef);
  groupService = inject(GroupV2Service);
  userService = inject(UsersService);

  @Input()
  group: Group;

  dataSourceGroupVM: {
    memberTotalCount: number;
    membersPageResult: MemberDataSource[];
    isLoading: boolean;
    canViewGroupMembers: boolean;
  } = {
    isLoading: true,
    canViewGroupMembers: true,
    memberTotalCount: 0,
    membersPageResult: [],
  };

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  displayedColumns = ['picture', 'displayName', 'role'];

  ngOnInit(): void {
    this.getGroupMembersPage();
  }

  private getGroupMembersPage(page = 1, perPage = 10): void {
    this.dataSourceGroupVM = {
      isLoading: true,
      canViewGroupMembers: true,
      memberTotalCount: 0,
      membersPageResult: [],
    };
    this.groupService
      .getMembers(this.group.id, page, perPage)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: membersResponse => {
          this.dataSourceGroupVM = {
            isLoading: false,
            canViewGroupMembers: true,
            memberTotalCount: membersResponse.pagination.totalCount,
            membersPageResult: membersResponse.data.map(member => ({
              id: member.id,
              role: member.roles.find(role => role.scope === 'CLUSTER')?.name,
              displayName: member.displayName,
              picture: this.userService.getUserAvatar(member.id),
            })),
          };
        },
        error: ({ error }) => {
          if (error.httpStatus === 403) {
            this.dataSourceGroupVM = {
              isLoading: false,
              canViewGroupMembers: false,
              memberTotalCount: 0,
              membersPageResult: [],
            };
          }
        },
      });
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    if (!isEqual(this.filters, filters)) {
      this.filters = filters;
      this.getGroupMembersPage(filters.pagination.index, filters.pagination.size);
    }
  }
}

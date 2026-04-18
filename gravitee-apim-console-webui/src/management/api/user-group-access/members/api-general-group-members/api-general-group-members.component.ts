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
import { Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { GioAvatarModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { UsersService } from '../../../../../services-ngx/users.service';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { GroupData } from '../api-general-members.component';

interface MemberDataSource {
  id: string;
  role: string;
  displayName: string;
  picture: string;
}

@Component({
  selector: 'api-general-group-members',
  templateUrl: './api-general-group-members.component.html',
  styleUrls: ['./api-general-group-members.component.scss'],
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, GioAvatarModule, GioLoaderModule, GioTableWrapperModule],
})
export class ApiGeneralGroupMembersComponent implements OnInit, OnDestroy {
  @Input() groupData: GroupData;
  @Input() roleScope: string = 'API';

  @Output() destroy = new EventEmitter<void>();

  private readonly groupService = inject(GroupV2Service);
  private readonly userService = inject(UsersService);

  dataSourceGroupVM: {
    memberTotalCount: number;
    membersPageResult: MemberDataSource[];
    isLoading: boolean;
    canViewGroupMembers: boolean;
  };

  displayedColumns = ['picture', 'displayName', 'role'];

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  ngOnInit(): void {
    if (!this.groupData.id || this.groupData.id.length === 0) {
      this.destroy.emit();
      return;
    }
    this.getGroupMembersPage();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    if (isEqual(this.filters, filters)) {
      return;
    }
    this.filters = filters;

    this.getGroupMembersPage(filters.pagination.index, filters.pagination.size);
  }

  private getGroupMembersPage(page = 1, perPage = 10): void {
    this.dataSourceGroupVM = {
      isLoading: true,
      canViewGroupMembers: true,
      memberTotalCount: 0,
      membersPageResult: [],
    };
    this.groupService
      .getMembers(this.groupData.id, page, perPage)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: membersResponse => {
          if (!membersResponse.pagination.totalCount || membersResponse.pagination.totalCount === 0) {
            this.destroy.emit();
            return;
          }
          this.dataSourceGroupVM = {
            isLoading: false,
            canViewGroupMembers: true,
            memberTotalCount: membersResponse.pagination.totalCount,
            membersPageResult: membersResponse.data.map(member => ({
              id: member.id,
              role: member.roles.find(role => role.scope === this.roleScope)?.name,
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
}

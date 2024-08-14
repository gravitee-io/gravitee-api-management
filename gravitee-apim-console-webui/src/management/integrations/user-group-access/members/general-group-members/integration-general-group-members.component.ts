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
import { Component, DestroyRef, EventEmitter, inject, Input, OnDestroy, OnInit, Output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Subject } from "rxjs";

import { UsersService } from "../../../../../services-ngx/users.service";
import { GioTableWrapperFilters } from "../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component";
import { GroupV2Service } from "../../../../../services-ngx/group-v2.service";
import { GroupData } from "../integration-general-members.component";

interface MemberDataSource {
  id: string;
  role: string;
  displayName: string;
  picture: string;
}

@Component({
  selector: "integration-general-group-members",
  templateUrl: "./integration-general-group-members.component.html",
  styleUrls: ["./integration-general-group-members.component.scss"]
})
export class IntegrationGeneralGroupMembersComponent implements OnInit, OnDestroy {
  @Input()
  groupData: GroupData;

  @Output()
  destroy = new EventEmitter<void>();

  private destroyRef: DestroyRef = inject(DestroyRef);

  public dataSourceGroup: {
    memberTotalCount?: number;
    membersPageResult?: MemberDataSource[];
  };

  public displayedColumns = ["picture", "displayName", "role"];

  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: ""
  };

  public canViewGroupMembers: boolean;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly groupService: GroupV2Service,
    private readonly userService: UsersService
  ) {
  }

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

  onFiltersChanged($event: GioTableWrapperFilters) {
    // Only refresh data if not all data is shown or requested page size is less than total count
    if (
      this.dataSourceGroup.membersPageResult.length < this.dataSourceGroup.memberTotalCount ||
      $event.pagination.size <= this.dataSourceGroup.memberTotalCount
    ) {
      this.getGroupMembersPage($event.pagination.index, $event.pagination.size);
    }
  }

  private getGroupMembersPage(page = 1, perPage = 10): void {
    this.groupService
      .getMembers(this.groupData.id, page, perPage)
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (membersResponse) => {
          if (!membersResponse.pagination.totalCount || membersResponse.pagination.totalCount === 0) {
            this.destroy.emit();
            return;
          }
          this.canViewGroupMembers = true;
          this.dataSourceGroup = {
            memberTotalCount: membersResponse.pagination.totalCount,
            membersPageResult: membersResponse.data.map((member) => ({
              id: member.id,
              role: member.roles.find((role) => role.scope === "API")?.name,
              displayName: member.displayName,
              picture: this.userService.getUserAvatar(member.id)
            }))
          };
        },
        error: ({ error }) => {
          if (error.httpStatus === 403) {
            this.canViewGroupMembers = false;
          }
        }
      });
  }
}




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
import { Component, Input, OnInit } from '@angular/core';
import { forkJoin, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { UsersService } from '../../../../../../services-ngx/users.service';
import { GroupV2Service } from '../../../../../../services-ngx/group-v2.service';

class MemberDataSource {
  id: string;
  role: string;
  displayName: string;
  picture: string;
}

@Component({
  selector: 'api-portal-groups-members',
  template: require('./api-portal-groups-members.component.html'),
  styles: [require('./api-portal-groups-members.component.scss')],
})
export class ApiPortalGroupsMembersComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  dataSourceGroups: {
    groupName: string;
    dataSource: MemberDataSource[];
  }[];
  displayedColumns = ['picture', 'displayName', 'role'];

  @Input()
  groupIds: string[] = [];

  constructor(private readonly groupService: GroupV2Service, private readonly userService: UsersService) {}

  ngOnInit(): void {
    if (this.groupIds.length === 0) {
      return;
    }

    forkJoin(this.groupIds.map((id) => this.groupService.getMembers(id, 1, 9999))) // Return all members since pagination not in place
      .pipe(
        takeUntil(this.unsubscribe$),
        map((responses) => responses.filter((r) => r.data.length > 0)),
      )
      .subscribe((responses) => {
        this.dataSourceGroups = responses.map((membersResponse) => ({
          groupName: membersResponse.metadata['groupName'] as string,
          dataSource: membersResponse.data.map((member) => {
            return {
              id: member.id,
              role: member.roles.find((role) => role.scope === 'API')?.name,
              displayName: member.displayName,
              picture: this.userService.getUserAvatar(member.id),
            };
          }),
        }));
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }
}

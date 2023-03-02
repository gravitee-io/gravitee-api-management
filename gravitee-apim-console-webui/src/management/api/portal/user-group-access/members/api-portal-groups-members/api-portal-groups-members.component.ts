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
import { Component, Inject, OnInit } from '@angular/core';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../../services-ngx/api.service';
import { GroupService } from '../../../../../../services-ngx/group.service';
import { UsersService } from '../../../../../../services-ngx/users.service';

class MembersDataSource {
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

  private apiId: string;

  dataSourceGroups: {
    id: string;
    groupName: string;
    dataSource: MembersDataSource[];
  }[];
  displayedColumns = ['picture', 'displayName', 'role'];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly groupService: GroupService,
    private readonly apiService: ApiService,
    private readonly userService: UsersService,
  ) {}

  ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;

    combineLatest([this.apiService.getGroupIdsWithMembers(this.apiId), this.groupService.list()])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([groupIdsWithMembers, groups]) => {
          this.dataSourceGroups = Object.entries(groupIdsWithMembers).map(([groupId, members]) => {
            return {
              id: groupId,
              groupName: groups.find((group) => group.id === groupId).name,
              dataSource: members.map((member) => {
                return {
                  id: member.id,
                  role: member.roles['API'],
                  displayName: member.displayName,
                  picture: this.userService.getUserAvatar(member.id),
                };
              }),
            };
          });
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }
}

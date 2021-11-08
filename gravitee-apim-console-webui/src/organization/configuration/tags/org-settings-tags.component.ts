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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { GroupService } from '../../../services-ngx/group.service';
import { TagService } from '../../../services-ngx/tag.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

type TagTableDS = {
  id: string;
  name?: string;
  description?: string;
  restrictedGroupsName?: string[];
}[];

@Component({
  selector: 'org-settings-tags',
  template: require('./org-settings-tags.component.html'),
  styles: [require('./org-settings-tags.component.scss')],
})
export class OrgSettingsTagsComponent implements OnInit, OnDestroy {
  isLoading = true;

  providedConfigurationMessage = 'Configuration provided by the system';

  tagsTableDS: TagTableDS;
  filteredTagsTableDS: TagTableDS;
  tagsTableDisplayedColumns: string[] = ['id', 'name', 'description', 'restrictedGroupsName', 'actions'];

  private unsubscribe$ = new Subject<boolean>();

  constructor(private readonly tagService: TagService, private readonly groupService: GroupService) {}

  ngOnInit(): void {
    combineLatest([this.tagService.list(), this.groupService.listByOrganization()])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([tags, groups]) => {
        this.tagsTableDS = tags.map((tag) => ({
          id: tag.id,
          name: tag.name,
          description: tag.description,
          restrictedGroupsName: (tag.restricted_groups ?? []).map((groupId) => groups.find((g) => g.id === groupId).name),
        }));
        this.filteredTagsTableDS = this.tagsTableDS;

        this.isLoading = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onTagsFiltersChanged(filters: GioTableWrapperFilters) {
    this.filteredTagsTableDS = gioTableFilterCollection(this.tagsTableDS, filters);
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onAddTagClicked() {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onEditTagClicked() {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onDeleteTagClicked() {}
}

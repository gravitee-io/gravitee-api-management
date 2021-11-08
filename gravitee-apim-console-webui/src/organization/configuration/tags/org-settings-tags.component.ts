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
import { FormControl, FormGroup } from '@angular/forms';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { PortalSettings } from '../../../entities/portal/portalSettings';
import { EntrypointService } from '../../../services-ngx/entrypoint.service';
import { GroupService } from '../../../services-ngx/group.service';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { TagService } from '../../../services-ngx/tag.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

type TagTableDS = {
  id: string;
  name?: string;
  description?: string;
  restrictedGroupsName?: string[];
}[];

type EntrypointTableDS = {
  url: string;
  tags: string[];
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

  portalSettings: PortalSettings;
  defaultConfigForm: FormGroup;
  initialDefaultConfigFormValues: unknown;

  entrypointsTableDS: EntrypointTableDS;
  filteredEntrypointsTableDS: EntrypointTableDS;
  entrypointsTableDisplayedColumns: string[] = ['entrypoint', 'tags', 'actions'];

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly tagService: TagService,
    private readonly groupService: GroupService,
    private readonly portalSettingsService: PortalSettingsService,
    private readonly entrypointService: EntrypointService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.tagService.list(),
      this.groupService.listByOrganization(),
      this.portalSettingsService.get(),
      this.entrypointService.list(),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([tags, groups, portalSettings, entrypoints]) => {
        this.tagsTableDS = tags.map((tag) => ({
          id: tag.id,
          name: tag.name,
          description: tag.description,
          restrictedGroupsName: (tag.restricted_groups ?? []).map((groupId) => groups.find((g) => g.id === groupId).name),
        }));
        this.filteredTagsTableDS = this.tagsTableDS;

        this.portalSettings = portalSettings;
        this.defaultConfigForm = new FormGroup({
          entrypoint: new FormControl({
            value: this.portalSettings.portal.entrypoint,
            disabled: this.isReadonlySetting('portal.entrypoint'),
          }),
        });
        this.initialDefaultConfigFormValues = this.defaultConfigForm.getRawValue();

        this.entrypointsTableDS = entrypoints.map((entrypoint) => ({ url: entrypoint.value, tags: entrypoint.tags }));
        this.filteredEntrypointsTableDS = this.entrypointsTableDS;

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

  isReadonlySetting(property: string): boolean {
    return PortalSettingsService.isReadonly(this.portalSettings, property);
  }

  submitForm() {
    const portalSettingsToSave = {
      ...this.portalSettings,
      portal: {
        ...this.portalSettings.portal,
        entrypoint: this.defaultConfigForm.get('entrypoint').value,
      },
    };

    this.portalSettingsService
      .save(portalSettingsToSave)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => {
          this.snackBarService.success('Configuration saved!');
          this.ngOnInit();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe();
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onAddTagClicked() {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onEditTagClicked() {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onDeleteTagClicked() {}

  onEntrypointsFiltersChanged(filters: GioTableWrapperFilters) {
    this.filteredEntrypointsTableDS = gioTableFilterCollection(this.entrypointsTableDS, filters);
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onAddEntrypointClicked() {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onEditEntrypointClicked() {}

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onDeleteEntrypointClicked() {}
}

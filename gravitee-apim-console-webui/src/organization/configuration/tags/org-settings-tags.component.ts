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
import { UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService } from '@gravitee/ui-particles-angular';

import { OrgSettingAddTagDialogComponent, OrgSettingAddTagDialogData } from './org-settings-add-tag-dialog.component';
import { OrgSettingAddMappingDialogComponent, OrgSettingAddMappingDialogData } from './org-settings-add-mapping-dialog.component';

import { Entrypoint } from '../../../entities/entrypoint/entrypoint';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { Tag } from '../../../entities/tag/tag';
import { EntrypointService } from '../../../services-ngx/entrypoint.service';
import { GroupService } from '../../../services-ngx/group.service';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { TagService } from '../../../services-ngx/tag.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-data';

type TagTableDS = {
  id: string;
  name?: string;
  description?: string;
  restrictedGroupsName?: string[];
}[];

type EntrypointTableDS = {
  id: string;
  url: string;
  tags: string[];
  tagsName: string[];
}[];
@Component({
  selector: 'org-settings-tags',
  templateUrl: './org-settings-tags.component.html',
  styleUrls: ['./org-settings-tags.component.scss'],
})
export class OrgSettingsTagsComponent implements OnInit, OnDestroy {
  isLoading = true;

  providedConfigurationMessage = 'Configuration provided by the system';

  tags: Tag[];
  tagsTableDS: TagTableDS;
  filteredTagsTableDS: TagTableDS;
  tagsTableUnpaginatedLength = 0;
  tagsTableDisplayedColumns: string[] = ['id', 'name', 'description', 'restrictedGroupsName', 'actions'];

  portalSettings: PortalSettings;
  defaultConfigForm: UntypedFormGroup;
  initialDefaultConfigFormValues: unknown;

  entrypoints: Entrypoint[];
  entrypointsTableDS: EntrypointTableDS;
  filteredEntrypointsTableDS: EntrypointTableDS;
  entrypointsTableUnpaginatedLength = 0;
  entrypointsTableDisplayedColumns: string[] = ['entrypoint', 'tags', 'actions'];
  shardingTagsLicenseOptions = { feature: ApimFeature.APIM_SHARDING_TAGS };
  hasShardingTagsLock$: Observable<boolean>;

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly tagService: TagService,
    private readonly groupService: GroupService,
    private readonly portalSettingsService: PortalSettingsService,
    private readonly entrypointService: EntrypointService,
    private readonly snackBarService: SnackBarService,
    private readonly matLegacyDialog: MatDialog,
    private readonly matDialog: MatDialog,
    private readonly gioLicenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    this.hasShardingTagsLock$ = this.gioLicenseService.isMissingFeature$(this.shardingTagsLicenseOptions);
    combineLatest([
      this.tagService.list(),
      this.groupService.listByOrganization(),
      this.portalSettingsService.get(),
      this.entrypointService.list(),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([tags, groups, portalSettings, entrypoints]) => {
        this.tags = tags;
        this.tagsTableDS = tags.map((tag) => ({
          id: tag.id,
          name: tag.name,
          description: tag.description,
          restrictedGroupsName: (tag.restricted_groups ?? [])
            .map((groupId) => groups.find((g) => g.id === groupId)?.name)
            .filter((name) => !!name),
        }));
        this.filteredTagsTableDS = this.tagsTableDS;
        this.tagsTableUnpaginatedLength = this.tagsTableDS.length;

        this.portalSettings = portalSettings;

        this.defaultConfigForm = new UntypedFormGroup({
          entrypoint: new UntypedFormControl({
            value: this.portalSettings.portal.entrypoint,
            disabled: this.isReadonlySetting('portal.entrypoint'),
          }),
          tcpPort: new UntypedFormControl(
            {
              value: this.portalSettings.portal.tcpPort,
              disabled: this.isReadonlySetting('portal.tcpPort'),
            },
            [tcpPortValidator],
          ),
        });
        this.initialDefaultConfigFormValues = this.defaultConfigForm.getRawValue();

        this.entrypoints = entrypoints;
        this.entrypointsTableDS = entrypoints.map((entrypoint) => ({
          id: entrypoint.id,
          url: entrypoint.value,
          tags: entrypoint.tags,
          tagsName: (entrypoint.tags ?? []).map((tagId) => tags.find((t) => t.id === tagId)?.name ?? tagId),
        }));
        this.filteredEntrypointsTableDS = this.entrypointsTableDS;
        this.entrypointsTableUnpaginatedLength = this.entrypointsTableDS.length;

        this.isLoading = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onTagsFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.tagsTableDS, filters);
    this.filteredTagsTableDS = filtered.filteredCollection;
    this.tagsTableUnpaginatedLength = filtered.unpaginatedLength;
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
        tcpPort: this.defaultConfigForm.get('tcpPort').value,
      },
    };

    this.portalSettingsService
      .save(portalSettingsToSave)
      .pipe(
        tap(() => {
          this.snackBarService.success('Configuration saved!');
          this.ngOnInit();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onAddTagClicked() {
    this.matLegacyDialog
      .open<OrgSettingAddTagDialogComponent, OrgSettingAddTagDialogData, Tag>(OrgSettingAddTagDialogComponent, {
        width: '450px',
        data: {},
        role: 'dialog',
        id: 'addTagDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((newTag) => this.tagService.create(newTag)),
        tap(() => {
          this.snackBarService.success('Tag successfully created!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onEditTagClicked(tag: TagTableDS[number]) {
    this.matLegacyDialog
      .open<OrgSettingAddTagDialogComponent, OrgSettingAddTagDialogData, Tag>(OrgSettingAddTagDialogComponent, {
        width: '450px',
        data: {
          tag: this.tags.find((t) => t.id === tag.id),
        },
        role: 'dialog',
        id: 'addTagDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((updatedTag) => this.tagService.update(updatedTag)),
        tap(() => {
          this.snackBarService.success('Tag successfully updated!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onDeleteTagClicked(tag: TagTableDS[number]) {
    const entrypointsToUpdate = this.entrypoints.filter((entrypoint) => entrypoint.tags.includes(tag.id));
    const entrypointsToUpdateWithOneTag = entrypointsToUpdate.filter((e) => e.tags.length === 1);
    const entrypointsToUpdateWithManyTags = entrypointsToUpdate.filter((e) => e.tags.length > 1);

    let entrypointsInfoMessage = '';
    if (entrypointsToUpdateWithManyTags.length === 1) {
      entrypointsInfoMessage = `<br>The tag will be removed for the entrypoint <strong>${entrypointsToUpdateWithManyTags[0].value}</strong>.`;
    } else if (entrypointsToUpdateWithManyTags.length > 1) {
      entrypointsInfoMessage = `
        <br>The tag will be removed from all these entrypoints:
        <ul>
          <li><strong>${entrypointsToUpdateWithManyTags.map((e) => e.value).join('</strong></li><li><strong>')}</strong></li>
        </ul>`;
    }

    if (entrypointsToUpdateWithOneTag.length === 1) {
      entrypointsInfoMessage += `<br>The <strong>${entrypointsToUpdateWithOneTag[0].value}</strong> entrypoint will be deleted as it is only using this tag.`;
    } else if (entrypointsToUpdateWithOneTag.length > 1) {
      entrypointsInfoMessage += `
        <br>The following entrypoints will be deleted as they are only using this tag:
        <ul>
          <li><strong>${entrypointsToUpdateWithOneTag.map((e) => e.value).join('</strong></li><li><strong>')}</strong></li>
        </ul>`;
    }

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete a tag',
          content: `Are you sure you want to delete the tag <strong>${tag.name}</strong>?
          ${entrypointsInfoMessage}
          `,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'removeTagConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        // Remove tag in each entrypoints and update them all
        switchMap(() => {
          if (entrypointsToUpdate.length === 0) {
            // Need to emit `undefined` to have a consistent return type and be sure next switchMap will be executed
            return of<void[]>(undefined);
          }
          const entrypointsUpdated = entrypointsToUpdateWithManyTags.map((entrypoint) => ({
            ...entrypoint,
            tags: entrypoint.tags.filter((t) => t !== tag.id),
          }));
          const entrypointsToDelete = entrypointsToUpdateWithOneTag;

          return combineLatest([
            ...entrypointsUpdated.map((entrypoint) => this.entrypointService.update(entrypoint)),
            ...entrypointsToDelete.map((entrypoint) => this.entrypointService.delete(entrypoint.id)),
          ]);
        }),
        switchMap(() => this.tagService.delete(tag.id)),
        tap(() => this.snackBarService.success(`Tag "${tag.name}" has been deleted.`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onEntrypointsFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.entrypointsTableDS, filters);
    this.filteredEntrypointsTableDS = filtered.filteredCollection;
    this.entrypointsTableUnpaginatedLength = filtered.unpaginatedLength;
  }

  onAddEntrypointClicked() {
    this.matLegacyDialog
      .open<OrgSettingAddMappingDialogComponent, OrgSettingAddMappingDialogData, Entrypoint>(OrgSettingAddMappingDialogComponent, {
        width: '450px',
        data: {},
        role: 'dialog',
        id: 'addMappingDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((newEntrypoint) => this.entrypointService.create(newEntrypoint)),
        tap(() => {
          this.snackBarService.success('Mapping successfully created!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onEditEntrypointClicked(entrypoint: EntrypointTableDS[number]) {
    this.matLegacyDialog
      .open<OrgSettingAddMappingDialogComponent, OrgSettingAddMappingDialogData, Entrypoint>(OrgSettingAddMappingDialogComponent, {
        width: '450px',
        data: {
          entrypoint: this.entrypoints.find((e) => e.id === entrypoint.id),
        },
        role: 'dialog',
        id: 'editMappingDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((newEntrypoint) => this.entrypointService.update(newEntrypoint)),
        tap(() => {
          this.snackBarService.success('Mapping successfully updated!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onDeleteEntrypointClicked(entrypoint: EntrypointTableDS[number]) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete a entrypoint',
          content: `Are you sure you want to delete the entrypoint <strong>${entrypoint.url}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteEntrypointConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.entrypointService.delete(entrypoint.id)),
        tap(() => this.snackBarService.success(`Entrypoint "${entrypoint.url}" has been delete`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
const tcpPortValidator: ValidatorFn = (control: UntypedFormControl): ValidationErrors | null => {
  const tcpPort = control.value;
  return tcpPort < 1025 || tcpPort > 65535 ? { invalidTcpPort: true } : null;
};

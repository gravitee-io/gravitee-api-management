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
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { catchError, filter, finalize, map, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
  GIO_DIALOG_WIDTH,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormSlideToggleModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { BehaviorSubject, EMPTY, Observable, of, tap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Group, GroupEventRule } from '../../../entities/group/group';
import { GroupService } from '../../../services-ngx/group.service';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

export interface GroupsResponse {
  id: string;
  name?: string;
  manageable?: boolean;
  roles?: Record<string, string>;
  event_rules?: GroupEventRule[];
  created_at?: number;
  updated_at?: number;
  max_invitation?: number;
  lock_api_role?: boolean;
  lock_application_role?: boolean;
  system_invitation?: boolean;
  email_invitation?: boolean;
  disable_membership_notifications?: boolean;
  apiPrimaryOwner?: boolean;
  shouldAddToNewAPIs?: boolean;
  shouldAddToNewApplications?: boolean;
}

@Component({
  templateUrl: './groups.component.html',
  selector: 'app-groups',
  styleUrls: ['./groups.component.scss'],
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    RouterModule,
    FormsModule,
    GioFormSlideToggleModule,
    MatSlideToggleModule,
    ReactiveFormsModule,
    GioGoBackButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatOptionModule,
    MatCheckboxModule,
    GioSaveBarModule,
    GioPermissionModule,
    MatTabsModule,
    MatMenuModule,
    GioTableWrapperModule,
  ],
})
export class GroupsComponent implements OnInit {
  groups$: Observable<Group[]> = of([]);
  columnDefs: string[] = ['name', 'actions'];
  settingsForm: FormGroup<{ userGroupRequired: FormControl<boolean> }>;
  initialSettings: unknown;
  defaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 10,
    },
  };
  filteredData: GroupsResponse[] = [];
  noOfRecords: number = 0;
  isLoading: boolean = false;
  protectedGroups = new Set<string>();
  addGroupDisabled = false;

  private groups = new BehaviorSubject<Group[]>([]);
  private settings: ConsoleSettings = {};
  private destroyRef: DestroyRef = inject(DestroyRef);

  constructor(
    private groupService: GroupService,
    private matDialog: MatDialog,
    private snackBarService: SnackBarService,
    private permissionService: GioPermissionService,
    private consoleSettingsService: ConsoleSettingsService,
  ) {}

  ngOnInit() {
    this.resetFilters();
    this.initializeSettingsForm();
    this.loadGroups();
  }

  private loadGroups() {
    if (this.isLoading) return;

    this.isLoading = true;

    const searchTerm = this.defaultFilters.searchTerm ?? '';
    this.groupService
      .listPaginated(this.defaultFilters.pagination.index, this.defaultFilters.pagination.size, searchTerm)
      .pipe(
        filter(Boolean),
        map(groupsPage => {
          this.noOfRecords = groupsPage.page.total_elements;
          this.filteredData = groupsPage.data;
          return groupsPage.data.map(group => ({
            ...group,
            shouldAddToNewAPIs: this.checkEventRule(group, 'API_CREATE'),
            shouldAddToNewApplications: this.checkEventRule(group, 'APPLICATION_CREATE'),
          }));
        }),
        map(groups => {
          this.groups.next(groups);
          this.filterData(this.defaultFilters);
          this.disableCreateGroup();
          this.disableDeleteGroup();
        }),
        finalize(() => {
          this.isLoading = false;
        }),
        catchError(() => {
          this.snackBarService.error('Error occurred while loading groups.');
          this.isLoading = false;
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private checkEventRule(group: Group, event: string) {
    return group.event_rules ? group.event_rules.some(rule => rule.event === event) : false;
  }

  private initializeSettingsForm() {
    const canReadSettings = this.permissionService.hasAnyMatching(['environment-settings-r']);

    if (canReadSettings) {
      this.consoleSettingsService
        .get()
        .pipe(
          tap(response => {
            this.settings = response;
            this.initializeFormValues();
          }),
          catchError(() => {
            this.snackBarService.error(`Error occurred while fetching console settings.`);
            return EMPTY;
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    }
  }

  private initializeFormValues() {
    this.settingsForm = new FormGroup({
      userGroupRequired: new FormControl<boolean>(this.settings.userGroup.required.enabled),
    });
    this.initialSettings = this.settingsForm.getRawValue();
  }

  deleteGroup(group: Group) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete Group',
          content: `Are you sure, you want to delete the group?`,
          confirmButton: 'Yes',
          cancelButton: 'No',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed),
        switchMap(_ => this.groupService.delete(group.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: _ => {
          this.snackBarService.success(`Successfully deleted the group.`);
          this.loadGroups();
        },
        error: () => this.snackBarService.error(`Error while deleting the group.`),
      });
  }

  saveSettings() {
    this.settings.userGroup.required.enabled = this.settingsForm.controls.userGroupRequired.value;

    this.consoleSettingsService
      .save(this.settings)
      .pipe(
        tap(response => {
          this.settings = response;
          this.initializeFormValues();
          this.snackBarService.success('Successfully updated groups settings.');
        }),
        catchError(() => {
          this.snackBarService.error('Error occurred while saving groups settings.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  filterData(filters: GioTableWrapperFilters) {
    this.defaultFilters = { ...this.defaultFilters, ...filters };
    this.loadGroups();
  }

  resetFilters() {
    this.groups.next([]);
    this.filteredData = [];
  }

  private disableDeleteGroup() {
    if (!this.permissionService.hasAnyMatching(['environment-group-d'])) {
      this.protectedGroups = new Set(this.groups.value.map(group => group.id));
    } else {
      this.protectedGroups = new Set(this.groups.value.filter(group => group.apiPrimaryOwner).map(group => group.id));
    }
  }

  private disableCreateGroup() {
    if (!this.permissionService.hasAnyMatching(['environment-group-c'])) {
      this.addGroupDisabled = true;
    }
  }
}

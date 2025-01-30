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
import { Component, OnInit } from '@angular/core';
import { filter, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
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
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
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
  standalone: true,
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
  columnDefs: string[] = ['name', 'actions'];
  settingsForm: FormGroup<{ enabled: FormControl<boolean> }>;
  initialSettings: unknown;
  defaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 10,
    },
  };
  filteredData: GroupsResponse[];
  noOfRecords: number = 0;
  isLoading: boolean = false;

  private groups$: Group[];
  private settings: ConsoleSettings;

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
    this.initializeGroups();
    this.hideActionsForReadOnlyUser();
  }

  private initializeGroups() {
    this.isLoading = true;
    this.groupService.list().subscribe({
      next: (response) => {
        this.groups$ = this.filteredData = response
          .filter((group) => group.manageable)
          .map((group) => {
            return {
              ...group,
              shouldAddToNewAPIs: this.checkEventRule(group, 'API_CREATE'),
              shouldAddToNewApplications: this.checkEventRule(group, 'APPLICATION_CREATE'),
            };
          })
          .sort((a, b) => a.name.localeCompare(b.name));
        this.filterData(this.defaultFilters);
        this.isLoading = false;
      },
      error: () => {
        this.snackBarService.error(`Error while loading groups.`);
      },
    });
  }

  private checkEventRule(group: Group, event: string) {
    return group.event_rules ? group.event_rules.some((rule) => rule.event === event) : false;
  }

  private initializeSettingsForm() {
    this.consoleSettingsService.get().subscribe({
      next: (response) => {
        this.settings = response;
        this.initializeFormValues();
      },
      error: () => {
        this.snackBarService.error(`Error while fetching console settings`);
      },
    });
  }

  private initializeFormValues() {
    this.settingsForm = new FormGroup<{ enabled: FormControl<boolean> }>({
      enabled: new FormControl(this.settings.userGroups.required),
    });
    this.initialSettings = this.settingsForm.getRawValue();
  }

  private hideActionsForReadOnlyUser() {
    if (!this.permissionService.hasAnyMatching(['environment-group-u', 'environment-group-d'])) {
      this.columnDefs.pop();
    }
  }

  deleteGroup(group: Group) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete group',
          content: `Are you sure, you want to delete the group ${group.name}?`,
          confirmButton: 'Yes',
          cancelButton: 'No',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap((_) => this.groupService.delete(group.id)),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success(`Successfully deleted group ${group.name}`);
          this.initializeGroups();
        },
        error: () => this.snackBarService.error(`Error while deleting group ${group.name}`),
      });
  }

  saveSettings() {
    this.settings.userGroups.required = this.settingsForm.controls['enabled'].value;

    this.consoleSettingsService.save(this.settings).subscribe({
      next: (response) => {
        this.settings = response;
        this.initializeFormValues();
        this.snackBarService.success('Successfully updated groups settings');
      },
      error: () => {
        this.snackBarService.error('Error while saving groups settings');
      },
    });
  }

  filterData(filters: GioTableWrapperFilters) {
    this.defaultFilters = { ...this.defaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groups$, filters);
    this.filteredData = filtered.filteredCollection;
    this.noOfRecords = filtered.unpaginatedLength;
  }

  resetFilters() {
    this.groups$ = [];
    this.filteredData = [];
  }
}

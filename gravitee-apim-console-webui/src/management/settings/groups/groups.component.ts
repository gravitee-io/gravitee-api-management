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
import { BehaviorSubject, Observable, of } from 'rxjs';
import { FormControl, FormGroup } from '@angular/forms';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { catchError, filter, map, switchMap } from 'rxjs/operators';
import { GroupService } from '../../../services-ngx/group.service';
import { Group } from '../../../entities/group/group';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  templateUrl: './groups.component.html',
  selector: 'app-groups',
  styleUrls: ['./groups.component.scss'],
})
export class GroupsComponent implements OnInit {
  dataSource: Observable<Group[]> = of([]);
  columnDefs: string[] = ['name', 'associatedToNewApi', 'associatedToNewApplication', 'actions'];
  settingsForm: FormGroup<{ enabled: FormControl<boolean> }>;
  initialSettings: unknown;

  private refreshGroups = new BehaviorSubject(1);
  private settings: ConsoleSettings;
  private destroyRef = inject(DestroyRef);

  constructor(
    private groupService: GroupService,
    private matDialog: MatDialog,
    private snackBarService: SnackBarService,
    private permissionService: GioPermissionService,
    private consoleSettingsService: ConsoleSettingsService,
  ) {}

  ngOnInit() {
    this.hideActionsForReadOnlyUser();
    this.initializeSettingsForm();
    this.initializeGroups();
  }

  private initializeGroups() {
    this.dataSource = this.refreshGroups.pipe(
      switchMap((_) => this.groupService.list()),
      map((groups) => groups.sort((a, b) => a.name.localeCompare(b.name))),
      map((groups) =>
        groups.map((group) => ({
          ...group,
          shouldAddToNewAPIs: this.checkEventRule(group, 'API_CREATE'),
          shouldAddToNewApplications: this.checkEventRule(group, 'APPLICATION_CREATE'),
        })),
      ),
      catchError(() => of([])),
    );
  }

  private checkEventRule(group: Group, event: string) {
    return !!group.event_rules ? group.event_rules.some((rule) => rule.event === event) : false;
  }

  private initializeSettingsForm() {
    this.consoleSettingsService.get().subscribe({
      next: (settings) => {
        this.settings = settings;
        this.settingsForm = new FormGroup<{ enabled: FormControl<boolean> }>({
          enabled: new FormControl(this.settings.userGroups.required),
        });
        this.initialSettings = this.settingsForm.getRawValue();
      },
      error: () => {
        this.snackBarService.error(`Error while fetching console settings`);
      },
    });
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
          content: `Are you sure you want to delete the group ${group.name}?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap((_) => this.groupService.delete(group.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success(`Successfully deleted group ${group.name}`);
          this.refreshGroups.next(1);
        },
        error: () => this.snackBarService.error(`Error while deleting group ${group.name}`),
      });
  }

  saveSettings() {
    this.settings.userGroups.required = this.settingsForm.controls['enabled'].value;

    this.consoleSettingsService
      .save(this.settings)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.initializeSettingsForm();
          this.snackBarService.success('Successfully updated groups settings');
        },
        error: () => {
          this.snackBarService.error('Error while saving groups settings');
        },
      });
  }
}

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
import { Role } from 'src/entities/role/role';

import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BehaviorSubject, Observable, of, switchMap, tap } from 'rxjs';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { isEmpty } from 'lodash';

import { Invitation } from '../../../../entities/invitation/invitation';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormSlideToggleModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GioGoBackButtonModule } from '../../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { DeleteMemberDialogComponent } from './delete-member-dialog/delete-member-dialog.component';
import { EditMemberDialogComponent } from './edit-member-dialog/edit-member-dialog.component';
import { AddMembersDialogComponent } from './add-members-dialog/add-members-dialog.component';
import { InviteMemberDialogComponent } from './invite-member-dialog/invite-member-dialog.component';
import { GroupService } from '../../../../services-ngx/group.service';
import { Member } from '../../../../entities/management-api-v2';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { Group } from '../../../../entities/group/group';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

interface EditMemberDialogData {
  group: Group;
  member: Member;
  members: Member[];
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
}

interface DeleteMemberDialogData {
  group: Group;
  member: Member;
  members: Member[];
}

interface AddOrInviteMembersDialogData {
  group: Group;
  members: Member[];
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles?: Role[];
}

@Component({
  selector: 'app-group',
  templateUrl: './group.component.html',
  styleUrls: ['./group.component.scss'],
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
export class GroupComponent implements OnInit {
  group$: Observable<Group>;
  groupMembers$: Observable<Member[]>;
  invitations$: Observable<Invitation[]>;
  groupAPIs$: Observable<any>;
  groupApplications$: Observable<[]>;
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
  groupForm: FormGroup<{
    name: FormControl<string>;
    defaultAPIRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    maxNumberOfMembers: FormControl<number>;
    shouldAllowInvitationViaSearch: FormControl<boolean>;
    shouldAllowInvitationViaEmail: FormControl<boolean>;
    canAdminChangeAPIRole: FormControl<boolean>;
    canAdminChangeApplicationRole: FormControl<boolean>;
    shouldNotifyWhenMemberAdded: FormControl<boolean>;
    shouldAddToNewAPIs: FormControl<boolean>;
    shouldAddToNewApplications: FormControl<boolean>;
  }>;
  mode: 'new' | 'edit' = 'new';
  memberColumnDefs: string[] = ['name', 'defaultApiRole', 'defaultApplicationRole', 'defaultIntegrationRole', 'actions'];
  invitationColumnDefs: string[] = ['guestEmail', 'guestApiRole', 'guestApplicationRole', 'guestInvitedOn', 'guestActions'];
  groupAPIColumnDefs: string[] = ['apiName', 'apiVersion'];
  groupApplicationsColumnDefs: string[] = ['applicationName'];
  groupId: string = undefined;
  noOfMembers: number;
  initialGroupForm: unknown;
  membersDefaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 10,
    },
  };
  invitationsDefaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 10,
    },
  };
  apisDefaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 5,
    },
  };
  applicationsDefaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 5,
    },
  };
  noOfGroupMembers: number;
  filteredGroupMembers: Member[];
  noOfGroupInvitations: number;
  filteredGroupInvitations: Invitation[];
  noOfGroupAPIs: number;
  filteredGroupAPIs: any[];
  noOfGroupApplications: number;
  filteredGroupApplications: any[];

  private group = new BehaviorSubject<Group>(null);
  private refreshGroup = new BehaviorSubject(1);
  private groupMembers = new BehaviorSubject([]);
  private refreshGroupMembers = new BehaviorSubject(1);
  private groupInvitations = new BehaviorSubject([]);
  private refreshInvitations = new BehaviorSubject(1);
  private groupAPIs = new BehaviorSubject([]);
  private refreshGroupAPIs = new BehaviorSubject(1);
  private groupApplications = new BehaviorSubject([]);
  private refreshGroupApplications = new BehaviorSubject(1);
  private destroyRef = inject(DestroyRef);

  constructor(
    private groupService: GroupService,
    private route: ActivatedRoute,
    private roleService: RoleService,
    private snackBarService: SnackBarService,
    private router: Router,
    private permissionService: GioPermissionService,
    private matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.initializeData();
  }

  private initializeData() {
    this.group$ = this.refreshGroup.pipe(
      switchMap((_) => {
        this.groupId = this.route.snapshot.paramMap.get('groupId');

        if (!!this.groupId && this.groupId !== 'new') {
          this.mode = 'edit';
          return this.groupService.get(this.groupId);
        }
        return of({} as Group);
      }),
      tap((group: Group) => {
        this.group.next(group);
        this.groupForm = this.initializeForm(group);
        this.initialGroupForm = this.groupForm.getRawValue();
        this.hideActionsForReadOnlyUser();
        this.initializeDependents();
      }),
    );
  }

  private initializeDependents() {
    this.initializeGroupMembers();
    this.initializeGroupAPIs();
    this.initializeGroupApplications();
    this.initializeDefaultRoles();
  }

  private initializeForm(group: Group) {
    return new FormGroup({
      name: new FormControl<string>(group.name, { validators: Validators.required }),
      defaultAPIRole: new FormControl<string>(!isEmpty(group.roles) ? group.roles['API'] : undefined),
      defaultApplicationRole: new FormControl<string>(!isEmpty(group.roles) ? group.roles['APPLICATION'] : undefined),
      maxNumberOfMembers: new FormControl<number>(group.max_invitation),
      shouldAllowInvitationViaSearch: new FormControl<boolean>(group.system_invitation),
      shouldAllowInvitationViaEmail: new FormControl<boolean>(group.email_invitation),
      canAdminChangeAPIRole: new FormControl<boolean>(!group.lock_api_role),
      canAdminChangeApplicationRole: new FormControl<boolean>(!group.lock_application_role),
      shouldNotifyWhenMemberAdded: new FormControl<boolean>(!group.disable_membership_notifications),
      shouldAddToNewAPIs: new FormControl<boolean>(group.event_rules ? this.checkEventRule(group, 'API_CREATE') : false),
      shouldAddToNewApplications: new FormControl<boolean>(group.event_rules ? this.checkEventRule(group, 'APPLICATION_CREATE') : false),
    });
  }

  canEditMembers(): boolean {
    return this.isSuperAdmin() || (this.group.value.manageable && this.canInviteMember());
  }

  private canInviteMember() {
    return this.group.value.system_invitation || this.group.value.email_invitation;
  }

  isSuperAdmin() {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  private checkEventRule(group: Group, eventType: string) {
    return group.event_rules.some((rule) => rule.event === eventType);
  }

  private hideActionsForReadOnlyUser() {
    if (!this.isSuperAdmin()) {
      this.memberColumnDefs.pop();
    }
  }

  private updateEventRules() {
    const eventRules = [];

    if (this.groupForm.controls['shouldAddToNewAPIs'].value) {
      eventRules.push({ event: 'API_CREATE' });
    }

    if (this.groupForm.controls['shouldAddToNewApplications'].value) {
      eventRules.push({ event: 'APPLICATION_CREATE' });
    }

    this.group.value.event_rules = eventRules;
  }

  private updateRoles() {
    const roles: any = {};

    if (this.groupForm.controls['defaultAPIRole'].value) {
      roles['API'] = this.groupForm.controls['defaultAPIRole'].value;
    } else {
      delete roles['API'];
    }

    if (this.groupForm.controls['defaultApplicationRole'].value) {
      roles['APPLICATION'] = this.groupForm.controls['defaultApplicationRole'].value;
    } else {
      delete roles['APPLICATION'];
    }

    this.group.value.roles = roles;
  }

  private mapUpdatedGroup() {
    const groupFormControls = this.groupForm.controls;
    const group: Group = {
      ...this.group.value,
      name: groupFormControls['name'].value,
      max_invitation: groupFormControls['maxNumberOfMembers'].value,
      lock_api_role: !groupFormControls['canAdminChangeAPIRole'].value,
      lock_application_role: !groupFormControls['canAdminChangeApplicationRole'].value,
      system_invitation: groupFormControls['shouldAllowInvitationViaSearch'].value,
      email_invitation: groupFormControls['shouldAllowInvitationViaEmail'].value,
      disable_membership_notifications: !groupFormControls['shouldNotifyWhenMemberAdded'].value,
    };
    return group;
  }

  private initializeDefaultRoles() {
    this.roleService.list('API').subscribe((roles: Role[]) => {
      this.defaultAPIRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
    });

    this.roleService.list('APPLICATION').subscribe((roles) => {
      this.defaultApplicationRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
    });

    this.roleService.list('INTEGRATION').subscribe((roles) => {
      this.defaultIntegrationRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
    });
  }

  private initializeGroupMembers() {
    if (this.mode === 'edit') {
      this.groupMembers$ = this.refreshGroupMembers.pipe(
        switchMap((_) => this.groupService.getMembers(this.groupId)),
        map((members) => members.sort((a, b) => a.displayName.localeCompare(b.displayName))),
        tap((members) => {
          this.groupMembers.next(members);
          this.filterGroupMembers(this.membersDefaultFilters);
        }),
      );
    }
  }

  saveOrUpdate() {
    this.updateEventRules();
    this.updateRoles();
    this.groupService
      .saveOrUpdate(this.mode, this.mapUpdatedGroup())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (group) => {
          this.snackBarService.success(`Successfully saved group ${group.name}`);

          if (this.mode === 'new') {
            this.router.navigate(['..', group.id], { relativeTo: this.route });
          } else {
            this.refreshGroup.next(1);
          }
        },
        error: () => this.snackBarService.error(`Error occurred while saving group ${this.mapUpdatedGroup().name}`),
      });
  }

  openDeleteMemberDialog(member: Member) {
    this.matDialog
      .open<DeleteMemberDialogComponent, DeleteMemberDialogData>(DeleteMemberDialogComponent, {
        data: {
          group: this.group.value,
          member: member,
          members: this.groupMembers.value,
        },
        role: 'alertdialog',
        id: 'deleteMemberDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: '30%',
      })
      .afterClosed()
      .pipe(tap(() => this.refreshGroupMembers.next(1)))
      .subscribe();
  }

  openEditMemberDialog(member: Member) {
    this.matDialog
      .open<EditMemberDialogComponent, EditMemberDialogData>(EditMemberDialogComponent, {
        data: {
          group: this.group.value,
          member: member,
          members: this.groupMembers.value,
          defaultAPIRoles: this.defaultAPIRoles,
          defaultApplicationRoles: this.defaultApplicationRoles,
          defaultIntegrationRoles: this.defaultIntegrationRoles,
        },
        role: 'alertdialog',
        id: 'editMemberDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: '30%',
      })
      .afterClosed()
      .pipe(tap(() => this.refreshGroupMembers.next(1)))
      .subscribe();
  }

  openSearchMembersDialog() {
    this.matDialog
      .open<AddMembersDialogComponent, AddOrInviteMembersDialogData>(AddMembersDialogComponent, {
        data: {
          group: this.group.value,
          members: this.groupMembers.value,
          defaultAPIRoles: this.defaultAPIRoles,
          defaultApplicationRoles: this.defaultApplicationRoles,
          defaultIntegrationRoles: this.defaultIntegrationRoles,
        },
        role: 'alertdialog',
        id: 'addMembersDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: '30%',
      })
      .afterClosed()
      .pipe(tap(() => this.refreshGroupMembers.next(1)))
      .subscribe();
  }

  openInviteMemberDialog() {
    this.matDialog
      .open<InviteMemberDialogComponent, AddOrInviteMembersDialogData>(InviteMemberDialogComponent, {
        data: {
          group: this.group.value,
          members: this.groupMembers.value,
          defaultAPIRoles: this.defaultAPIRoles,
          defaultApplicationRoles: this.defaultApplicationRoles,
        },
        role: 'alertdialog',
        id: 'inviteMemberDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: '30%',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        tap(() => this.refreshInvitations.next(1)),
      )
      .subscribe();
  }

  private initializeInvitations() {
    this.invitations$ = this.refreshInvitations.pipe(
      switchMap((_) => this.groupService.getInvitations(this.groupId)),
      map((invitations) => {
        return invitations.sort((a, b) => a.email.localeCompare(b.email));
      }),
      tap((invitations) => {
        this.groupInvitations.next(invitations);
        this.filterGroupInvitations(this.invitationsDefaultFilters);
      }),
    );
  }

  deleteInvitation(invitationId: string, email: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete Invitation',
          content: `Are you sure, you want to delete invitation sent to ${email}?`,
          confirmButton: 'Delete',
          cancelButton: 'No',
        },
        role: 'alertdialog',
        id: 'deleteInvitationDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: '30%',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        tap(() => {
          this.groupService.deleteInvitation(this.groupId, invitationId).subscribe({
            next: () => {
              this.snackBarService.success(`Successfully deleted invitation to ${email}.`);
              this.refreshInvitations.next(1);
            },
            error: () => {
              this.snackBarService.error(`Error occurred while deleting invitation to ${email}.`);
            },
          });
        }),
      )
      .subscribe();
  }

  private initializeGroupAPIs() {
    this.groupAPIs$ = this.refreshGroupAPIs.pipe(
      switchMap((_) => this.groupService.getMemberships(this.groupId, 'api')),
      map((apis) => apis.sort((a, b) => a.name.localeCompare(b.name))),
      tap((apis) => {
        this.groupAPIs.next(apis);
        this.filterGroupAPIs(this.apisDefaultFilters);
      }),
    );
  }

  addToExistingAPIs(group: Group) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Add group to existing APIs',
          content: `Are you sure, you want to add the group ${group.name} to all existing APIs?`,
          confirmButton: 'Yes',
          cancelButton: 'No',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((_) => _),
        switchMap((_) => this.groupService.addToExistingComponents(group.id, 'api')),
      )
      .subscribe({
        next: (group: Group) => {
          this.refreshGroupAPIs.next(1);
          this.snackBarService.success(`Successfully added group ${group.name} to existing APIs.`);
        },
        error: () => {
          this.snackBarService.error(`Error occurred while adding group ${group.name} to existing APIs.`);
        },
      });
  }

  private initializeGroupApplications() {
    this.groupApplications$ = this.refreshGroupApplications.pipe(
      switchMap((_) => this.groupService.getMemberships(this.groupId, 'application')),
      map((applications) => applications.sort((a, b) => a.name.localeCompare(b.name))),
      tap((applications) => {
        this.groupApplications.next(applications);
        this.filterGroupApplications(this.applicationsDefaultFilters);
      }),
    );
  }

  addToExistingApplications(group: Group) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Add group to existing applications',
          content: `Are you sure, you want to add the group ${group.name} to all existing applications?`,
          confirmButton: 'Yes',
          cancelButton: 'No',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((_) => _),
        switchMap((_) => this.groupService.addToExistingComponents(group.id, 'application')),
      )
      .subscribe({
        next: (group: Group) => {
          this.refreshGroupApplications.next(1);
          this.snackBarService.success(`Successfully added group ${group.name} to existing applications.`);
        },
        error: () => {
          this.snackBarService.error(`Error occurred while adding group ${group.name} to existing applications.`);
        },
      });
  }

  disableSubmit() {
    return this.groupMembers.value.length === 1 && this.groupMembers.value[0].roles['API'] === 'PRIMARY_OWNER';
  }

  onTabChange(index: number) {
    switch (index) {
      case 1:
        this.initializeInvitations();
        break;
    }
  }

  filterGroupMembers(filters: GioTableWrapperFilters) {
    this.membersDefaultFilters = { ...this.membersDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupMembers.value, filters);
    this.filteredGroupMembers = filtered.filteredCollection;
    this.noOfGroupMembers = filtered.unpaginatedLength;
  }

  filterGroupInvitations(filters: GioTableWrapperFilters) {
    this.invitationsDefaultFilters = { ...this.invitationsDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupInvitations.value, filters);
    this.filteredGroupInvitations = filtered.filteredCollection;
    this.noOfGroupInvitations = filtered.unpaginatedLength;
  }

  filterGroupAPIs(filters: GioTableWrapperFilters) {
    this.apisDefaultFilters = { ...this.apisDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupAPIs.value, filters);
    this.filteredGroupAPIs = filtered.filteredCollection;
    this.noOfGroupAPIs = filtered.unpaginatedLength;
  }

  filterGroupApplications(filters: GioTableWrapperFilters) {
    this.applicationsDefaultFilters = { ...this.applicationsDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupApplications.value, filters);
    this.filteredGroupApplications = filtered.filteredCollection;
    this.noOfGroupApplications = filtered.unpaginatedLength;
  }
}

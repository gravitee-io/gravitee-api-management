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
import { GroupMembership } from 'src/entities/group/groupMember';

import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BehaviorSubject, EMPTY, Observable, of, switchMap, tap } from 'rxjs';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, filter, map } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
  GIO_DIALOG_WIDTH,
  GioBannerModule,
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';

import { DeleteMemberDialogComponent } from './delete-member-dialog/delete-member-dialog.component';
import { EditMemberDialogComponent } from './edit-member-dialog/edit-member-dialog.component';
import { AddMembersDialogComponent } from './add-members-dialog/add-members-dialog.component';
import { InviteMemberDialogComponent } from './invite-member-dialog/invite-member-dialog.component';
import { Member, RoleName } from './membershipState';
import { TooManyUsersDialogComponent } from './too-many-users-dialog/too-many-users-dialog.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Invitation } from '../../../../entities/invitation/invitation';
import { GroupService } from '../../../../services-ngx/group.service';
import { RoleService } from '../../../../services-ngx/role.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { Group } from '../../../../entities/group/group';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { CurrentUserService } from '../../../../services-ngx/current-user.service';

export interface EditMemberDialogData {
  group: Group;
  member: Member;
  members: Member[];
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles: Role[];
  defaultClusterRoles: Role[];
}

export interface DeleteMemberDialogData {
  member: Member;
  members: Member[];
}

export interface AddOrInviteMembersDialogData {
  group: Group;
  members: Member[];
  defaultAPIRoles: Role[];
  defaultApplicationRoles: Role[];
  defaultIntegrationRoles?: Role[];
  defaultClusterRoles?: Role[];
}

interface AddOrUpdateMemberDialogResult {
  memberships: GroupMembership[];
}

interface InviteMemberDialogResult {
  invitation: Invitation;
}

interface DeleteMemberDialogResult {
  primaryOwnerMembership: GroupMembership;
  shouldDelete: boolean;
}

export interface TooManyUsersDialogData {
  email: string;
}

@Component({
  selector: 'app-group',
  templateUrl: './group.component.html',
  styleUrls: ['./group.component.scss'],
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
    GioBannerModule,
  ],
})
export class GroupComponent implements OnInit {
  group$: Observable<Group> = of(null);
  groupMembers$: Observable<Member[]> = of([]);
  invitations$: Observable<Invitation[]> = of([]);
  groupAPIs$: Observable<any> = of([]);
  groupApplications$: Observable<[]> = of([]);
  defaultAPIRoles: Role[] = [];
  defaultApplicationRoles: Role[] = [];
  defaultIntegrationRoles: Role[] = [];
  defaultClusterRoles: Role[] = [];
  groupId: string = undefined;
  initialFormValues: unknown;
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
  memberColumnDefs: string[] = [
    'name',
    'defaultApiRole',
    'defaultApplicationRole',
    'defaultIntegrationRole',
    'defaultClusterRole',
    'actions',
  ];
  invitationColumnDefs: string[] = ['guestEmail', 'guestApiRole', 'guestApplicationRole', 'guestInvitedOn', 'guestActions'];
  groupAPIColumnDefs: string[] = ['apiName', 'apiVersion'];
  groupApplicationsColumnDefs: string[] = ['applicationName'];
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
  noOfFilteredMembers: number = 0;
  filteredMembers: Member[] = [];
  noOfInvitations: number = 0;
  filteredInvitations: Invitation[] = [];
  noOfAPIs: number = 0;
  filteredAPIs: any[] = [];
  noOfApplications: number = 0;
  filteredApplications: any[] = [];
  canAddMembers = false;
  maxInvitationsLimitReached = false;
  deleteDisabled = false;
  disableAddGroupToExistingApplications = false;
  disableAddGroupToExistingAPIs = false;

  private group = new BehaviorSubject<Group>(null);
  private groupMembers = new BehaviorSubject<Member[]>([]);
  private groupInvitations = new BehaviorSubject<Invitation[]>([]);
  private groupAPIs = new BehaviorSubject<any[]>([]);
  private groupApplications = new BehaviorSubject<any[]>([]);
  private destroyRef = inject(DestroyRef);

  constructor(
    private groupService: GroupService,
    private route: ActivatedRoute,
    private roleService: RoleService,
    private snackBarService: SnackBarService,
    private router: Router,
    private permissionService: GioPermissionService,
    private matDialog: MatDialog,
    private currentUserService: CurrentUserService,
  ) {}

  ngOnInit(): void {
    this.initializeDefaultRoles();
    this.initializeGroup();
  }

  private initializeGroup() {
    this.group$ = this.fetchGroup().pipe(
      tap((group: Group) => {
        this.group.next(group);
        this.initializeForm(group);
        this.initialFormValues = this.groupForm.getRawValue();
        this.initializeDependents();
        this.disableForm();
      }),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  onTabChange(index: number) {
    switch (index) {
      case 1:
        this.initializeInvitations();
        break;
    }
  }

  private fetchGroup(): Observable<Group> {
    return this.route.params.pipe(
      map(params => params.groupId),
      tap((groupId: string) => {
        this.groupId = groupId;
        this.mode = groupId && groupId !== 'new' ? 'edit' : 'new';
      }),
      switchMap((groupId: string) => {
        return groupId && groupId !== 'new' ? this.groupService.get(groupId) : of({} as Group);
      }),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  private initializeForm(group: Group) {
    this.groupForm = new FormGroup({
      name: new FormControl<string>(group.name, { validators: Validators.required }),
      defaultAPIRole: new FormControl<string>(group.roles ? group.roles.API : null),
      defaultApplicationRole: new FormControl<string>(group.roles ? group.roles.APPLICATION : null),
      maxNumberOfMembers: new FormControl<number>(group.max_invitation),
      shouldAllowInvitationViaSearch: new FormControl<boolean>(group.system_invitation ? group.system_invitation : false),
      shouldAllowInvitationViaEmail: new FormControl<boolean>(group.email_invitation ? group.email_invitation : false),
      canAdminChangeAPIRole: new FormControl<boolean>(!group.lock_api_role),
      canAdminChangeApplicationRole: new FormControl<boolean>(!group.lock_application_role),
      shouldNotifyWhenMemberAdded: new FormControl<boolean>(!group.disable_membership_notifications),
      shouldAddToNewAPIs: new FormControl<boolean>(group.event_rules ? this.checkEventRule(group, 'API_CREATE') : false),
      shouldAddToNewApplications: new FormControl<boolean>(group.event_rules ? this.checkEventRule(group, 'APPLICATION_CREATE') : false),
    });
  }

  private initializeDependents() {
    this.initializeGroupMembers();
    this.initializeGroupAPIs();
    this.initializeGroupApplications();
  }

  private initializeGroupMembers() {
    this.groupMembers$ = this.groupService.getMembers(this.groupId).pipe(
      tap(members => {
        this.groupMembers.next(members.sort((a, b) => a.displayName.localeCompare(b.displayName)));
        this.maxInvitationsLimitReached = this.group.value.max_invitation <= this.groupMembers.value.length;
        this.filterGroupMembers(this.membersDefaultFilters);
        this.shouldAllowAddMembers();
        this.hideActionsForReadOnlyUser();
        this.disableDeleteMember();
      }),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  private initializeInvitations() {
    this.invitations$ = this.groupService.getInvitations(this.groupId).pipe(
      map((invitations: Invitation[]) => invitations.sort((a, b) => a.email.localeCompare(b.email))),
      tap(invitations => {
        this.groupInvitations.next(invitations);
        this.filterGroupInvitations(this.invitationsDefaultFilters);
      }),
    );
  }

  private initializeGroupAPIs() {
    this.groupAPIs$ = this.groupService.getMemberships(this.groupId, 'api').pipe(
      map(apis => apis.sort((a, b) => a.name.localeCompare(b.name))),
      tap(apis => {
        this.groupAPIs.next(apis);
        this.filterGroupAPIs(this.apisDefaultFilters);
      }),
    );
  }

  private initializeGroupApplications() {
    this.groupApplications$ = this.groupService.getMemberships(this.groupId, 'application').pipe(
      map(applications => applications.sort((a, b) => a.name.localeCompare(b.name))),
      tap(applications => {
        this.groupApplications.next(applications);
        this.filterGroupApplications(this.applicationsDefaultFilters);
      }),
    );
  }

  private canInviteMember(): boolean {
    return this.group.value.manageable && (this.group.value.system_invitation || this.group.value.email_invitation);
  }

  private canUpdateGroup(): boolean {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  private checkEventRule(group: Group, eventType: string): boolean {
    return group.event_rules.some(rule => rule.event === eventType);
  }

  private hideActionsForReadOnlyUser(): void {
    const groupMembers = this.groupMembers.value;

    this.currentUserService
      .current()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(user => {
        if (user) {
          const index = groupMembers.findIndex(member => member.id === user.id && member.roles['GROUP'] === 'ADMIN');

          if (!this.canUpdateGroup() && index === -1) {
            this.memberColumnDefs.pop();
          }
        }
      });
  }

  private updateEventRules(): void {
    const eventRules = [];

    if (this.groupForm.controls.shouldAddToNewAPIs.value) {
      eventRules.push({ event: 'API_CREATE' });
    }

    if (this.groupForm.controls.shouldAddToNewApplications.value) {
      eventRules.push({ event: 'APPLICATION_CREATE' });
    }

    this.group.value.event_rules = eventRules;
  }

  private updateRoles(): void {
    const roles: any = {};

    if (this.groupForm.controls.defaultAPIRole.value) {
      roles['API'] = this.groupForm.controls.defaultAPIRole.value;
    } else {
      delete roles['API'];
    }

    if (this.groupForm.controls.defaultApplicationRole.value) {
      roles['APPLICATION'] = this.groupForm.controls.defaultApplicationRole.value;
    } else {
      delete roles['APPLICATION'];
    }

    this.group.value.roles = roles;
  }

  private mapUpdatedGroup(): Group {
    const formControls = this.groupForm.controls;
    return {
      ...this.group.value,
      name: formControls.name.value,
      max_invitation: formControls.maxNumberOfMembers.value,
      lock_api_role: !formControls.canAdminChangeAPIRole.value,
      lock_application_role: !formControls.canAdminChangeApplicationRole.value,
      system_invitation: formControls.shouldAllowInvitationViaSearch.value,
      email_invitation: formControls.shouldAllowInvitationViaEmail.value,
      disable_membership_notifications: !formControls.shouldNotifyWhenMemberAdded.value,
    };
  }

  private initializeDefaultRoles(): void {
    this.roleService
      .list('API')
      .pipe(
        tap((roles: Role[]) => {
          this.defaultAPIRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.roleService
      .list('APPLICATION')
      .pipe(
        tap((roles: Role[]) => {
          this.defaultApplicationRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.roleService
      .list('INTEGRATION')
      .pipe(
        tap((roles: Role[]) => {
          this.defaultIntegrationRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.roleService
      .list('CLUSTER')
      .pipe(
        tap((roles: Role[]) => {
          this.defaultClusterRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  saveOrUpdate(): void {
    this.updateEventRules();
    this.updateRoles();

    this.groupService
      .saveOrUpdate(this.mode, this.mapUpdatedGroup())
      .pipe(
        tap((group: Group) => {
          this.snackBarService.success(`Successfully saved the group.`);

          if (this.mode === 'new') {
            this.router.navigate(['..', group.id], { relativeTo: this.route });
          } else {
            this.initializeGroup();
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  openDeleteMemberDialog(member: Member): void {
    this.matDialog
      .open<DeleteMemberDialogComponent, DeleteMemberDialogData, DeleteMemberDialogResult>(DeleteMemberDialogComponent, {
        data: { member, members: this.groupMembers.value },
        role: 'alertdialog',
        id: 'deleteMemberDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter(function (dialogResult: DeleteMemberDialogResult): boolean {
          return dialogResult.shouldDelete;
        }),
        switchMap(dialogResult => this.deleteMember(member, dialogResult)),
        switchMap(dialogResult => this.handleOwnershipTransfer(member, dialogResult)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private deleteMember(member: Member, dialogResult: DeleteMemberDialogResult) {
    return this.groupService.deleteMember(this.groupId, member.id).pipe(
      tap(() => {
        this.snackBarService.success('Successfully deleted member from the group.');
        this.initializeGroupMembers();
      }),
      catchError(() => {
        this.snackBarService.error('Error occurred while deleting member from the group.');
        return EMPTY;
      }),
      map(() => dialogResult),
    );
  }

  private handleOwnershipTransfer(member: Member, dialogResult: DeleteMemberDialogResult): Observable<void> {
    if (member.roles['API'] === RoleName.PRIMARY_OWNER) {
      return this.groupService.addOrUpdateMemberships(this.groupId, [dialogResult.primaryOwnerMembership]).pipe(
        tap(() => {
          this.snackBarService.success('Successfully transferred the ownership.');
          this.initializeGroupMembers();
        }),
        catchError(() => {
          this.snackBarService.error('Error occurred while transferring the ownership.');
          return EMPTY;
        }),
      );
    }
    return of(null);
  }

  openEditMemberDialog(member: Member): void {
    this.matDialog
      .open<EditMemberDialogComponent, EditMemberDialogData, AddOrUpdateMemberDialogResult>(EditMemberDialogComponent, {
        data: {
          group: this.group.value,
          member: member,
          members: this.groupMembers.value,
          defaultAPIRoles: this.defaultAPIRoles,
          defaultApplicationRoles: this.defaultApplicationRoles,
          defaultIntegrationRoles: this.defaultIntegrationRoles,
          defaultClusterRoles: this.defaultClusterRoles,
        },
        role: 'alertdialog',
        id: 'editMemberDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((dialogResult: AddOrUpdateMemberDialogResult) => dialogResult?.memberships?.length > 0),
        switchMap(dialogResult =>
          this.groupService.addOrUpdateMemberships(this.groupId, dialogResult?.memberships).pipe(
            tap(() => {
              this.snackBarService.success('Successfully saved edited member(s) of the group.');
              this.initializeGroupMembers();
            }),
            catchError(() => {
              this.snackBarService.error('Error occurred while saving edited member(s) of the group.');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  openAddMembersDialog(): void {
    this.matDialog
      .open<AddMembersDialogComponent, AddOrInviteMembersDialogData, AddOrUpdateMemberDialogResult>(AddMembersDialogComponent, {
        data: {
          group: this.group.value,
          members: this.groupMembers.value,
          defaultAPIRoles: this.defaultAPIRoles,
          defaultApplicationRoles: this.defaultApplicationRoles,
          defaultIntegrationRoles: this.defaultIntegrationRoles,
          defaultClusterRoles: this.defaultClusterRoles,
        },
        role: 'alertdialog',
        id: 'addMembersDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((dialogResult: AddOrUpdateMemberDialogResult) => dialogResult.memberships?.length > 0),
        switchMap(dialogResult =>
          this.groupService.addOrUpdateMemberships(this.groupId, dialogResult.memberships).pipe(
            tap(() => {
              this.snackBarService.success('Successfully added member(s) to the group.');
              this.initializeGroupMembers();
            }),
            catchError(() => {
              this.snackBarService.error('Error occurred while adding members to the group.');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  openInviteMemberDialog(): void {
    this.matDialog
      .open<InviteMemberDialogComponent, AddOrInviteMembersDialogData, InviteMemberDialogResult>(InviteMemberDialogComponent, {
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
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((dialogResult: InviteMemberDialogResult) => !!dialogResult && !!dialogResult.invitation),
        switchMap(dialogResult =>
          this.groupService.inviteMember(this.groupId, dialogResult.invitation).pipe(
            tap(response => {
              if (response.status === 200) {
                this.snackBarService.success('Successfully invited user to the group.');
                this.initializeInvitations();
                this.initializeGroupMembers();
              } else if (response.status === 202) {
                this.openTooManyUsersDialog(dialogResult.invitation.email);
              }
            }),
            catchError(() => {
              this.snackBarService.error('Error while inviting member to the group.');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  deleteInvitation(invitationId: string, email: string): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete Invitation',
          content: `You are trying to delete an invitation sent to ${email}. Do you want to continue?`,
          confirmButton: 'Continue',
          cancelButton: 'Cancel',
        },
        role: 'alertdialog',
        id: 'deleteInvitationDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed),
        switchMap(() =>
          this.groupService.deleteInvitation(this.groupId, invitationId).pipe(
            tap(() => {
              this.snackBarService.success(`Successfully deleted the invitation.`);
              this.initializeInvitations();
            }),
            catchError(() => {
              this.snackBarService.error(`Error occurred while deleting the invitation.`);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  addToExistingAPIs(group: Group): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Add group to existing APIs',
          content: `You are trying to add the group to all the existing APIs. Do you want to continue?`,
          confirmButton: 'Continue',
          cancelButton: 'Cancel',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.groupService.addToExistingComponents(group.id, 'api')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.initializeGroupAPIs();
          this.snackBarService.success(`Successfully added the group to existing APIs.`);
        },
        error: () => {
          this.snackBarService.error(`Error occurred while adding the group to existing APIs.`);
        },
      });
  }

  addToExistingApplications(group: Group): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Add group to existing applications',
          content: `You are trying to add the group to all the existing applications. Do you want to continue?`,
          confirmButton: 'Continue',
          cancelButton: 'Cancel',
        },
        role: 'alertdialog',
        id: 'confirmDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.groupService.addToExistingComponents(group.id, 'application')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.initializeGroupApplications();
          this.snackBarService.success(`Successfully added the group to existing applications.`);
        },
        error: () => {
          this.snackBarService.error(`Error occurred while adding the group to existing applications.`);
        },
      });
  }

  filterGroupMembers(filters: GioTableWrapperFilters): void {
    this.membersDefaultFilters = { ...this.membersDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupMembers.value, filters);
    this.filteredMembers = filtered.filteredCollection;
    this.noOfFilteredMembers = filtered.unpaginatedLength;
  }

  filterGroupInvitations(filters: GioTableWrapperFilters): void {
    this.invitationsDefaultFilters = { ...this.invitationsDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupInvitations.value, filters);
    this.filteredInvitations = filtered.filteredCollection;
    this.noOfInvitations = filtered.unpaginatedLength;
  }

  filterGroupAPIs(filters: GioTableWrapperFilters): void {
    this.apisDefaultFilters = { ...this.apisDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupAPIs.value, filters);
    this.filteredAPIs = filtered.filteredCollection;
    this.noOfAPIs = filtered.unpaginatedLength;
  }

  filterGroupApplications(filters: GioTableWrapperFilters): void {
    this.applicationsDefaultFilters = { ...this.applicationsDefaultFilters, ...filters };
    const filtered = gioTableFilterCollection(this.groupApplications.value, filters);
    this.filteredApplications = filtered.filteredCollection;
    this.noOfApplications = filtered.unpaginatedLength;
  }

  private disableDeleteMember(): void {
    const groupMembers = this.groupMembers.value;
    this.deleteDisabled = groupMembers.length === 1 && groupMembers[0].roles['API'] === RoleName.PRIMARY_OWNER;
  }

  private disableForm() {
    if (this.mode === 'edit' && !this.canUpdateGroup()) {
      this.groupForm.disable();
      this.disableAddGroupToExistingAPIs = true;
      this.disableAddGroupToExistingApplications = true;
    }

    if (!this.group.value.lock_api_role) {
      this.groupForm.controls.defaultAPIRole.enable();
    }

    if (!this.group.value.lock_application_role) {
      this.groupForm.controls.defaultApplicationRole.enable();
    }
  }

  private shouldAllowAddMembers() {
    this.canAddMembers = (this.canUpdateGroup() || this.canInviteMember()) && !this.maxInvitationsLimitReached;
  }

  private openTooManyUsersDialog(email: string) {
    this.matDialog
      .open<TooManyUsersDialogComponent, TooManyUsersDialogData, boolean>(TooManyUsersDialogComponent, {
        data: {
          email: email,
        },
        role: 'alertdialog',
        id: 'tooManyUsersDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(filter(Boolean), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.openAddMembersDialog();
      });
  }
}

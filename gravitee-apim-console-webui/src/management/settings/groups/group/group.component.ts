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
import { Component, ElementRef, Injector, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { ActivatedRoute } from '@angular/router';

@Component({
  template: '',
  selector: 'settings-group-edit',
  host: {
    class: 'bootstrap gv-sub-content',
  },
})
<<<<<<< HEAD
export class GroupComponent extends UpgradeComponent {
=======
export class GroupComponent implements OnInit {
  group$: Observable<Group> = of(null);
  groupMembers$: Observable<Member[]> = of([]);
  invitations$: Observable<Invitation[]> = of([]);
  groupAPIs$: Observable<any> = of([]);
  groupApplications$: Observable<[]> = of([]);
  defaultAPIRoles: Role[] = [];
  defaultApplicationRoles: Role[] = [];
  defaultIntegrationRoles: Role[] = [];
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
  memberColumnDefs: string[] = ['name', 'defaultApiRole', 'defaultApplicationRole', 'defaultIntegrationRole', 'actions'];
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
  noOfMembers: number = 0;
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
  disableDelete = signal(false);

  private group = new BehaviorSubject<Group>(null);
  private groupMembers = new BehaviorSubject<Member[]>([]);
  private groupInvitations = new BehaviorSubject<Invitation[]>([]);
  private groupAPIs = new BehaviorSubject<any[]>([]);
  private groupApplications = new BehaviorSubject<any[]>([]);
  private destroyRef = inject(DestroyRef);

>>>>>>> 44d2eefe18 (fix: disable adding members beyond max invitations limit in a group)
  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
  ) {
    super('settingsGroupEditAjs', elementRef, injector);
  }

<<<<<<< HEAD
  override ngOnInit() {
    // Hack to Force the binding between Angular and AngularJS
    this.ngOnChanges({
      activatedRoute: new SimpleChange(null, this.activatedRoute, true),
    });

    super.ngOnInit();
=======
  private initializeGroup() {
    this.group$ = this.fetchGroup().pipe(
      tap((group: Group) => {
        this.group.next(group);
        this.initializeForm(group);
        this.initialFormValues = this.groupForm.getRawValue();
        this.hideActionsForReadOnlyUser();
        this.initializeDependents();
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
      map((params) => params.groupId),
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
      defaultAPIRole: new FormControl<string>(!isEmpty(group.roles) ? group.roles['API'] : undefined),
      defaultApplicationRole: new FormControl<string>(!isEmpty(group.roles) ? group.roles['APPLICATION'] : undefined),
      maxNumberOfMembers: new FormControl<number>(group.max_invitation),
      shouldAllowInvitationViaSearch: new FormControl<boolean>(group.system_invitation !== undefined ? group.system_invitation : false),
      shouldAllowInvitationViaEmail: new FormControl<boolean>(group.email_invitation !== undefined ? group.email_invitation : false),
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
      tap((members: Member[]) => {
        this.groupMembers.next(members.sort((a, b) => a.displayName.localeCompare(b.displayName)));
        this.disableDeleteMember();
        this.maxInvitationsLimitReached = this.group.value.max_invitation <= this.groupMembers.value.length;
        this.shouldAllowAddMembers();
        this.filterGroupMembers(this.membersDefaultFilters);
      }),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  private initializeInvitations() {
    this.invitations$ = this.groupService.getInvitations(this.groupId).pipe(
      map((invitations: Invitation[]) => invitations.sort((a, b) => a.email.localeCompare(b.email))),
      tap((invitations) => {
        this.groupInvitations.next(invitations);
        this.filterGroupInvitations(this.invitationsDefaultFilters);
      }),
    );
  }

  private initializeGroupAPIs() {
    this.groupAPIs$ = this.groupService.getMemberships(this.groupId, 'api').pipe(
      map((apis) => apis.sort((a, b) => a.name.localeCompare(b.name))),
      tap((apis) => {
        this.groupAPIs.next(apis);
        this.filterGroupAPIs(this.apisDefaultFilters);
      }),
    );
  }

  private initializeGroupApplications() {
    this.groupApplications$ = this.groupService.getMemberships(this.groupId, 'application').pipe(
      map((applications) => applications.sort((a, b) => a.name.localeCompare(b.name))),
      tap((applications) => {
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
    return group.event_rules.some((rule) => rule.event === eventType);
  }

  private hideActionsForReadOnlyUser(): void {
    if (!this.canUpdateGroup()) {
      this.memberColumnDefs.pop();
    }
  }

  private updateEventRules(): void {
    const eventRules = [];

    if (this.groupForm.controls['shouldAddToNewAPIs'].value) {
      eventRules.push({ event: 'API_CREATE' });
    }

    if (this.groupForm.controls['shouldAddToNewApplications'].value) {
      eventRules.push({ event: 'APPLICATION_CREATE' });
    }

    this.group.value.event_rules = eventRules;
  }

  private updateRoles(): void {
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

  private mapUpdatedGroup(): Group {
    const formControls = this.groupForm.controls;
    return {
      ...this.group.value,
      name: formControls['name'].value,
      max_invitation: formControls['maxNumberOfMembers'].value,
      lock_api_role: !formControls['canAdminChangeAPIRole'].value,
      lock_application_role: !formControls['canAdminChangeApplicationRole'].value,
      system_invitation: formControls['shouldAllowInvitationViaSearch'].value,
      email_invitation: formControls['shouldAllowInvitationViaEmail'].value,
      disable_membership_notifications: !formControls['shouldNotifyWhenMemberAdded'].value,
    };
  }

  private initializeDefaultRoles(): void {
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

  saveOrUpdate(): void {
    this.updateEventRules();
    this.updateRoles();

    this.groupService
      .saveOrUpdate(this.mode, this.mapUpdatedGroup())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (group) => {
          this.snackBarService.success(`Successfully saved the group.`);

          if (this.mode === 'new') {
            this.router.navigate(['..', group.id], { relativeTo: this.route });
          } else {
            this.initializeGroup();
          }
        },
        error: () => this.snackBarService.error(`Error occurred while saving the group.`),
      });
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
        switchMap((dialogResult) => this.deleteMember(member, dialogResult)),
        switchMap((dialogResult) => this.handleOwnershipTransfer(member, dialogResult)),
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
          member,
          members: this.groupMembers.value,
          defaultAPIRoles: this.defaultAPIRoles,
          defaultApplicationRoles: this.defaultApplicationRoles,
          defaultIntegrationRoles: this.defaultIntegrationRoles,
        },
        role: 'alertdialog',
        id: 'editMemberDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((dialogResult: AddOrUpdateMemberDialogResult) => dialogResult.memberships?.length > 0),
        switchMap((dialogResult) =>
          this.groupService.addOrUpdateMemberships(this.groupId, dialogResult.memberships).pipe(
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
        switchMap((dialogResult) =>
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
        filter((dialogResult: InviteMemberDialogResult) => !!dialogResult.invitation),
        switchMap((dialogResult) =>
          this.groupService.inviteMember(this.groupId, dialogResult.invitation).pipe(
            tap(() => {
              this.snackBarService.success('Successfully invited user to the group.');
              this.initializeInvitations();
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
          content: `Are you sure you want to delete an invitation sent to ${email}?`,
          confirmButton: 'Delete',
          cancelButton: 'No',
        },
        role: 'alertdialog',
        id: 'deleteInvitationDialog',
        hasBackdrop: true,
        autoFocus: true,
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
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
          content: `Are you sure you want to add the group to all existing APIs?`,
          confirmButton: 'Add',
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
          content: `Are you sure you want to add the group to all existing applications?`,
          confirmButton: 'Add',
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
    this.disableDelete.set(groupMembers.length === 1 && groupMembers[0].roles['API'] === RoleName.PRIMARY_OWNER);
  }

  private disableForm() {
    if (!this.canUpdateGroup()) {
      this.groupForm.disable();
    }
>>>>>>> 44d2eefe18 (fix: disable adding members beyond max invitations limit in a group)
  }

  private shouldAllowAddMembers() {
    this.canAddMembers = (this.canUpdateGroup() || this.canInviteMember()) && !this.maxInvitationsLimitReached;
  }
}

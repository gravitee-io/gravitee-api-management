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
import { Component, DestroyRef, inject, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatAutocomplete, MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatInput } from '@angular/material/input';
import { catchError, EMPTY, forkJoin, Observable, of, startWith } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { MatChip, MatChipRemove, MatChipSet } from '@angular/material/chips';
import { GioBannerModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSlideToggle } from '@angular/material/slide-toggle';

import { Member, RoleName } from '../membershipState';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPrimaryOwnerMode } from '../../../../../services/apiPrimaryOwnerMode.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { Role } from '../../../../../entities/role/role';
import { GroupMembership, GroupMembershipMemberRoleEntity } from '../../../../../entities/group/groupMember';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { UsersService } from '../../../../../services-ngx/users.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { EditMemberDialogData } from '../group.component';
import { Group } from '../../../../../entities/group/group';

@Component({
  selector: 'edit-member-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatAutocomplete,
    MatAutocompleteTrigger,
    MatInput,
    MatChip,
    MatChipRemove,
    MatChipSet,
    GioFormSlideToggleModule,
    MatSlideToggle,
    GioBannerModule,
  ],
  templateUrl: './edit-member-dialog.component.html',
  styleUrl: './edit-member-dialog.component.scss',
})
export class EditMemberDialogComponent implements OnInit {
  group: Group = null;
  member: Member = null;
  members: Member[] = [];
  defaultAPIRoles: Role[] = [];
  defaultAPIProductRoles: Role[] = [];
  defaultApplicationRoles: Role[] = [];
  defaultIntegrationRoles: Role[] = [];
  defaultClusterRoles: Role[] = [];
  editMemberForm: FormGroup<{
    displayName: FormControl<string>;
    groupAdmin: FormControl<boolean>;
    defaultAPIRole: FormControl<string>;
    defaultAPIProductRole: FormControl<string>;
    defaultApplicationRole: FormControl<string>;
    defaultIntegrationRole: FormControl<string>;
    defaultClusterRole: FormControl<string>;
    searchTerm: FormControl<string>;
  }>;
  ownershipTransferMessage: string = null;
  filteredMembers$: Observable<Member[]> = of([]);
  selectedPrimaryOwner: Member = null;
  memberships: GroupMembership[] = [];
  downgradedMember: Member = null;
  disableSubmit = true;
  disabledAPIRoles = new Set<string>();
  disabledAPIProductRoles = new Set<string>();

  private user: SearchableUser = null;
  private initialValues: any = null;
  private destroyRef = inject(DestroyRef);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: EditMemberDialogData,
    private usersService: UsersService,
    private matDialogRef: MatDialogRef<EditMemberDialogComponent>,
    private permissionService: GioPermissionService,
    private settingsService: EnvironmentSettingsService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.initializeDataFromInput();
    this.getUserDetails();
    this.initializeForm();
    this.initialValues = this.editMemberForm.getRawValue();
    this.initializeFilterFn();
    this.disableControlsForUser();
  }

  private initializeDataFromInput() {
    this.group = this.data.group;
    this.member = this.data.member;
    this.members = this.data.members;
    this.defaultAPIRoles = this.data.defaultAPIRoles;
    this.defaultAPIProductRoles = this.data.defaultAPIProductRoles;
    this.defaultApplicationRoles = this.data.defaultApplicationRoles;
    this.defaultIntegrationRoles = this.data.defaultIntegrationRoles;
    this.defaultClusterRoles = this.data.defaultClusterRoles;
  }

  private initializeForm() {
    this.editMemberForm = new FormGroup({
      displayName: new FormControl<string>(this.member.displayName),
      groupAdmin: new FormControl<boolean>({
        value: this.member.roles['GROUP'] === 'ADMIN',
        disabled: !this.group.system_invitation,
      }),
      defaultAPIRole: new FormControl<string>(this.member.roles['API']),
      defaultAPIProductRole: new FormControl<string>(this.member.roles['API_PRODUCT']),
      defaultApplicationRole: new FormControl<string>(this.member.roles['APPLICATION']),
      defaultIntegrationRole: new FormControl<string>(this.member.roles['INTEGRATION']),
      defaultClusterRole: new FormControl<string>(this.member.roles['CLUSTER']),
      searchTerm: new FormControl<string>({ value: '', disabled: true }),
    });
  }

  private initializeFilterFn() {
    this.filteredMembers$ = this.editMemberForm.controls.searchTerm.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      map((searchTerm: string) => this.filterMembers(searchTerm)),
    );
  }

  private getUserDetails() {
    this.usersService
      .search(this.member.displayName)
      .pipe(
        map((users: SearchableUser[]) => (this.user = users.length > 0 ? users.find(u => u.id === this.member.id) : undefined)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private disableControlsForUser() {
    this.disableDefaultAPIRole();
    this.disableDefaultAPIProductRole();
    this.disableDefaultApplicationRole();
    this.disableDefaultIntegrationRole();
    this.disableDefaultClusterRole();
    this.disableAPIRoleOptions();
    this.disableAPIProductRoleOptions();
  }

  private disableDefaultAPIRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_api_role) {
      this.editMemberForm.controls.defaultAPIRole.disable();
    }
  }

  private disableDefaultAPIProductRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_api_product_role) {
      this.editMemberForm.controls.defaultAPIProductRole.disable();
    }
  }

  private disableDefaultApplicationRole(): void {
    if (!this.canUpdateGroup() && this.group.lock_application_role) {
      this.editMemberForm.controls.defaultApplicationRole.disable();
    }
  }

  private disableDefaultIntegrationRole(): void {
    if (!this.canUpdateGroup()) {
      this.editMemberForm.controls.defaultIntegrationRole.disable();
    }
  }

  private disableDefaultClusterRole(): void {
    if (!this.canUpdateGroup()) {
      this.editMemberForm.controls.defaultClusterRole.disable();
    }
  }

  private disableAPIRoleOptions() {
    this.disabledAPIRoles = new Set(
      this.defaultAPIRoles.filter(role => this.isPrimaryOwnerDisabled(role) || this.isSystemRoleDisabled(role)).map(role => role.id),
    );
  }

  private disableAPIProductRoleOptions(): void {
    this.disabledAPIProductRoles = new Set(
      this.defaultAPIProductRoles
        .filter(role => this.isApiProductPrimaryOwnerDisabled(role) || this.isSystemRoleDisabled(role))
        .map(role => role.id),
    );
  }

  private canUpdateGroup() {
    return this.permissionService.hasAnyMatching(['environment-group-u']);
  }

  private isPrimaryOwnerDisabled(role: Role): boolean {
    return this.checkPrimaryOwnerMode() && role.name === RoleName.PRIMARY_OWNER;
  }

  private isApiProductPrimaryOwnerDisabled(role: Role): boolean {
    return this.checkApiProductPrimaryOwnerMode() && role.name === RoleName.PRIMARY_OWNER;
  }

  private checkPrimaryOwnerMode() {
    return this.settingsService.getSnapshot().api.primaryOwnerMode.toUpperCase() === ApiPrimaryOwnerMode.USER;
  }

  private checkApiProductPrimaryOwnerMode() {
    return this.settingsService.getSnapshot()?.apiProduct?.primaryOwnerMode?.toUpperCase() === ApiPrimaryOwnerMode.USER;
  }

  private isSystemRoleDisabled(role: Role): boolean {
    return role.system && role.name !== RoleName.PRIMARY_OWNER;
  }

  submit() {
    const apiUpgrade = this.isRoleUpgrade('API');
    const apiProductUpgrade = this.isRoleUpgrade('API_PRODUCT');
    const apiDowngrade = this.isRoleDowngrade('API');
    const apiProductDowngrade = this.isRoleDowngrade('API_PRODUCT');

    const editMembership = this.mapEditUserMembership();
    this.setGroupAdminRole(editMembership);

    const apiPo = apiUpgrade ? this.findExistingPrimaryOwner('API') : null;
    const apiProductPo = apiProductUpgrade ? this.findExistingPrimaryOwner('API_PRODUCT') : null;
    const sameOutgoing = !!(apiPo && apiProductPo && apiPo.id === apiProductPo.id);

    const demotionLookups: Observable<GroupMembership>[] = [];
    if (apiPo) {
      const scopes: ('API' | 'API_PRODUCT')[] = sameOutgoing ? ['API', 'API_PRODUCT'] : ['API'];
      demotionLookups.push(this.demote(apiPo, scopes));
    }
    if (apiProductPo && !sameOutgoing) {
      demotionLookups.push(this.demote(apiProductPo, ['API_PRODUCT']));
    }

    const successorLookup: Observable<GroupMembership> | null =
      apiDowngrade || apiProductDowngrade ? this.promoteSuccessor(this.selectedPrimaryOwner, apiDowngrade, apiProductDowngrade) : null;

    if (demotionLookups.length === 0 && !successorLookup) {
      this.memberships = [editMembership];
      this.matDialogRef.close({ memberships: this.memberships });
      return;
    }

    const allLookups = successorLookup ? [...demotionLookups, successorLookup] : demotionLookups;

    forkJoin(allLookups)
      .pipe(
        catchError(() => {
          this.handleTransferLookupFailure();
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(resolved => {
        // Order: previous-PO demotion(s) → edit user → successor promotion.
        const demoted = resolved.slice(0, demotionLookups.length);
        const successor = successorLookup ? resolved[resolved.length - 1] : null;
        this.memberships = successor ? [...demoted, editMembership, successor] : [...demoted, editMembership];
        this.matDialogRef.close({ memberships: this.memberships });
      });
  }

  private promoteSuccessor(successor: Member, apiDowngrade: boolean, apiProductDowngrade: boolean): Observable<GroupMembership> {
    return this.usersService.search(successor.displayName).pipe(
      map(users => {
        const successorUser = users.find(u => u.id === successor.id);
        if (!successorUser) {
          throw new Error(`Could not resolve user reference for member ${successor.id}`);
        }
        const successorMembership = this.buildMembershipFromMember(successor, successorUser.reference);
        if (apiDowngrade) {
          this.setRole(successorMembership, 'API', RoleName.PRIMARY_OWNER);
        }
        if (apiProductDowngrade) {
          this.setRole(successorMembership, 'API_PRODUCT', RoleName.PRIMARY_OWNER);
        }
        return successorMembership;
      }),
    );
  }

  private demote(member: Member, scopes: ('API' | 'API_PRODUCT')[]): Observable<GroupMembership> {
    return this.usersService.search(member.displayName).pipe(
      map(users => {
        const memberUser = users.find(u => u.id === member.id);
        if (!memberUser) {
          throw new Error(`Could not resolve user reference for member ${member.id}`);
        }
        const membership = this.buildMembershipFromMember(member, memberUser.reference);
        scopes.forEach(scope => this.setRole(membership, scope, RoleName.OWNER));
        return membership;
      }),
    );
  }

  private handleTransferLookupFailure(): void {
    this.snackBarService.error('Could not resolve member references for the ownership transfer. Please try again.');
    this.disableSubmit = false;
  }

  private setGroupAdminRole(groupMembership: GroupMembership) {
    if (this.editMemberForm.controls.groupAdmin.value) {
      groupMembership.roles.push({ name: 'ADMIN', scope: 'GROUP' });
    }
  }

  private mapEditUserMembership(): GroupMembership {
    return {
      id: this.user.id,
      reference: this.user.reference,
      roles: [
        { name: this.editMemberForm.controls.defaultAPIRole.value, scope: 'API' },
        { name: this.editMemberForm.controls.defaultAPIProductRole.value, scope: 'API_PRODUCT' },
        { name: this.editMemberForm.controls.defaultApplicationRole.value, scope: 'APPLICATION' },
        { name: this.editMemberForm.controls.defaultIntegrationRole.value, scope: 'INTEGRATION' },
        { name: this.editMemberForm.controls.defaultClusterRole.value, scope: 'CLUSTER' },
      ],
    };
  }

  private buildMembershipFromMember(member: Member, reference: string): GroupMembership {
    const roles: GroupMembershipMemberRoleEntity[] = [];
    Object.entries(member.roles).forEach(([scope, name]) => {
      if (name) {
        roles.push({ name, scope: scope as GroupMembershipMemberRoleEntity['scope'] });
      }
    });
    return { id: member.id, reference, roles };
  }

  private setRole(membership: GroupMembership, scope: GroupMembershipMemberRoleEntity['scope'], name: string) {
    const existing = membership.roles.find(role => role.scope === scope);
    if (existing) {
      existing.name = name;
    } else {
      membership.roles.push({ name, scope });
    }
  }

  selectPrimaryOwner($event: MatAutocompleteSelectedEvent) {
    if (this.isAnyRoleDowngrade()) {
      this.selectedPrimaryOwner = $event.option.value;
      this.ownershipTransferMessage = this.buildOwnershipTransferMessage(this.selectedPrimaryOwner);
      this.editMemberForm.controls.searchTerm.disable();
      this.editMemberForm.controls.searchTerm.setValue('');
      this.editMemberForm.controls.searchTerm.removeValidators(Validators.required);
      this.disableSubmit = false;
    }
  }

  deselectPrimaryOwner() {
    if (this.isAnyRoleDowngrade()) {
      this.selectedPrimaryOwner = null;
      this.ownershipTransferMessage = this.buildOwnershipTransferMessage(null);
      this.editMemberForm.controls.searchTerm.enable();
      this.editMemberForm.controls.searchTerm.addValidators(Validators.required);
      this.disableSubmit = true;
    }
  }

  private filterMembers(searchTerm: string | Member | null | undefined) {
    const term = typeof searchTerm === 'string' ? searchTerm : (searchTerm?.displayName ?? '');
    if (!term.trim()) {
      return [];
    }
    const filterValue = term.toLowerCase();
    return this.members.filter(member => member.displayName.toLowerCase().includes(filterValue) && member.id !== this.member.id);
  }

  displayMember = (member: Member | string | null): string => {
    if (!member) {
      return '';
    }
    return typeof member === 'string' ? member : member.displayName;
  };

  onChange() {
    this.selectedPrimaryOwner = null;
    this.ownershipTransferMessage = null;
    this.downgradedMember = null;
    this.memberships = [];

    const apiUpgrade = this.isRoleUpgrade('API');
    const apiProductUpgrade = this.isRoleUpgrade('API_PRODUCT');
    const apiDowngrade = this.isRoleDowngrade('API');
    const apiProductDowngrade = this.isRoleDowngrade('API_PRODUCT');
    const apiPo = apiUpgrade ? this.findExistingPrimaryOwner('API') : null;
    const apiProductPo = apiProductUpgrade ? this.findExistingPrimaryOwner('API_PRODUCT') : null;

    if (apiDowngrade || apiProductDowngrade) {
      // A downgrade requires the operator to pick a successor; the upgrade-side message (if any) is
      // composed alongside it so the banner reflects ALL transfers happening in this save, including
      // mixed downgrade-on-one-scope + upgrade-on-the-other cases.
      this.editMemberForm.controls.searchTerm.enable();
      this.editMemberForm.controls.searchTerm.addValidators(Validators.required);
      this.downgradedMember = this.member;
      this.disableSubmit = !this.selectedPrimaryOwner;
      this.ownershipTransferMessage = this.buildOwnershipTransferMessage(this.selectedPrimaryOwner);
      return;
    }

    if (apiUpgrade || apiProductUpgrade) {
      this.editMemberForm.controls.searchTerm.disable();
      this.editMemberForm.controls.searchTerm.removeValidators(Validators.required);
      this.downgradedMember = apiPo ?? apiProductPo;
      this.selectedPrimaryOwner = this.member;
      this.ownershipTransferMessage = this.buildUpgradeMessage(apiUpgrade, apiProductUpgrade, apiPo, apiProductPo);
      this.disableSubmit = false;
      return;
    }

    this.editMemberForm.controls.searchTerm.disable();
    this.editMemberForm.controls.searchTerm.removeValidators(Validators.required);
    this.disableSubmit = false;
  }

  private buildOwnershipTransferMessage(successor: Member | null): string | null {
    const apiUpgrade = this.isRoleUpgrade('API');
    const apiProductUpgrade = this.isRoleUpgrade('API_PRODUCT');
    const apiDowngrade = this.isRoleDowngrade('API');
    const apiProductDowngrade = this.isRoleDowngrade('API_PRODUCT');
    const apiPo = apiUpgrade ? this.findExistingPrimaryOwner('API') : null;
    const apiProductPo = apiProductUpgrade ? this.findExistingPrimaryOwner('API_PRODUCT') : null;

    const parts: string[] = [];

    // Downgrade message(s) — only when a successor is actually picked.
    if (successor) {
      if (apiDowngrade && apiProductDowngrade) {
        parts.push(
          `${this.member.displayName} is the API and API Product primary owner. Primary ownership will be transferred to ${successor.displayName} and ${this.member.displayName} will be updated as owner.`,
        );
      } else if (apiDowngrade) {
        parts.push(
          `${this.member.displayName} is the API primary owner. The API primary ownership will be transferred to ${successor.displayName} and ${this.member.displayName} will be updated as owner.`,
        );
      } else if (apiProductDowngrade) {
        parts.push(
          `${this.member.displayName} is the API Product primary owner. The API Product primary ownership will be transferred to ${successor.displayName} and ${this.member.displayName} will be updated as owner.`,
        );
      }
    }

    // Upgrade message(s) — independent of successor selection.
    parts.push(this.buildUpgradeMessage(apiUpgrade, apiProductUpgrade, apiPo, apiProductPo));

    const message = parts.filter(Boolean).join(' ').trim();
    return message.length > 0 ? message : null;
  }

  private buildUpgradeMessage(apiUpgrade: boolean, apiProductUpgrade: boolean, apiPo: Member | null, apiProductPo: Member | null): string {
    if (apiUpgrade && apiProductUpgrade && apiPo && apiProductPo && apiPo.id === apiProductPo.id) {
      return `${apiPo.displayName} is the API and API Product primary owner. Primary ownership will be transferred to ${this.member.displayName} and ${apiPo.displayName} will be updated as owner.`;
    }

    const parts: string[] = [];
    if (apiUpgrade) {
      if (apiPo) {
        parts.push(
          `${apiPo.displayName} is the API primary owner. The API primary ownership will be transferred to ${this.member.displayName} and ${apiPo.displayName} will be updated as owner.`,
        );
      } else {
        parts.push(`${this.member.displayName} will become the API primary owner of this group.`);
      }
    }
    if (apiProductUpgrade) {
      if (apiProductPo) {
        parts.push(
          `${apiProductPo.displayName} is the API Product primary owner. The API Product primary ownership will be transferred to ${this.member.displayName} and ${apiProductPo.displayName} will be updated as owner.`,
        );
      } else {
        parts.push(`${this.member.displayName} will become the API Product primary owner of this group.`);
      }
    }
    return parts.join(' ');
  }

  private findExistingPrimaryOwner(scope: 'API' | 'API_PRODUCT'): Member | null {
    return this.members.find(member => member.roles[scope] === RoleName.PRIMARY_OWNER && member.id !== this.member.id) ?? null;
  }

  private isAnyRoleDowngrade() {
    return this.isRoleDowngrade('API') || this.isRoleDowngrade('API_PRODUCT');
  }

  private isRoleUpgrade(scope: 'API' | 'API_PRODUCT'): boolean {
    const controlName = scope === 'API' ? 'defaultAPIRole' : 'defaultAPIProductRole';
    return (
      this.editMemberForm.controls[controlName].value === RoleName.PRIMARY_OWNER &&
      this.initialValues[controlName] !== RoleName.PRIMARY_OWNER
    );
  }

  private isRoleDowngrade(scope: 'API' | 'API_PRODUCT'): boolean {
    const controlName = scope === 'API' ? 'defaultAPIRole' : 'defaultAPIProductRole';
    return (
      this.initialValues[controlName] === RoleName.PRIMARY_OWNER &&
      this.editMemberForm.controls[controlName].value !== RoleName.PRIMARY_OWNER
    );
  }
}

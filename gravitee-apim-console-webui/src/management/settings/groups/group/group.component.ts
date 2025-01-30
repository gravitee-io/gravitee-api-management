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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { Group } from '../../../../entities/group/group';
import { BehaviorSubject, Observable, of, switchMap, tap } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupService } from '../../../../services-ngx/group.service';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { RoleService } from '../../../../services-ngx/role.service';
import { Role } from 'src/entities/role/role';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-group',
  templateUrl: './group.component.html',
  styleUrls: ['./group.component.scss'],
})
export class GroupComponent implements OnInit {
  dataSource: Observable<Group>;
  apiRoles: Role[];
  applicationRoles: Role[];
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
  initialGroupForm: unknown;
  mode: 'new' | 'edit' = 'new';

  private refreshGroup = new BehaviorSubject(1);
  private group$ = new BehaviorSubject<Group>(null);
  private destroyRef = inject(DestroyRef);

  constructor(
    private groupService: GroupService,
    private route: ActivatedRoute,
    private roleService: RoleService,
    private snackBarService: SnackBarService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.initializeGroup();
  }

  private initializeGroup() {
    this.dataSource = this.refreshGroup.pipe(
      switchMap((_) => this.route.params),
      switchMap(({ groupId }) => {
        if (!!groupId && groupId !== 'new') {
          this.mode = 'edit';
          return this.groupService.get(groupId);
        }
        return of({} as Group);
      }),
      tap((group: Group) => {
        this.group$.next(group);
        this.groupForm = this.initializeForm(group);
        this.initializeDefaultRoles();
        this.initialGroupForm = this.groupForm.getRawValue();
      }),
    );
  }

  private initializeDefaultRoles() {
    this.roleService.list('API').subscribe((roles: Role[]) => {
      this.apiRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
    });

    this.roleService.list('APPLICATION').subscribe((roles) => {
      this.applicationRoles = roles.sort((a, b) => a.name.localeCompare(b.name));
    });
  }

  private initializeForm(group: Group) {
    return new FormGroup({
      name: new FormControl<string>(group.name, { validators: Validators.required }),
      defaultAPIRole: new FormControl<string>(!!group.roles ? group.roles['API'] : undefined),
      defaultApplicationRole: new FormControl<string>(!!group.roles ? group.roles['APPLICATION'] : undefined),
      maxNumberOfMembers: new FormControl<number>(group.max_invitation),
      shouldAllowInvitationViaSearch: new FormControl<boolean>(group.system_invitation),
      shouldAllowInvitationViaEmail: new FormControl<boolean>(group.email_invitation),
      canAdminChangeAPIRole: new FormControl<boolean>(!group.lock_api_role),
      canAdminChangeApplicationRole: new FormControl<boolean>(!group.lock_application_role),
      shouldNotifyWhenMemberAdded: new FormControl<boolean>(!group.disable_membership_notifications),
      shouldAddToNewAPIs: new FormControl<boolean>(!!group.event_rules ? this.checkEventRule(group, 'API_CREATE') : false),
      shouldAddToNewApplications: new FormControl<boolean>(!!group.event_rules ? this.checkEventRule(group, 'APPLICATION_CREATE') : false),
    });
  }

  addToExistingAPIs(group: Group) {
    this.groupService.addToExistingComponents(group.id, 'api').subscribe({
      next: (response: Group) => {
        this.group$.next(response);
        this.snackBarService.success(`Successfully added group ${group.name} to existing applications`);
      },
      error: () => {
        this.snackBarService.error(`Error while adding group ${group.name} to existing APIs`);
      },
    });
  }

  addToExistingApplications(group: Group) {
    this.groupService.addToExistingComponents(group.id, 'application').subscribe({
      next: (response: Group) => {
        this.group$.next(response);
        this.snackBarService.success(`Successfully added group ${group.name} to existing applications`);
      },
      error: () => {
        this.snackBarService.error(`Error while adding group ${group.name} to existing applications`);
      },
    });
  }

  private checkEventRule(group: Group, eventType: string) {
    return group.event_rules.some((rule) => rule.event === eventType);
  }

  saveOrUpdate() {
    this.updateEventRules();
    this.updateRoles();

    const group: Group = {
      ...this.group$.value,
      name: this.groupForm.controls['name'].value,
      max_invitation: this.groupForm.controls['maxNumberOfMembers'].value,
      lock_api_role: !this.groupForm.controls['canAdminChangeAPIRole'].value,
      lock_application_role: !this.groupForm.controls['canAdminChangeApplicationRole'].value,
      system_invitation: this.groupForm.controls['shouldAllowInvitationViaSearch'].value,
      email_invitation: this.groupForm.controls['shouldAllowInvitationViaEmail'].value,
      disable_membership_notifications: !this.groupForm.controls['shouldNotifyWhenMemberAdded'].value,
    };

    this.groupService
      .saveOrUpdate(this.mode, group)
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
        error: () => this.snackBarService.error(`Error while saving group ${group.name}`),
      });
  }

  private updateEventRules() {
    const eventRules = [];

    if (this.groupForm.controls['shouldAddToNewAPIs'].value) {
      eventRules.push({ event: 'API_CREATE' });
    }

    if (this.groupForm.controls['shouldAddToNewApplications'].value) {
      eventRules.push({ event: 'APPLICATION_CREATE' });
    }

    this.group$.value.event_rules = eventRules;
  }

  private updateRoles() {
    const roles: any = {};

    if (this.groupForm.controls['defaultAPIRole'].value) {
      roles.API = this.groupForm.controls['defaultAPIRole'].value;
    } else {
      delete roles.API;
    }

    if (this.groupForm.controls['defaultApplicationRole'].value) {
      roles.APPLICATION = this.groupForm.controls['defaultApplicationRole'].value;
    } else {
      delete roles.APPLICATION;
    }

    this.group$.value.roles = roles;
  }
}

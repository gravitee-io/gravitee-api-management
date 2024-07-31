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
import { Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, Subject } from 'rxjs';
import { filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { isEmpty } from 'lodash';

import { RoleService } from '../../../../../services-ngx/role.service';
import { ApplicationMembersService } from '../../../../../services-ngx/application-members.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { Role } from '../../../../../entities/role/role';
import { Member } from '../../../../../entities/members/members';
import { ApplicationTransferOwnership } from '../../../../../entities/application/Application';
import { ApplicationService } from '../../../../../services-ngx/application.service';

@Component({
  selector: 'application-general-transfer-ownership',
  templateUrl: './application-general-transfer-ownership.component.html',
  styleUrls: ['./application-general-transfer-ownership.component.scss'],
})
export class ApplicationGeneralTransferOwnershipComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  form: UntypedFormGroup;
  applicationMembers: Member[];
  roles: Role[];
  isReadonly = false;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly applicationService: ApplicationService,
    private readonly roleService: RoleService,
    private readonly applicationMembersService: ApplicationMembersService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.applicationService.getById(this.activatedRoute.snapshot.params.applicationId),
      this.applicationMembersService.get(this.activatedRoute.snapshot.params.applicationId),
      this.roleService.list('APPLICATION'),
    ])
      .pipe(
        tap(([app, members, roles]) => {
          this.isReadonly = app.origin === 'KUBERNETES';
          this.applicationMembers = members.filter((member) => member.role !== 'PRIMARY_OWNER');
          this.roles = roles.filter((role) => role.name !== 'PRIMARY_OWNER');
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        const defaultRole = this.roles.find((role) => role.default);
        this.initForm(defaultRole);
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  onSubmit() {
    const newRole = this.form.get('roleId').value;

    const confirmDialog = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
      data: {
        title: 'Transfer Application ownership',
        content: `This action cannot be undone. If you are the primary owner of this Application, your role will be set to <code>${newRole}</code>.`,
        confirmButton: 'Transfer',
      },
      role: 'alertdialog',
      id: 'confirmTransferDialog',
    });

    const userMode = this.form.get('method').value;
    const user = this.form.get('user').value;
    const isUserMode = userMode === 'user' || userMode === 'applicationMember';

    const transferOwnershipToUser: ApplicationTransferOwnership = {
      id: user?.id,
      reference: user?.reference,
      role: newRole,
    };

    confirmDialog
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() =>
          this.applicationMembersService.transferOwnership(
            this.activatedRoute.snapshot.params.applicationId,
            isUserMode ? transferOwnershipToUser : {},
          ),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Transfer ownership done.');
          this.router.navigate(['../members'], { relativeTo: this.activatedRoute });
        },
        error: ({ error }) => this.snackBarService.error(error.message),
      });
  }

  private initForm(defaultRole: Role) {
    this.form = new UntypedFormGroup(
      {
        method: new UntypedFormControl('applicationMember'),
        user: new UntypedFormControl(),
        roleId: new UntypedFormControl(defaultRole.name),
      },
      [
        (control: AbstractControl): ValidationErrors | null => {
          const errors: ValidationErrors = {};
          if (!control.get('method').value) {
            errors.methodRequired = true;
          }

          const userMode = control.get('method').value;

          const isUserMode = userMode === 'user' || userMode === 'applicationMember';

          if (isUserMode && isEmpty(control.get('user').value)) {
            errors.userRequired = true;
          }
          if (!control.get('roleId').value) {
            errors.roleRequired = true;
          }

          return errors ? errors : null;
        },
      ],
    );

    this.form
      .get('method')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => {
        this.form.get('user').reset();
      });

    if (this.isReadonly) {
      this.form.disable({ emitEvent: false });
    }
  }
}

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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { StateService } from '@uirouter/core';

import { UsersService } from '../../../../services-ngx/users.service';
import { NewPreRegisterUser } from '../../../../entities/user/newPreRegisterUser';
import { IdentityProviderService } from '../../../../services-ngx/identity-provider.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UIRouterState } from '../../../../ajs-upgraded-providers';

export enum UserType {
  EXTERNAL_USER = 'EXTERNAL_USER',
  SERVICE_ACCOUNT = 'SERVICE_ACCOUNT',
}

@Component({
  selector: 'org-settings-new-user',
  template: require('./org-settings-new-user.component.html'),
  styles: [require('./org-settings-new-user.component.scss')],
})
export class OrgSettingsNewUserComponent implements OnInit, OnDestroy {
  isLoading = true;
  isCreating = false;
  identityProviders?: Array<{ id: string; name: string }>;

  UserType = UserType;

  userForm: FormGroup;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  private readonly graviteeIdp = {
    id: 'gravitee',
    name: 'Gravitee',
  };

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly usersService: UsersService,
    private readonly identityProviderService: IdentityProviderService,
    private readonly snackBarService: SnackBarService,
    @Inject(UIRouterState) private readonly $state: StateService,
  ) {}

  ngOnInit(): void {
    this.identityProviderService
      .list()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((identityProviders) => {
        this.identityProviders = identityProviders;

        let sourceFormControl = {};

        if (this.identityProviders && this.identityProviders.length > 0) {
          this.identityProviders.unshift(this.graviteeIdp);
          sourceFormControl = {
            source: [this.graviteeIdp.id, [Validators.required]],
            sourceId: [''],
          };
        }

        this.userForm = this.formBuilder.group({
          type: [UserType.EXTERNAL_USER],
          firstName: ['', Validators.required],
          lastName: ['', Validators.required],
          email: ['', [Validators.required, Validators.email]],
          ...sourceFormControl,
        });

        this.userForm
          .get('type')
          ?.valueChanges.pipe(takeUntil(this.unsubscribe$))
          // eslint-disable-next-line rxjs/no-nested-subscribe
          .subscribe((type) => {
            this.userForm.removeControl('firstName');
            this.userForm.removeControl('lastName');
            this.userForm.removeControl('email');

            if (type === UserType.SERVICE_ACCOUNT) {
              this.userForm.addControl('lastName', new FormControl('', [Validators.required]));
              this.userForm.addControl('email', new FormControl('', [Validators.email]));
            } else {
              this.userForm.addControl('lastName', new FormControl('', [Validators.required]));
              this.userForm.addControl('email', new FormControl('', [Validators.required, Validators.email]));
              this.userForm.addControl('firstName', new FormControl('', [Validators.required]));
            }
          });

        this.userForm
          .get('source')
          ?.valueChanges.pipe(takeUntil(this.unsubscribe$))
          // eslint-disable-next-line rxjs/no-nested-subscribe
          .subscribe((source) => {
            if (source !== this.graviteeIdp.id) {
              this.userForm.get('sourceId').addValidators(Validators.required);
            } else {
              this.userForm.get('sourceId').clearValidators();
              this.userForm.get('sourceId').reset();
            }
            this.userForm.updateValueAndValidity();
          });

        this.isLoading = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    if (this.userForm.invalid || this.isCreating) {
      return;
    }

    this.isCreating = true;
    const userFormValue = this.userForm.getRawValue();

    const userToCreate: NewPreRegisterUser = {
      firstname: userFormValue.firstName,
      lastname: userFormValue.lastName,
      email: userFormValue.email,
      source: userFormValue.source,
      sourceId: userFormValue.source === this.graviteeIdp.id ? '' : userFormValue.sourceId,
      service: userFormValue.type === UserType.SERVICE_ACCOUNT,
    };

    this.usersService
      .create(userToCreate)
      .pipe(
        tap(() => {
          this.snackBarService.success('New user successfully registered!');
          this.$state.go('organization.users');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          this.isCreating = false;
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}

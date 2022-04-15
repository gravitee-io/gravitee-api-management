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
import { Component, Inject, OnDestroy } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { LocationStrategy } from '@angular/common';

import { NewToken, Token } from '../../../../../entities/user/userTokens';
import { UsersTokenService } from '../../../../../services-ngx/users-token.service';
import { Constants } from '../../../../../entities/Constants';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

export type OrgSettingsUserGenerateTokenDialogData = {
  token?: Token;
  userId: string;
};

@Component({
  selector: 'org-settings-add-tenant',
  template: require('./org-settings-user-generate-token.component.html'),
  styles: [require('./org-settings-user-generate-token.component.scss')],
})
export class OrgSettingsUserGenerateTokenComponent implements OnDestroy {
  token?: Token;
  tokenForm: FormGroup;
  hasBeenGenerated = false;

  private unsubscribe$ = new Subject<boolean>();
  private userId: string;

  constructor(
    private usersTokenService: UsersTokenService,
    private locationStrategy: LocationStrategy,
    private readonly snackBarService: SnackBarService,
    @Inject(MAT_DIALOG_DATA) confirmDialogData: OrgSettingsUserGenerateTokenDialogData,
    @Inject('Constants') private readonly constants: Constants,
  ) {
    this.token = confirmDialogData.token;
    this.userId = confirmDialogData.userId;

    this.tokenForm = new FormGroup({
      name: new FormControl(this.token?.name, [Validators.required, Validators.minLength(2), Validators.maxLength(64)]),
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    const newToken: NewToken = {
      name: this.tokenForm.get('name').value,
    };

    this.usersTokenService
      .createToken(this.userId, newToken)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => {
          this.snackBarService.success('Token successfully created!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe((response) => {
        this.hasBeenGenerated = true;
        this.token = response;
      });
  }

  getExampleOfUse(token: string): string {
    let envBaseURL = `${this.constants.org.baseURL}/environments/${this.constants.org.currentEnv.id}`;
    if (envBaseURL.startsWith('/')) {
      envBaseURL = this.locationStrategy.getBaseHref() + envBaseURL;
    }
    return `curl -H "Authorization: Bearer ${token}" "${envBaseURL}"`;
  }
}

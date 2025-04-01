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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { ReCaptchaService } from '../../services-ngx/re-captcha.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { AuthService } from '../../auth/auth.service';
import { CustomUserField } from '../../entities/customUserFields';

@Component({
  selector: 'sign-up',
  templateUrl: './sign-up.component.html',
  styleUrls: ['../auth-common.component.scss'],
  standalone: false,
})
export class SignUpComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public signUpForm = new UntypedFormGroup({
    firstName: new UntypedFormControl('', Validators.required),
    lastName: new UntypedFormControl('', Validators.required),
    email: new UntypedFormControl('', [Validators.required, Validators.email]),
  });
  public customUserFields?: Array<
    CustomUserField & {
      type: 'input' | 'select';
    }
  >;
  public signUpSuccess = false;

  public signUpInProgress = false;

  constructor(
    private readonly reCaptchaService: ReCaptchaService,
    private readonly snackBarService: SnackBarService,
    private readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.reCaptchaService.displayBadge();

    this.authService
      .signUpCustomUserFields()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((customUserFields) => {
        this.customUserFields = customUserFields.map((customUserField) => ({
          ...customUserField,
          type: isEmpty(customUserField.values) ? 'input' : 'select',
        }));

        if (customUserFields.length > 0) {
          this.signUpForm.addControl(
            'customUserFields',
            new UntypedFormGroup({
              ...customUserFields.reduce((acc, customUserField) => {
                acc[customUserField.key] = new UntypedFormControl(null, customUserField.required ? Validators.required : undefined);
                return acc;
              }, {}),
            }),
          );
        }
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  signUp() {
    this.signUpInProgress = true;
    this.reCaptchaService
      .execute('register')
      .then(() => {
        this.authService
          .signUp({
            firstName: this.signUpForm.get('firstName').value,
            lastName: this.signUpForm.get('lastName').value,
            email: this.signUpForm.get('email').value,
            customFields: this.signUpForm.get('customUserFields')?.value,
          })
          .pipe(
            tap(() => {
              this.signUpSuccess = true;
              this.snackBarService.success('Your account has been created.');
            }),
            takeUntil(this.unsubscribe$),
          )
          .subscribe({
            error: (e) => {
              this.signUpInProgress = false;
              this.snackBarService.error(e.error?.message ?? 'An error occurred while creating your account.');
            },
            next: () => {
              this.signUpInProgress = false;
            },
          });
      })
      .catch(() => {
        this.signUpInProgress = false;
      });
  }
}

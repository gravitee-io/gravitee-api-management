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
import { UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import { jwtDecode, JwtPayload } from 'jwt-decode';
import { ActivatedRoute, Router } from '@angular/router';

import { ReCaptchaService } from '../../services-ngx/re-captcha.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['../auth-common.component.scss'],
  standalone: false,
})
export class ResetPasswordComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public resetPasswordForm: UntypedFormGroup;
  public resetPasswordError?: string;
  public resetPasswordSuccess = false;
  public resetPasswordInProgress = false;

  private userTokenDecoded: JwtPayload & {
    firstname?: string;
    lastname?: string;
    email: string;
  };

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly reCaptchaService: ReCaptchaService,
    private readonly snackBarService: SnackBarService,
    private readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.reCaptchaService.displayBadge();

    const token = this.activatedRoute.snapshot.params.token;

    try {
      this.userTokenDecoded = jwtDecode<
        JwtPayload & {
          firstname?: string;
          lastname?: string;
          email: string;
        }
      >(token);
    } catch (error) {
      this.resetPasswordError = 'Invalid registration token!';
      throw error;
    }
    if (!this.userTokenDecoded) {
      this.resetPasswordError = 'Invalid registration token!';
      return;
    }
    if (this.userTokenDecoded.exp && this.userTokenDecoded.exp * 1000 < Date.now()) {
      this.resetPasswordError = 'Your registration token has expired!';
      return;
    }

    this.resetPasswordForm = new UntypedFormGroup(
      {
        firstName: new UntypedFormControl({
          value: this.userTokenDecoded.firstname,
          disabled: true,
        }),
        lastName: new UntypedFormControl({
          value: this.userTokenDecoded.lastname,
          disabled: true,
        }),
        email: new UntypedFormControl({
          value: this.userTokenDecoded.email,
          disabled: true,
        }),
        password: new UntypedFormControl(null, Validators.required),
        confirmPassword: new UntypedFormControl(null, Validators.required),
      },
      {
        validators: [passwordValidator],
      },
    );
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  resetPassword() {
    this.resetPasswordInProgress = true;
    this.reCaptchaService
      .execute('register')
      .then(() => {
        this.authService
          .resetPassword({
            userId: this.userTokenDecoded.sub,
            token: this.activatedRoute.snapshot.params.token,
            firstName: this.userTokenDecoded.firstname,
            lastName: this.userTokenDecoded.lastname,
            password: this.resetPasswordForm.get('password').value,
          })
          .pipe(
            tap(() => {
              this.resetPasswordSuccess = true;
            }),
            switchMap(() => this.snackBarService.success('Your password has been reset.').afterDismissed()),
            takeUntil(this.unsubscribe$),
          )
          .subscribe({
            next: () => {
              this.resetPasswordInProgress = false;
            },
            error: (e) => {
              this.snackBarService.error(e.error?.message ?? 'An error occurred while resetting your password.');
              this.resetPasswordInProgress = false;
            },
          });
      })
      .catch(() => {
        this.resetPasswordInProgress = false;
      });
  }
}

const passwordValidator: ValidatorFn = (control: UntypedFormGroup): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  const passwordNotMatch = password.value && confirmPassword.value && password.value !== confirmPassword.value;

  return passwordNotMatch ? { passwordNotMatch: true } : null;
};

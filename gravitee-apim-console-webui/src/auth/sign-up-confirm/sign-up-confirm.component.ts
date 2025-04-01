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
import { jwtDecode, JwtPayload } from 'jwt-decode';
import { ActivatedRoute, Router } from '@angular/router';
import { switchMap, takeUntil, tap } from 'rxjs/operators';

import { ReCaptchaService } from '../../services-ngx/re-captcha.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'sign-up-confirm',
  templateUrl: './sign-up-confirm.component.html',
  styleUrls: ['../auth-common.component.scss'],
  standalone: false,
})
export class SignUpConfirmComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public signUpConfirmForm: UntypedFormGroup;
  public signUpConfirmError?: string;
  public signUpConfirmSuccess = false;
  public signUpConfirmInProgress = false;

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
    let userTokenDecoded: JwtPayload & {
      firstname?: string;
      lastname?: string;
      email: string;
    };

    try {
      userTokenDecoded = jwtDecode<
        JwtPayload & {
          firstname?: string;
          lastname?: string;
          email: string;
        }
      >(token);
    } catch (error) {
      this.signUpConfirmError = 'Invalid registration token!';
      throw error;
    }
    if (!userTokenDecoded) {
      this.signUpConfirmError = 'Invalid registration token!';
      return;
    }
    if (userTokenDecoded.exp && userTokenDecoded.exp * 1000 < Date.now()) {
      this.signUpConfirmError = 'Your registration token has expired!';
      return;
    }

    this.signUpConfirmForm = new UntypedFormGroup(
      {
        firstName: new UntypedFormControl(
          {
            value: userTokenDecoded.firstname,
            disabled: !!userTokenDecoded.firstname,
          },
          Validators.required,
        ),
        lastName: new UntypedFormControl(
          {
            value: userTokenDecoded.lastname,
            disabled: !!userTokenDecoded.lastname,
          },
          Validators.required,
        ),
        email: new UntypedFormControl({
          value: userTokenDecoded.email,
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

  signUpConfirm() {
    this.signUpConfirmInProgress = true;
    this.reCaptchaService
      .execute('finalizeRegistration')
      .then(() => {
        this.authService
          .signUpConfirm({
            token: this.activatedRoute.snapshot.params.token,
            password: this.signUpConfirmForm.get('password').value,
            firstName: this.signUpConfirmForm.get('firstName').value,
            lastName: this.signUpConfirmForm.get('lastName').value,
          })
          .pipe(
            tap(() => {
              this.signUpConfirmSuccess = true;
            }),
            switchMap(() => this.snackBarService.success('Your account has been confirmed.').afterDismissed()),
            takeUntil(this.unsubscribe$),
          )
          .subscribe({
            next: () => {
              this.signUpConfirmInProgress = false;
              this.router.navigateByUrl('/_login');
            },
            error: (e) => {
              this.signUpConfirmInProgress = false;
              this.snackBarService.error(e.error?.message ?? 'An error occurred while confirming your account.');
            },
          });
      })
      .catch(() => {
        this.signUpConfirmInProgress = false;
      });
  }
}

const passwordValidator: ValidatorFn = (control: UntypedFormGroup): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  const passwordNotMatch = password.value && confirmPassword.value && password.value !== confirmPassword.value;

  return passwordNotMatch ? { passwordNotMatch: true } : null;
};

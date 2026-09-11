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
import { CUSTOM_ELEMENTS_SCHEMA, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ResetUserPasswordInput, UsersService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ReCaptchaService } from '../../services/recaptcha.service';
import { GvFormControlDirective } from '../../directives/gv-form-control.directive';

type ResetPasswordFormType = FormGroup<{
  username: FormControl<string>;
}>;

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css'],
  imports: [ReactiveFormsModule, NgIf, GvFormControlDirective, RouterLink, TranslatePipe],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ResetPasswordComponent implements OnInit {
  private usersService = inject(UsersService);
  private reCaptchaService = inject(ReCaptchaService);

  isSubmitted: boolean;
  resetPasswordForm: ResetPasswordFormType;

  constructor() {
    this.isSubmitted = false;
  }

  ngOnInit() {
    this.resetPasswordForm = new FormGroup({
      username: new FormControl<string>(''),
    });
    this.reCaptchaService.displayBadge();
  }

  onSubmitResetPassword() {
    if (this.resetPasswordForm.valid && !this.isSubmitted) {
      const resetUserPasswordInput: ResetUserPasswordInput = {
        username: this.resetPasswordForm.value.username,
        reset_page_url: window.location.href + '/confirm',
      };
      this.reCaptchaService.execute('reset_password').then(() => {
        this.usersService
          .resetUserPassword({ resetUserPasswordInput })
          .toPromise()
          .then(() => (this.isSubmitted = true))
          .catch(() => ({}));
      });
    }
  }
}

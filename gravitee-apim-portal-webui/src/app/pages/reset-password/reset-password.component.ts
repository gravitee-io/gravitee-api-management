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
import { FormBuilder, FormGroup } from '@angular/forms';

import { ResetUserPasswordInput, UsersService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ReCaptchaService } from '../../services/recaptcha.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css'],
})
export class ResetPasswordComponent implements OnInit {
  isSubmitted: boolean;
  resetPasswordForm: FormGroup;

  constructor(private usersService: UsersService, private formBuilder: FormBuilder, private reCaptchaService: ReCaptchaService) {
    this.isSubmitted = false;
  }

  ngOnInit() {
    this.resetPasswordForm = this.formBuilder.group({
      username: '',
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

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
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { RegisterUserInput, UsersService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ReCaptchaService } from '../../services/recaptcha.service';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css'],
})
export class RegistrationComponent implements OnInit {
  isSubmitted: boolean;
  registrationForm: FormGroup;

  constructor(private usersService: UsersService, private formBuilder: FormBuilder, private reCaptchaService: ReCaptchaService) {
    this.isSubmitted = false;
  }

  ngOnInit() {
    this.registrationForm = this.formBuilder.group({
      firstname: new FormControl('', Validators.required),
      lastname: new FormControl('', Validators.required),
      email: new FormControl('', [Validators.required, Validators.email]),
    });
    this.reCaptchaService.displayBadge();
  }

  onSubmitRegistration() {
    if (this.registrationForm.valid && !this.isSubmitted) {
      const input: RegisterUserInput = {
        email: this.registrationForm.value.email,
        firstname: this.registrationForm.value.firstname,
        lastname: this.registrationForm.value.lastname,
        confirmation_page_url: window.location.href + '/confirm',
      };
      this.reCaptchaService.execute('registration').then(() => {
        this.usersService
          .registerNewUser({ RegisterUserInput: input })
          .toPromise()
          .then(() => (this.isSubmitted = true))
          .catch(() => {});
      });
    }
  }
}

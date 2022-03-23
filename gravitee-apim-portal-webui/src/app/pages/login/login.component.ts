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
import { AfterViewInit, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { IdentityProvider, PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';

import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-icon';
import '@gravitee/ui-components/wc/gv-input';
import { NotificationService } from '../../services/notification.service';
import { ConfigurationService } from '../../services/configuration.service';
import { FeatureEnum } from '../../model/feature.enum';
import { AuthService } from '../../services/auth.service';
import { ReCaptchaService } from '../../services/recaptcha.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {
  loginForm: FormGroup;
  registrationEnabled: boolean;
  loginEnabled: boolean;
  providers: IdentityProvider[];
  private redirectUrl: string;
  firstClickHandler: any;

  constructor(
    private portalService: PortalService,
    private formBuilder: FormBuilder,
    private router: Router,
    private notificationService: NotificationService,
    private config: ConfigurationService,
    private authService: AuthService,
    private activatedRoute: ActivatedRoute,
    private reCaptchaService: ReCaptchaService,
  ) {}

  ngOnInit() {
    this.firstClickHandler = this.onFirstClick.bind(this);

    this.loginForm = this.formBuilder.group({ username: '', password: '' });
    this.loginEnabled = this.config.hasFeature(FeatureEnum.localLogin);
    this.registrationEnabled = this.config.hasFeature(FeatureEnum.userRegistration);
    this.redirectUrl = this.activatedRoute.snapshot.queryParams.redirectUrl || '';
    this.portalService.getPortalIdentityProviders().subscribe(
      configurationIdentitiesResponse => {
        this.providers = configurationIdentitiesResponse.data;
      },
      error => {
        console.error('something wrong occurred with identity providers: ' + error.statusText);
      },
    );
    this.reCaptchaService.displayBadge();
  }

  private onFirstClick() {
    this.loginForm.get('username').setValidators(Validators.required);
    this.loginForm.get('username').setValue(null);
    this.loginForm.get('password').setValidators(Validators.required);
    this.loginForm.get('password').setValue(null);
    this.loginForm.markAllAsTouched();
    window.removeEventListener('click', this.firstClickHandler);
  }

  ngAfterViewInit() {
    if (this.loginForm) {
      window.addEventListener('click', this.firstClickHandler);
    }
  }

  ngOnDestroy() {
    window.removeEventListener('click', this.firstClickHandler);
  }

  login() {
    if (this.isFormValid()) {
      this.reCaptchaService.execute('login').then(() => {
        this.authService.login(this.loginForm.value.username, this.loginForm.value.password, this.redirectUrl).then(
          () => ({}),
          () => {
            this.loginForm.setValue({ username: this.loginForm.value.username, password: '' });
          },
        );
      });
    }
  }

  authenticate(provider) {
    this.authService.authenticate(provider, this.redirectUrl);
  }

  isFormValid() {
    return this.loginForm.valid.valueOf();
  }

  getProviderStyle(provider) {
    if (provider.color) {
      return `--gv-button-${provider.type.toLowerCase()}--bgc:${provider.color};`;
    }
    return '';
  }
}

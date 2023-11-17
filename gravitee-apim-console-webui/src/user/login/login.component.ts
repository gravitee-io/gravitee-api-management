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
import { from, Subject } from 'rxjs';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { switchMap, takeUntil } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { ReCaptchaService } from '../../services-ngx/re-captcha.service';
import { IdentityProvider } from '../../entities/identity-provider';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { SocialIdentityProvider } from '../../entities/organization/socialIdentityProvider';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'login',
  template: require('./login.component.html'),
  styles: [require('./login.component.scss')],
})
export class LoginComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public identityProviders: SocialIdentityProvider[] = [];
  public loginForm = new FormGroup({
    username: new FormControl('', Validators.required),
    password: new FormControl('', Validators.required),
  });
  public localLoginDisabled = false;
  public userCreationEnabled = false;

  constructor(
    @Inject('Constants') private readonly constants,
    private readonly activatedRoute: ActivatedRoute,
    private readonly reCaptchaService: ReCaptchaService,
    private readonly snackBarService: SnackBarService,
    private readonly authService: AuthService,
  ) {
    this.userCreationEnabled = constants.org.settings.management?.userCreation?.enabled ?? false;
    this.localLoginDisabled = !(constants.org.settings.authentication?.localLogin?.enabled ?? true);
  }

  ngOnInit(): void {
    this.reCaptchaService.displayBadge();

    this.identityProviders = this.constants.org.identityProviders ?? [];

    if (this.localLoginDisabled === true && this.identityProviders.length === 0) {
      this.snackBarService.error('No login method available. Please contact your administrator.');
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  login() {
    const redirect = this.activatedRoute.snapshot.queryParams['redirect'];

    from(this.reCaptchaService.execute('login'))
      .pipe(
        switchMap(() =>
          this.authService.loginWithApim(
            {
              username: this.loginForm.get('username').value,
              password: this.loginForm.get('password').value,
            },
            redirect,
          ),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        error: () => {
          this.snackBarService.error('Login failed! Check username and password.');
        },
      });
  }

  authenticate(identityProvider: SocialIdentityProvider) {
    const redirect = this.activatedRoute.snapshot.queryParams['redirect'];
    this.authService
      .loginWithProvider(identityProvider.id, redirect ?? '/')
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }

  getProviderStyle(provider: IdentityProvider) {
    return {
      'background-color': getProviderBackGroundColor(provider),
      color: getProviderColor(provider),
    };
  }
}

const getProviderColor = (provider: IdentityProvider) => {
  if (provider.type === 'GRAVITEEIO_AM') {
    return '#383E3F';
  }
  return 'white';
};

const getProviderBackGroundColor = (provider: IdentityProvider) => {
  if (provider.color) {
    return provider.color;
  }
  if (provider.type === 'OIDC') {
    return 'black';
  }
  if (provider.type === 'GRAVITEEIO_AM') {
    return '#86c3d0';
  }
  return '';
};

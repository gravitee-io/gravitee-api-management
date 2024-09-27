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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { switchMap, takeUntil } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { isEmpty, uniq } from 'lodash';

import { ReCaptchaService } from '../../services-ngx/re-captcha.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { SocialIdentityProvider } from '../../entities/organization/socialIdentityProvider';
import { AuthService } from '../auth.service';
import { Constants } from '../../entities/Constants';

export type SocialIdentityProviderVM = SocialIdentityProvider & { textColor?: string };

@Component({
  selector: 'login',
  templateUrl: './login.component.html',
  styleUrls: ['../auth-common.component.scss', './login.component.scss'],
})
export class LoginComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public identityProviders: SocialIdentityProviderVM[] = [];
  public loginForm = new UntypedFormGroup({
    username: new UntypedFormControl('', Validators.required),
    password: new UntypedFormControl('', Validators.required),
  });
  public localLoginDisabled = false;
  public userCreationEnabled = false;
  public loginInProgress = false;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly activatedRoute: ActivatedRoute,
    private readonly reCaptchaService: ReCaptchaService,
    private readonly snackBarService: SnackBarService,
    private readonly authService: AuthService,
    private readonly iconRegistry: MatIconRegistry,
    private readonly sanitizer: DomSanitizer,
  ) {
    this.userCreationEnabled = constants.org.settings.management?.userCreation?.enabled ?? false;
    this.localLoginDisabled = !(constants.org.settings.authentication?.localLogin?.enabled ?? true);
    this.identityProviders = (this.constants.org.identityProviders ?? []).map((idp) => ({
      ...idp,
      textColor: getProviderTextColor(idp),
    }));

    uniq(this.identityProviders.map((i) => i.type)).forEach((type) => {
      this.iconRegistry.addSvgIcon(
        `idp-${type.toLowerCase()}`,
        this.sanitizer.bypassSecurityTrustResourceUrl('assets/logo_' + type.toLowerCase() + '-idp.svg'),
      );
    });
  }

  ngOnInit(): void {
    this.reCaptchaService.displayBadge();

    if (this.localLoginDisabled === true && this.identityProviders.length === 0) {
      this.snackBarService.error('No login method available. Please contact your administrator.');
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  login() {
    this.loginInProgress = true;
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
        next: () => {
          this.loginInProgress = false;
        },
        error: () => {
          this.loginInProgress = false;
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
}

const getProviderTextColor = (provider: SocialIdentityProviderVM) => {
  if (isEmpty(provider.color)) {
    return;
  }
  return colorIsDarkAdvanced(provider.color) ? 'white' : 'black';
};

const colorIsDarkAdvanced = (bgColor: string): boolean => {
  const color = bgColor.charAt(0) === '#' ? bgColor.substring(1, 7) : bgColor;
  const r = parseInt(color.substring(0, 2), 16); // hexToR
  const g = parseInt(color.substring(2, 4), 16); // hexToG
  const b = parseInt(color.substring(4, 6), 16); // hexToB
  const uicolors = [r / 255, g / 255, b / 255];
  const c = uicolors.map((col) => {
    if (col <= 0.03928) {
      return col / 12.92;
    }
    return Math.pow((col + 0.055) / 1.055, 2.4);
  });
  const L = 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
  return L <= 0.179;
};

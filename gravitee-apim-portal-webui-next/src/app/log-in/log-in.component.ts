/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { OAuthModule } from 'angular-oauth2-oidc';
import { map, switchMap, tap } from 'rxjs';

import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { IdentityProvider } from '../../entities/configuration/identity-provider';
import { IdentityProviderType } from '../../entities/configuration/identity-provider-type';
import { AuthService } from '../../services/auth.service';
import { ConfigService } from '../../services/config.service';
import { CurrentUserService } from '../../services/current-user.service';
import { IdentityProviderService } from '../../services/identity-provider.service';
import { PortalNavigationItemsService } from '../../services/portal-navigation-items.service';

@Component({
  selector: 'app-log-in',
  imports: [
    MatCardModule,
    MatFormField,
    MatInput,
    MatButtonModule,
    MatLabel,
    ReactiveFormsModule,
    MatError,
    RouterLink,
    OAuthModule,
    AsyncPipe,
    MobileClassDirective,
  ],
  templateUrl: './log-in.component.html',
  styleUrl: './log-in.component.scss',
})
export class LogInComponent {
  logInForm: FormGroup<{ username: FormControl; password: FormControl }> = new FormGroup({
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required]),
  });
  error = signal(200);
  isLocalLoginEnabled = inject(ConfigService).configuration.authentication?.localLogin?.enabled ?? false;
  identityProviders$ = inject(IdentityProviderService)
    .getPortalIdentityProviders()
    .pipe(map(({ data }) => data ?? []));
  private readonly redirectUrl = toSignal(inject(ActivatedRoute).queryParams.pipe(map(params => params['redirectUrl'] || '')));

  constructor(
    private readonly authService: AuthService,
    private readonly currentUserService: CurrentUserService,
    private readonly portalNavigationItemsService: PortalNavigationItemsService,
    private readonly router: Router,
    private readonly destroyRef: DestroyRef,
  ) {}

  logIn() {
    this.authService
      .login(this.logInForm.value.username, this.logInForm.value.password)
      .pipe(
        switchMap(_ => this.currentUserService.loadUser()),
        switchMap(_ => this.portalNavigationItemsService.loadTopNavBarItems()),
        tap(_ => this.router.navigate([this.redirectUrl()])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (err: HttpErrorResponse) => {
          this.error.set(err.status);
        },
      });
  }

  authenticateSSO(provider: IdentityProvider) {
    this.authService.authenticateSSO(provider, this.redirectUrl());
  }

  getProviderLogo(provider: IdentityProvider) {
    const type = provider.type ?? IdentityProviderType.OIDC;
    return `${type.toLowerCase()}.svg`;
  }
}

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
import { DOCUMENT } from '@angular/common';
import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';

import { AuthService } from './auth.service';
import { ConfigService } from './config.service';
import { IdentityProviderService } from './identity-provider.service';
import { DataResponse } from '../entities/common/data-response';
import { IdentityProvider } from '../entities/configuration/identity-provider';

const LOG_IN_ROUTE = '/log-in';

@Injectable({
  providedIn: 'root',
})
export class SignInService {
  private readonly authService = inject(AuthService);
  private readonly configService = inject(ConfigService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly identityProviderService = inject(IdentityProviderService);
  private readonly router = inject(Router);

  private readonly isInProgress = signal(false);

  constructor() {
    this.document.defaultView?.addEventListener('pageshow', () => this.isInProgress.set(false));
  }

  signIn(): void {
    if (this.isInProgress()) {
      return;
    }
    this.isInProgress.set(true);

    if (!this.isShortcutConfigured()) {
      this.openLogInPage();
      return;
    }

    this.identityProviderService
      .getPortalIdentityProviders()
      .pipe(
        catchError(() => of<DataResponse<IdentityProvider>>({})),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(response => {
        const onlyProvider = this.findOnlyProvider(response);
        if (onlyProvider) {
          this.authService.authenticateSSO(onlyProvider, '');
          return;
        }
        this.openLogInPage();
      });
  }

  private isShortcutConfigured(): boolean {
    const configuration = this.configService.configuration;
    return (
      configuration.portalNext?.skipLoginWithSingleIdp?.enabled === true &&
      configuration.authentication?.localLogin?.enabled !== true &&
      configuration.portal?.userCreation?.enabled !== true
    );
  }

  private findOnlyProvider(response: DataResponse<IdentityProvider>): IdentityProvider | undefined {
    return response.metadata?.pagination?.total === 1 ? response.data?.[0] : undefined;
  }

  private openLogInPage(): void {
    this.isInProgress.set(false);
    this.router.navigate([LOG_IN_ROUTE]);
  }
}

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
import { provideHttpClient, withInterceptorsFromDi, withXsrfConfiguration } from '@angular/common/http';
import { ApplicationRef, DoBootstrap, inject, InjectionToken, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';

import { GioMatConfigModule } from '@gravitee/ui-particles-angular';

import { httpInterceptorProviders } from '../../shared/interceptors/http-interceptors';
import { gammaPortalRoutes } from './gamma-portal.routing';
import { GammaRootComponent } from './gamma-root.component';

/** Injection token for the host element where the gamma portal is mounted (provided by mount-gamma.ts). */
export const GAMMA_MOUNT_ELEMENT = new InjectionToken<HTMLElement>('GAMMA_MOUNT_ELEMENT');

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    RouterModule.forRoot(gammaPortalRoutes, {
      useHash: true,
      paramsInheritanceStrategy: 'always',
    }),
    GioMatConfigModule,
    GammaRootComponent,
  ],
  providers: [
    httpInterceptorProviders,
    provideHttpClient(
      withInterceptorsFromDi(),
      withXsrfConfiguration({
        cookieName: 'none',
        headerName: 'none',
      }),
    ),
  ],
})
export class GammaPortalModule implements DoBootstrap {
  private readonly mountElement = inject(GAMMA_MOUNT_ELEMENT, { optional: true });

  ngDoBootstrap(appRef: ApplicationRef): void {
    const hostElement = this.mountElement;
    if (!hostElement) {
      throw new Error('GAMMA_MOUNT_ELEMENT must be provided when bootstrapping GammaPortalModule');
    }
    appRef.bootstrap(GammaRootComponent, hostElement);
  }
}

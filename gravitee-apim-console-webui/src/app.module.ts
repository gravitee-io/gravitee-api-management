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
import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpClientXsrfModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { UpgradeModule } from '@angular/upgrade/static';
import { UIRouterUpgradeModule } from '@uirouter/angular-hybrid';

import { uiRouterStateProvider, uiRouterStateParamsProvider, currentUserProvider, ajsRootScopeProvider } from './ajs-upgraded-providers';
import { ManagementModule } from './management/management.module';
import { OrganizationSettingsModule } from './organization/configuration/organization-settings.module';
import { httpInterceptorProviders } from './shared/interceptors/http-interceptors';
@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    // Explicitly disable automatic csrf handling as it will not work for cross-domain (using custom csrf interceptor).
    HttpClientXsrfModule.withOptions({
      cookieName: 'none',
      headerName: 'none',
    }),
    UpgradeModule,
    UIRouterUpgradeModule,
    OrganizationSettingsModule,
    ManagementModule,
  ],
  providers: [httpInterceptorProviders, uiRouterStateProvider, uiRouterStateParamsProvider, currentUserProvider, ajsRootScopeProvider],
})
export class AppModule {
  constructor(private upgrade: UpgradeModule) {}

  ngDoBootstrap() {
    this.upgrade.bootstrap(document.documentElement, ['gravitee-management'], { strictDi: true });
  }
}

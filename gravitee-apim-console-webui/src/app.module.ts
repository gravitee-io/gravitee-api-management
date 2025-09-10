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
import * as angular from 'angular';

import { CommonModule } from '@angular/common';
import { provideHttpClient, withInterceptorsFromDi, withXsrfConfiguration } from '@angular/common/http';
import { ApplicationRef, DoBootstrap, importProvidersFrom, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { setAngularJSGlobal, UpgradeModule } from '@angular/upgrade/static';
import { GioPendoModule, GIO_PENDO_SETTINGS_TOKEN } from '@gravitee/ui-analytics';
import { GioMatConfigModule } from '@gravitee/ui-particles-angular';
import { MatMomentDateModule, provideMomentDateAdapter } from '@angular/material-moment-adapter';

import { currentUserProvider, ajsScopeProvider } from './ajs-upgraded-providers';
import { Constants } from './entities/Constants';
import { httpInterceptorProviders } from './shared/interceptors/http-interceptors';
import { GioSideNavModule } from './components/gio-side-nav/gio-side-nav.module';
import { GioTopNavModule } from './components/gio-top-nav/gio-top-nav.module';
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { UserComponent } from './user/my-accout/user.component';
import { AuthModule } from './auth/auth.module';
import { GioFormJsonSchemaExtendedModule } from './shared/components/form-json-schema-extended/form-json-schema-extended.module';
import { ClusterRoutingModule } from './management/clusters/cluster-routing.module';

@NgModule({
  declarations: [AppComponent, UserComponent],
  imports: [
    // /!\ This module must be importer only once in the application.
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    UpgradeModule,
    MatMomentDateModule,
    AppRoutingModule,
    ClusterRoutingModule,
    GioPendoModule.forRoot(),
    GioMatConfigModule,
    AuthModule,
    GioSideNavModule,
    GioTopNavModule,
    GioFormJsonSchemaExtendedModule,
  ],
  providers: [
    httpInterceptorProviders,
    currentUserProvider,
    ajsScopeProvider,
    {
      provide: GIO_PENDO_SETTINGS_TOKEN,
      useFactory: (constants: Constants) => {
        return {
          enabled: constants.org?.settings?.analyticsPendo?.enabled ?? false,
          apiKey: constants.org?.settings?.analyticsPendo?.apiKey,
        };
      },
      deps: [Constants],
    },
    {
      provide: 'isFactory',
      useValue: true,
    },
    provideMomentDateAdapter(undefined, { useUtc: true }),
    importProvidersFrom(GioFormJsonSchemaExtendedModule),
    provideHttpClient(
      withInterceptorsFromDi(),
      withXsrfConfiguration({
        cookieName: 'none',
        headerName: 'none',
      }),
    ),
  ],
})
export class AppModule implements DoBootstrap {
  constructor(private upgrade: UpgradeModule) {}

  ngDoBootstrap(app: ApplicationRef) {
    setAngularJSGlobal(angular);
    this.upgrade.bootstrap(document.documentElement, ['gravitee-management'], { strictDi: true });
    app.bootstrap(AppComponent);
  }
}

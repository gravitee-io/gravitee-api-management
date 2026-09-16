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
import { enableProdMode, APP_INITIALIZER, importProvidersFrom } from '@angular/core';
import { provideTranslateService, TranslateLoader, TranslateCompiler } from '@ngx-translate/core';
import { TranslateHttpLoader, TRANSLATE_HTTP_LOADER_CONFIG } from '@ngx-translate/http-loader';
import { TranslateMessageFormatCompiler, MESSAGE_FORMAT_CONFIG } from 'ngx-translate-messageformat-compiler';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';
import { provideAnimations } from '@angular/platform-browser/animations';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';

import { BASE_PATH, ApiModule } from '../projects/portal-webclient-sdk/src/lib';

import { initApp } from './app/app-initializer';
import { environment } from './environments/environment';
import { ConfigurationService } from './app/services/configuration.service';
import { AuthService } from './app/services/auth.service';
import { CurrentUserService } from './app/services/current-user.service';
import { TranslationService } from './app/services/translation.service';
import { ReCaptchaService } from './app/services/recaptcha.service';
import { ApiRequestInterceptor } from './app/interceptors/api-request.interceptor';
import { AppRoutingModule } from './app/app-routing.module';
import { SharedModule } from './app/shared/shared.module';
import { AppComponent } from './app/app.component';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [
    // TranslatePipe and TranslateDirective are standalone declarables, not modules: the components
    // that use them import them directly.
    importProvidersFrom(ApiModule, AppRoutingModule, BrowserModule, SharedModule),
    ...provideTranslateService({
      loader: {
        provide: TranslateLoader,
        useClass: TranslateHttpLoader,
      },
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler,
      },
    }),
    {
      provide: TRANSLATE_HTTP_LOADER_CONFIG,
      // v18 reads `resources` only: a bare prefix makes the loader issue no request at all and
      // return an empty object, and failOnError keeps a missing file from passing unnoticed.
      useValue: { resources: [{ prefix: './assets/i18n/', suffix: '.json' }], failOnError: true },
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initApp,
      deps: [ConfigurationService, AuthService, CurrentUserService, TranslationService, ReCaptchaService],
      multi: true,
    },
    {
      provide: BASE_PATH,
      useFactory: (config: ConfigurationService) => config.get('baseURL'),
      deps: [ConfigurationService],
    },
    { provide: MESSAGE_FORMAT_CONFIG, useValue: { locales: environment.locales } },
    { provide: HTTP_INTERCEPTORS, useClass: ApiRequestInterceptor, multi: true },
    CookieService,
    provideAnimations(),
  ],
}).catch(err => console.error(err));

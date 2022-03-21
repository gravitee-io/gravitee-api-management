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
import '@gravitee/ui-components/wc/gv-spinner';
import { ApiContactComponent } from './pages/api/api-contact/api-contact.component';
import { ApiDocumentationComponent } from './pages/api/api-documentation/api-documentation.component';
import { ApiGeneralComponent } from './pages/api/api-general/api-general.component';
import { ApiModule, BASE_PATH } from '../../projects/portal-webclient-sdk/src/lib';
import { ApiRequestInterceptor } from './interceptors/api-request.interceptor';
import { ApiSubscribeComponent } from './pages/api/api-subscribe/api-subscribe.component';
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { AuthService } from './services/auth.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { CatalogSearchComponent } from './pages/catalog/search/catalog-search.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { ConfigurationService } from './services/configuration.service';
import { CookiesComponent } from './pages/cookies/cookies.component';
import { CookieService } from 'ngx-cookie-service';
import { CurrentUserService } from './services/current-user.service';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { DocumentationComponent } from './pages/documentation/documentation.component';
import { environment } from '../environments/environment';
import { filter } from 'rxjs/operators';
import { FilteredCatalogComponent } from './pages/catalog/filtered-catalog/filtered-catalog.component';
import { GvContactComponent } from './components/gv-contact/gv-contact.component';
import { GvCookieConsentComponent } from './components/gv-cookie-consent/gv-cookie-consent.component';
import { GvDocumentationComponent } from './components/gv-documentation/gv-documentation.component';
import { GvHeaderItemComponent } from './components/gv-header-item/gv-header-item.component';
import { GvMenuRightSlotDirective } from './directives/gv-menu-right-slot.directive';
import { GvMenuRightTransitionSlotDirective } from './directives/gv-menu-right-transition-slot.directive';
import { GvMenuTopSlotDirective } from './directives/gv-menu-top-slot.directive';
import { GvSearchApiComponent } from './components/gv-search-api/gv-search-api.component';
import { HomepageComponent } from './pages/homepage/homepage.component';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { LoginComponent } from './pages/login/login.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { MESSAGE_FORMAT_CONFIG, TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';
import { APP_INITIALIZER, CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { ResetPasswordConfirmationComponent } from './pages/reset-password/reset-password-confirmation/reset-password-confirmation.component';
import { Router, Scroll } from '@angular/router';
import { SharedModule } from './shared/shared.module';
import { SinglePageComponent } from './pages/single-page/single-page.component';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateCompiler, TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslationService } from './services/translation.service';
import { UserAccountComponent } from './pages/user/user-account/user-account.component';
import { UserContactComponent } from './pages/user/user-contact/user-contact.component';
import { UserNotificationComponent } from './pages/user/user-notification/user-notification.component';
import { ViewportScroller } from '@angular/common';
import { ReCaptchaService } from './services/recaptcha.service';
import { TicketsHistoryComponent } from './components/gv-tickets-history/tickets-history.component';

@NgModule({
  declarations: [
    ApiContactComponent,
    ApiDocumentationComponent,
    ApiGeneralComponent,
    ApiSubscribeComponent,
    AppComponent,
    CatalogSearchComponent,
    CatalogSearchComponent,
    CategoriesComponent,
    CookiesComponent,
    DashboardComponent,
    DocumentationComponent,
    FilteredCatalogComponent,
    GvContactComponent,
    GvCookieConsentComponent,
    GvDocumentationComponent,
    GvHeaderItemComponent,
    GvMenuRightSlotDirective,
    GvMenuRightTransitionSlotDirective,
    GvMenuTopSlotDirective,
    GvSearchApiComponent,
    HomepageComponent,
    LoginComponent,
    LogoutComponent,
    NotFoundComponent,
    RegistrationComponent,
    RegistrationConfirmationComponent,
    ResetPasswordComponent,
    ResetPasswordConfirmationComponent,
    SinglePageComponent,
    UserAccountComponent,
    UserContactComponent,
    UserNotificationComponent,
    TicketsHistoryComponent,
  ],
  imports: [
    ApiModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    SharedModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new TranslateHttpLoader(http, './assets/i18n/'),
        deps: [HttpClient],
      },
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler,
      },
    }),
  ],
  providers: [
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
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  bootstrap: [AppComponent],
  exports: [TranslateModule, SharedModule],
})
export class AppModule {
  constructor(router: Router, viewportScroller: ViewportScroller) {
    router.events.pipe(filter((e): e is Scroll => e instanceof Scroll)).subscribe((e) => {
      if (e.position) {
        // backward navigation
        viewportScroller.scrollToPosition(e.position);
      } else if (!e.anchor) {
        // forward navigation
        viewportScroller.scrollToPosition([0, 0]);
      }
    });
  }
}

export function initApp(
  configurationService: ConfigurationService,
  authService: AuthService,
  currentUserService: CurrentUserService,
  translationService: TranslationService,
  reCaptchaService: ReCaptchaService,
) {
  return () =>
    configurationService.load().then(() => {
      return authService
        .load()
        .then(() => currentUserService.load().then(() => translationService.load().then(() => reCaptchaService.load())));
    });
}

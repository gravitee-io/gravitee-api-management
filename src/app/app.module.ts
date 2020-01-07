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
import { AccountComponent } from './pages/account/account.component';
import { ApiContactComponent } from './pages/api/api-contact/api-contact.component';
import { ApiDocumentationComponent } from './pages/api/api-documentation/api-documentation.component';
import { ApiGeneralComponent } from './pages/api/api-general/api-general.component';
import { ApiLabelsPipe } from './pipes/api-labels.pipe';
import { ApiModule, BASE_PATH } from '@gravitee/ng-portal-webclient';
import { ApiRequestInterceptor } from './interceptors/api-request.interceptor';
import { ApiStatesPipe } from './pipes/api-states.pipe';
import { ApiSubscribeComponent } from './pages/api-subscribe/api-subscribe.component';
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { AppsComponent } from './pages/apps/apps.component';
import { BrowserModule } from '@angular/platform-browser';
import { CatalogSearchComponent } from './pages/catalog/search/catalog-search.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { ConfigurationService } from './services/configuration.service';
import { ContactComponent } from './pages/contact/contact.component';
import { CurrentUserService } from './services/current-user.service';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { DocumentationComponent } from './pages/documentation/documentation.component';
import { environment } from '../environments/environment';
import { FilteredCatalogComponent } from './pages/catalog/filtered-catalog/filtered-catalog.component';
import { GvContactComponent } from './components/gv-contact/gv-contact.component';
import { GvDocumentationComponent } from './components/gv-documentation/gv-documentation.component';
import { GvHeaderApiComponent } from './components/gv-header-api/gv-header-api.component';
import { GvMenuRightSlotDirective } from './directives/gv-menu-right-slot.directive';
import { GvMenuTopSlotDirective } from './directives/gv-menu-top-slot.directive';
import { GvPageComponent } from './components/gv-page/gv-page.component';
import { GvPageContentSlotDirective } from './directives/gv-page-content-slot.directive';
import { GvPageMarkdownComponent } from './components/gv-page-markdown/gv-page-markdown.component';
import { GvPageRedocComponent } from './components/gv-page-redoc/gv-page-redoc.component';
import { GvPageSwaggerUIComponent } from './components/gv-page-swaggerui/gv-page-swaggerui.component';
import { GvSearchComponent } from './components/gv-search/gv-search.component';
import { HomepageComponent } from './pages/homepage/homepage.component';
import { HttpClient, HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { LayoutComponent } from './layouts/layout/layout.component';
import { LoginComponent } from './pages/login/login.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { MarkdownModule } from 'ngx-markdown';
import { MESSAGE_FORMAT_CONFIG } from 'ngx-translate-messageformat-compiler';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA, APP_INITIALIZER } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { SafePipe } from './pipes/safe.pipe';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateLoader, TranslateModule, TranslateCompiler } from '@ngx-translate/core';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';
import { UserAvatarComponent } from './components/user-avatar/user-avatar.component';
import { OAuthModule } from 'angular-oauth2-oidc';
import { AuthService } from './services/auth.service';

@NgModule({
  declarations: [
    AccountComponent,
    ApiContactComponent,
    ApiDocumentationComponent,
    ApiGeneralComponent,
    ApiLabelsPipe,
    ApiStatesPipe,
    ApiSubscribeComponent,
    AppComponent,
    AppsComponent,
    CatalogSearchComponent,
    CatalogSearchComponent,
    CategoriesComponent,
    ContactComponent,
    ContactComponent,
    DashboardComponent,
    DocumentationComponent,
    FilteredCatalogComponent,
    GvContactComponent,
    GvDocumentationComponent,
    GvHeaderApiComponent,
    GvMenuRightSlotDirective,
    GvMenuTopSlotDirective,
    GvPageContentSlotDirective,
    GvPageComponent,
    GvSearchComponent,
    GvPageMarkdownComponent,
    GvPageRedocComponent,
    GvPageSwaggerUIComponent,
    HomepageComponent,
    LayoutComponent,
    LoginComponent,
    LogoutComponent,
    RegistrationComponent,
    RegistrationConfirmationComponent,
    SafePipe,
    UserAvatarComponent,
  ],
  entryComponents: [GvSearchComponent, GvHeaderApiComponent, GvPageMarkdownComponent, GvPageRedocComponent, GvPageSwaggerUIComponent],
  imports: [
    ApiModule,
    AppRoutingModule,
    BrowserModule,
    HttpClientModule,
    MarkdownModule.forRoot({ loader: HttpClient }),
    ReactiveFormsModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new TranslateHttpLoader(http),
        deps: [HttpClient]
      },
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler
      }
    }),
    OAuthModule.forRoot(),
  ],
  providers: [
    {
      provide: APP_INITIALIZER,
      useFactory: initApp,
      deps: [ConfigurationService, AuthService, CurrentUserService],
      multi: true
    },
    { provide: BASE_PATH, useFactory: (config: ConfigurationService) => config.get('baseUrl'), deps: [ConfigurationService] },
    { provide: MESSAGE_FORMAT_CONFIG, useValue: { locales: environment.locales } },
    { provide: HTTP_INTERCEPTORS, useClass: ApiRequestInterceptor, multi: true },
    ApiStatesPipe,
    ApiLabelsPipe
  ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule {
}

export function initApp(configurationService: ConfigurationService, authService: AuthService, currentUserService: CurrentUserService) {
  return () => configurationService.load().then(
    () => authService.load().then(() => currentUserService.load())
  );
}

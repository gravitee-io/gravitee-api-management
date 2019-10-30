import {BrowserModule} from '@angular/platform-browser';
import {NgModule, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import {HttpClient, HttpClientModule, HTTP_INTERCEPTORS} from '@angular/common/http';

import {TranslateLoader, TranslateModule, TranslateCompiler} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';
import { MESSAGE_FORMAT_CONFIG } from 'ngx-translate-messageformat-compiler';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {LoginComponent} from './pages/login/login.component';

import {ApiModule, BASE_PATH} from 'ng-portal-webclient/dist';
import {environment} from '../environments/environment';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { AppsComponent } from './pages/apps/apps.component';
import { UserComponent } from './pages/user/user.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { APIRequestInterceptor } from './interceptors/apiRequest.interceptor';
import { CurrentUserService } from './services/currentUser.service';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    CatalogComponent,
    AppsComponent,
    LoginComponent,
    UserComponent,
    LogoutComponent,
    RegistrationComponent,
    RegistrationConfirmationComponent
  ],
  imports: [
    ApiModule,
    AppRoutingModule,
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [HttpClient]
      },
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler
      }
    })
  ],
  providers: [
    {provide: BASE_PATH, useValue: environment.PORTAL_API_BASE_PATH},
    {provide: MESSAGE_FORMAT_CONFIG, useValue: {locales: environment.locales}},
    CurrentUserService,
    {provide: HTTP_INTERCEPTORS, useClass: APIRequestInterceptor, multi: true}
  ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA,
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule {
}

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}

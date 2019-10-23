import {BrowserModule} from '@angular/platform-browser';
import {NgModule, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import {HttpClient, HttpClientModule} from '@angular/common/http';

import {TranslateLoader, TranslateModule, TranslateCompiler} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';
import { MESSAGE_FORMAT_CONFIG } from 'ngx-translate-messageformat-compiler';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {LoginComponent} from './login/login.component';

import {ApiModule, BASE_PATH} from '@gravitee/clients-sdk/dist';
import {environment} from '../environments/environment';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { AppsComponent } from './pages/apps/apps.component';
import { UserComponent } from './user/user.component';
import { LogoutComponent } from './logout/logout.component';
import { CurrentUserService } from './currentUser.service';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    CatalogComponent,
    AppsComponent,
    LoginComponent,
    UserComponent,
    LogoutComponent
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
    CurrentUserService
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

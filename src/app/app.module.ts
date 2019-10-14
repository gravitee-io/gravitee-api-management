import {BrowserModule} from '@angular/platform-browser';
import {NgModule, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { ApiModule, BASE_PATH } from 'ng-portal-webclient/dist';
import { environment } from '../environments/environment';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    ApiModule,
    AppRoutingModule,
    BrowserModule,
    HttpClientModule,
  ],
  providers: [ { provide: BASE_PATH, useValue: environment.PORTAL_API_BASE_PATH }],
  schemas: [
      CUSTOM_ELEMENTS_SCHEMA,
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule {
}

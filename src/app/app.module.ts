import {BrowserModule} from '@angular/platform-browser';
import {NgModule, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

@NgModule({
  declarations: [
    AppComponent,
  ],
  imports: [
    AppRoutingModule,
    BrowserModule,
  ],
  providers: [],
  schemas: [
      CUSTOM_ELEMENTS_SCHEMA,
  ],
  bootstrap: [
    AppComponent,
  ]
})
export class AppModule {
}

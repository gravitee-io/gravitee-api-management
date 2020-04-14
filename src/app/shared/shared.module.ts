import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { OAuthModule } from 'angular-oauth2-oidc';

import { ApiLabelsPipe } from './../pipes/api-labels.pipe';
import { SafePipe } from './../pipes/safe.pipe';
import { ApiStatesPipe } from './../pipes/api-states.pipe';
@NgModule({
  declarations: [
    ApiLabelsPipe,
    ApiStatesPipe,
    SafePipe
  ],
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    OAuthModule.forRoot(),
  ],
  exports: [
    HttpClientModule,
    ReactiveFormsModule,
    OAuthModule
  ],
  providers: [
    ApiLabelsPipe,
    ApiStatesPipe,
  ]
})
export class SharedModule {
}

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
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslateCompiler, TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { OAuthModule } from 'angular-oauth2-oidc';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';

import { ApiLabelsPipe } from './../pipes/api-labels.pipe';
import { SafePipe } from './../pipes/safe.pipe';
import { ApiStatesPipe } from './../pipes/api-states.pipe';
@NgModule({
  declarations: [
    ApiLabelsPipe,
    ApiStatesPipe,
    SafePipe,
  ],
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    OAuthModule.forRoot(),
    TranslateModule.forChild({
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
  ],
  exports: [
    HttpClientModule,
    ReactiveFormsModule,
    OAuthModule,
  ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ],
  providers: [
    ApiLabelsPipe,
    ApiStatesPipe,
  ]
})
export class SharedModule {
}

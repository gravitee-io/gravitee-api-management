import { HttpClient, HttpClientModule } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslateCompiler, TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { OAuthModule } from 'angular-oauth2-oidc';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';
import { GvFileUploadComponent } from '../components/gv-file-upload/gv-file-upload.component';

import { ApiLabelsPipe } from './../pipes/api-labels.pipe';
import { SafePipe } from './../pipes/safe.pipe';
import { ApiStatesPipe } from './../pipes/api-states.pipe';
@NgModule({
  declarations: [
    ApiLabelsPipe,
    ApiStatesPipe,
    SafePipe,
    GvFileUploadComponent,
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
    OAuthModule
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

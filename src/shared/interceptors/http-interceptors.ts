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
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { CsrfInterceptor } from './csrf.interceptor';
import { AccessControlAllowCredentialsInterceptor } from './access-control-allow-credentials.interceptor';
import { ReplaceEnvInterceptor } from './replace-env.interceptor';

/** Http interceptor providers in outside-in order */
export const httpInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useClass: CsrfInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: AccessControlAllowCredentialsInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: ReplaceEnvInterceptor, multi: true },
];

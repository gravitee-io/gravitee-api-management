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
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { Observable } from 'rxjs';
import { GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';

import { ReporterSettingsNativeComponent } from './reporter-settings-native/reporter-settings-native.component';
import { ReporterSettingsProxyComponent } from './reporter-settings-proxy/reporter-settings-proxy.component';
import { ReporterSettingsMessageComponent } from './reporter-settings-message/reporter-settings-message.component';

import { ApiV4 } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { onlyApiV4Filter } from '../../../util/apiFilter.operator';

@Component({
  selector: 'reporter-settings',
  imports: [
    AsyncPipe,
    ReporterSettingsNativeComponent,
    GioCardEmptyStateModule,
    ReporterSettingsProxyComponent,
    ReporterSettingsMessageComponent,
  ],
  templateUrl: './reporter-settings.component.html',
})
export class ReporterSettingsComponent {
  api$: Observable<ApiV4> = this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV4Filter());

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
  ) {}
}

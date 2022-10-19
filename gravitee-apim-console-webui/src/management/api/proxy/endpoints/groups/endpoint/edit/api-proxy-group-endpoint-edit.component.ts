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
import { Component, Inject, OnInit } from '@angular/core';

import { UIRouterStateParams } from '../../../../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-proxy-group-endpoint-edit',
  template: require('./api-proxy-group-endpoint-edit.component.html'),
  styles: [require('./api-proxy-group-endpoint-edit.component.scss')],
})
export class ApiProxyGroupEndpointEditComponent implements OnInit {
  public apiId: string;

  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams) {}

  public ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;
  }
}

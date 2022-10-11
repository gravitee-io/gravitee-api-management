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
import { map, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ProxyGroup } from '../../../../../../entities/proxy';
import { ApiService } from '../../../../../../services-ngx/api.service';

@Component({
  selector: 'api-proxy-group-edit',
  template: require('./api-proxy-group-edit.component.html'),
  styles: [require('./api-proxy-group-edit.component.scss')],
})
export class ApiProxyGroupEditComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private groupName: string;

  public apiId: string;
  public group: ProxyGroup;

  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams, private readonly apiService: ApiService) {}

  ngOnInit() {
    this.apiId = this.ajsStateParams.apiId;
    this.groupName = this.ajsStateParams.groupName;

    this.apiService
      .get(this.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        map((api) => {
          this.group = api.proxy.groups.find((group) => group.name === this.groupName);
        }),
      )
      .subscribe();
  }
}

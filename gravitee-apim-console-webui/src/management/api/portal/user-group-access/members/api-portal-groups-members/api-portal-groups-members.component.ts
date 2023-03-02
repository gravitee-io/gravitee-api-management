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
import { Subject } from 'rxjs';

import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-portal-groups-members',
  template: require('./api-portal-groups-members.component.html'),
  styles: [require('./api-portal-groups-members.component.scss')],
})
export class ApiPortalGroupsMembersComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  private apiId: string;

  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams) {}

  ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }
}

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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { StateParams } from '@uirouter/angularjs';

import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-dynamic-properties',
  template: require('./api-dynamic-properties.component.html'),
  styles: [require('./api-dynamic-properties.component.scss')],
})
export class ApiDynamicPropertiesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();

  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams) {}

  ngOnInit(): void {}

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onSave() {}
}

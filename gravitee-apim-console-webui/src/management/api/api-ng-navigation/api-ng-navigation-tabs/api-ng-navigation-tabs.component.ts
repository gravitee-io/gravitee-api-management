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
import { Component, Inject, Input } from '@angular/core';
import { StateService } from '@uirouter/core';
import { castArray } from 'lodash';

import { MenuItem } from '../api-ng-navigation.component';
import { UIRouterState } from '../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-ng-navigation-tabs',
  template: require('./api-ng-navigation-tabs.component.html'),
  styles: [require('./api-ng-navigation-tabs.component.scss')],
})
export class ApiNgNavigationTabsComponent {
  @Input()
  public tabMenuItems: MenuItem[] = [];

  constructor(@Inject(UIRouterState) private readonly ajsState: StateService) {}

  navigateTo(route: string) {
    this.ajsState.go(route);
  }

  isActive(baseRoute: MenuItem['baseRoute']): boolean {
    return castArray(baseRoute).some((baseRoute) => this.ajsState.includes(baseRoute));
  }
}

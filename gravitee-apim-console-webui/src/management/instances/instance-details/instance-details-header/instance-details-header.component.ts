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

import { Component, Inject, Input, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';

import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { Instance } from '../../../../entities/instance/instance';

interface MenuItem {
  targetRoute?: string;
  baseRoute?: string;
  displayName: string;
  testId?: string;
}

@Component({
  selector: 'instance-details-header',
  template: require('./instance-details-header.component.html'),
  styles: [require('./instance-details-header.component.scss')],
})
export class InstanceDetailsHeaderComponent implements OnInit {
  @Input()
  public instance: Instance;

  public tabMenuItems: MenuItem[] = [];

  constructor(@Inject(UIRouterState) private readonly ajsState: StateService) {}

  ngOnInit(): void {
    this.tabMenuItems = [
      {
        displayName: 'Environment',
        targetRoute: 'management.instance.environment',
        baseRoute: 'management.instance.environment',
        testId: 'instances-detail-environment',
      },
      {
        displayName: 'Monitoring',
        targetRoute: 'management.instance.monitoring',
        baseRoute: 'management.instance.monitoring',
        testId: 'instances-detail-monitoring',
      },
    ];
  }

  navigateTo(route: string) {
    this.ajsState.go(route);
  }

  isActive(route: string): boolean {
    return this.ajsState.includes(route);
  }
}

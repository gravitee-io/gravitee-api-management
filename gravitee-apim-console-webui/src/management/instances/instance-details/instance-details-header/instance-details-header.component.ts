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

import { Component, Input, OnInit } from '@angular/core';

import { Instance } from '../../../../entities/instance/instance';

interface MenuItem {
  displayName: string;
  routerLink: string;
  testId?: string;
}

@Component({
  selector: 'instance-details-header',
  templateUrl: './instance-details-header.component.html',
  styleUrls: ['./instance-details-header.component.scss'],
  standalone: false,
})
export class InstanceDetailsHeaderComponent implements OnInit {
  @Input()
  public instance: Instance;

  public tabMenuItems: MenuItem[] = [];

  ngOnInit(): void {
    this.tabMenuItems = [
      {
        displayName: 'Environment',
        routerLink: 'environment',
        testId: 'instances-detail-environment',
      },
      {
        displayName: 'Monitoring',
        routerLink: 'monitoring',
        testId: 'instances-detail-monitoring',
      },
    ];
  }
}

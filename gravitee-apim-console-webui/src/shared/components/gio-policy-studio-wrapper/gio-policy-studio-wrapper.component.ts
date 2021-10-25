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
import { Location } from '@angular/common';

import '@gravitee/ui-components/wc/gv-policy-studio';

@Component({
  selector: 'gio-policy-studio-wrapper',
  template: require('./gio-policy-studio-wrapper.component.html'),
  styles: [require('./gio-policy-studio-wrapper.component.scss')],
})
export class GioPolicyStudioWrapperComponent implements OnInit {
  @Input()
  canAdd: boolean;

  @Input()
  canDebug: boolean;

  @Input()
  hasResources: boolean;

  @Input()
  hasProperties: boolean;

  @Input()
  hasPolicyFilter: boolean;

  @Input()
  sortable: boolean;

  @Input()
  policies: unknown[];

  @Input()
  definition: unknown;

  @Input()
  services: Record<string, unknown> = {};

  @Input()
  flowSchema: unknown;

  @Input()
  resourceTypes: unknown[];

  @Input()
  propertyProviders: unknown[];

  @Input()
  readonlyPlans: boolean;

  @Input()
  configurationSchema: unknown = {};

  @Input()
  dynamicPropertySchema: unknown = {};

  @Input()
  debugResponse: unknown;

  @Input()
  flowsTitle: string;

  @Input()
  configurationInformation =
    'By default, the selection of a flow is based on the operator defined in the flow itself. This operator allows either to select a flow when the path matches exactly, or when the start of the path matches. The "Best match" option allows you to select the flow from the path that is closest.';

  tabId: string;

  constructor(private readonly location: Location) {}

  ngOnInit(): void {
    const pathParts = this.location.path(false).split('#');
    if (pathParts.length > 1) {
      this.tabId = pathParts[1];
    }
  }

  public onTabChanged(tabName: string): void {
    // TODO: Improve this with Angular Router
    // Hack to add the tab as Fragment part of the URL
    const path = this.location.path(false).split('#')[0];
    this.location.go(`${path}#${tabName}`);
  }
}

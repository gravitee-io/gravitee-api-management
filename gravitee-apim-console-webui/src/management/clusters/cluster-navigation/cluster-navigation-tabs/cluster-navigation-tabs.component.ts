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
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabNavPanel, MatTabsModule } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioIconsModule, GioLicenseModule } from '@gravitee/ui-particles-angular';

import { MenuItem, MenuItemHeader } from '../cluster-navigation.component';

@Component({
  selector: 'cluster-navigation-tabs',
  templateUrl: './cluster-navigation-tabs.component.html',
  styleUrls: ['./cluster-navigation-tabs.component.scss'],
  imports: [CommonModule, MatTabsModule, RouterLink, MatTabNavPanel, RouterLinkActive, MatTooltipModule, GioIconsModule, GioLicenseModule],
})
export class ClusterNavigationTabsComponent {
  @Input()
  public tabMenuItems: MenuItem[] = [];

  @Input()
  public menuItemHeader: MenuItemHeader;
}

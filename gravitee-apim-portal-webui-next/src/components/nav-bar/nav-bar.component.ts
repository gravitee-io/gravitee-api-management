/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, inject, Input, input, InputSignal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

import { User } from '../../entities/user/user';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';
import { PortalMenuLink } from '../../services/portal-menu-links.service';
import { CompanyTitleComponent } from '../company-title/company-title.component';
import { DesktopNavBarComponent } from './desktop-nav-bar/desktop-nav-bar.component';
import { MobileNavBarComponent } from './mobile-nav-bar/mobile-nav-bar.component';

@Component({
  selector: 'app-nav-bar',
  imports: [MatButtonModule, CompanyTitleComponent, DesktopNavBarComponent, MobileNavBarComponent],
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.scss'],
})
export class NavBarComponent {
  @Input()
  siteTitle!: string;

  customLinks: InputSignal<PortalMenuLink[]> = input<PortalMenuLink[]>([]);
  currentUser: InputSignal<User> = input({});
  forceLogin: InputSignal<boolean> = input(false);
  logo: InputSignal<string> = input('');
  protected readonly isMobile = inject(ObservabilityBreakpointService).isMobile;
}

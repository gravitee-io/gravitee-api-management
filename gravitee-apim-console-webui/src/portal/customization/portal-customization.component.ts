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
import { Component, inject } from '@angular/core';
import { GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { MatAnchor } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';
import { RouterModule } from '@angular/router';
import { AsyncPipe } from '@angular/common';

import { PortalNavigationService } from '../navigation/portal-navigation.service';

@Component({
  selector: 'portal-customization',
  standalone: true,
  imports: [MatAnchor, MatIcon, MatTabLink, MatTabNav, MatTabNavPanel, RouterModule, GioSubmenuModule, AsyncPipe],
  templateUrl: './portal-customization.component.html',
  styleUrl: './portal-customization.component.scss',
})
export class PortalCustomizationComponent {
  customizationRoutesGroup = inject(PortalNavigationService).getCustomizationRoutes();
}

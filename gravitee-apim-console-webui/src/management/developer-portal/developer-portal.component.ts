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
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';

import { IntegrationsService } from '../../services-ngx/integrations.service';
import { Constants } from '../../entities/Constants';

@Component({
  selector: 'customization',
  standalone: true,
  imports: [
    GioBannerModule,
    MatButtonModule,
    MatIconModule,
    MatTabLink,
    MatTabNav,
    MatTabNavPanel,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
  ],
  templateUrl: './developer-portal.component.html',
  styleUrl: './developer-portal.component.scss',
})
export class DeveloperPortalComponent implements OnInit {
  portalUrl: string;

  constructor(
    public readonly integrationsService: IntegrationsService,
    @Inject(Constants) public readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.portalUrl = this.constants.env.baseURL.replace('{:envId}', this.constants.org.currentEnv.id) + '/portal/redirect?version=next';
  }
}

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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IntegrationsService } from '../../../../services-ngx/integrations.service';

@Component({
  selector: 'app-integration-user-permissions',
  templateUrl: './integration-user-permissions.component.html',
  styleUrl: './integration-user-permissions.component.scss',
})
export class IntegrationUserPermissionsComponent implements OnInit {
  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.integrationsService.getIntegration(this.activatedRoute.snapshot.params.integrationId).subscribe();
  }
}

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

import { Component, DestroyRef, Inject, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AgentStatus, Integration } from '../integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Constants } from '../../../entities/Constants';
import { IntegrationProviderService } from '../integration-provider.service';

@Component({
  selector: 'app-integration-agent',
  templateUrl: './integration-agent.component.html',
  styleUrl: './integration-agent.component.scss',
  standalone: false,
})
export class IntegrationAgentComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading: boolean = true;
  public integration: Integration;
  public documentationUrl: string = '';

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
    @Inject(Constants) private readonly constants: Constants,
    private readonly integrationProviderService: IntegrationProviderService,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
  }

  private getIntegration(): void {
    this.isLoading = true;
    const { integrationId } = this.activatedRoute.snapshot.params;
    this.integrationsService
      .getIntegration(integrationId)
      .pipe(
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((integration: Integration): void => {
        this.integration = integration;
        this.isLoading = false;
        this.documentationUrl = this.getDocumentationUrl(integration.provider);
      });
  }

  public refreshStatus(): void {
    this.getIntegration();
  }

  private getDocumentationUrl(providerName: string): string {
    const buildVersion = this.constants.build.version;
    const majorMinorVersion = this.extractMajorMinor(buildVersion);
    const apimDocs3rdPartyProviderName = this.integrationProviderService.getApimDocsNameByValue(providerName);

    if (!majorMinorVersion || !apimDocs3rdPartyProviderName) {
      return '';
    }

    return `https://documentation.gravitee.io/apim/${majorMinorVersion}/governance/federation/3rd-party-providers/${apimDocs3rdPartyProviderName}`;
  }

  private extractMajorMinor(version: string): string {
    const match = version.match(/^(\d+)\.(\d+)/);
    return match ? `${match[1]}.${match[2]}` : '';
  }

  protected readonly AgentStatus = AgentStatus;
}

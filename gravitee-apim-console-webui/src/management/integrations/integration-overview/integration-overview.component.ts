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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { Integration } from '../integrations.model';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-integration-overview',
  templateUrl: './integration-overview.component.html',
  styleUrls: ['./integration-overview.component.scss'],
})
export class IntegrationOverviewComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public integration: Integration;

  constructor(
    private route: ActivatedRoute,
    private integrationsService: IntegrationsService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
  }

  private getIntegration(): void {
    const id: string = this.route.snapshot.paramMap.get('integrationId');
    this.integrationsService
      .getIntegration(id)
      .pipe(
        catchError((_) => {
          this.snackBarService.error('Something went wrong!');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((integration: Integration): void => {
        this.integration = integration;
      });
  }
}

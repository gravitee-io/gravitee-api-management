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
import { ActivatedRoute, Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { IntegrationPreview, IntegrationPreviewApisState } from '../integrations.model';

@Component({
  selector: 'app-discovery-preview',
  templateUrl: './discovery-preview.component.html',
  styleUrls: ['./discovery-preview.component.scss'],
})
export class DiscoveryPreviewComponent implements OnInit {
  IntegrationPreviewApisState = IntegrationPreviewApisState;
  private destroyRef: DestroyRef = inject(DestroyRef);

  public displayedColumns = ['name', 'state'];
  public isLoadingPreview = true;
  public integrationPreview: IntegrationPreview = null;

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
    this.getPreview();
  }

  private getIntegration(): void {
    const { integrationId } = this.activatedRoute.snapshot.params;

    this.integrationsService
      .getIntegration(integrationId)
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getPreview(): void {
    const { integrationId } = this.activatedRoute.snapshot.params;

    this.integrationsService
      .previewIntegration(integrationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (integrationPreview) => {
          this.integrationPreview = integrationPreview;
          this.isLoadingPreview = false;
        },
      });
  }

  public proceedIngest() {
    this.integrationsService.setIsIngestToRun(true);
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }
}

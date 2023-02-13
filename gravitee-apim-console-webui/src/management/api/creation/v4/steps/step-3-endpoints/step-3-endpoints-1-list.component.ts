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

import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { Step3Endpoints2ConfigComponent } from './step-3-endpoints-2-config.component';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { EndpointService } from '../../../../../../services-ngx/endpoint.service';
import { ConnectorVM } from '../../models/ConnectorVM';

@Component({
  selector: 'step-3-endpoints-1-list',
  template: require('./step-3-endpoints-1-list.component.html'),
  styles: [require('../api-creation-steps-common.component.scss')],
})
export class Step3Endpoints1ListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: FormGroup;

  public endpoints: ConnectorVM[];

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly endpointService: EndpointService,
    private readonly matDialog: MatDialog,
    private readonly stepService: ApiCreationStepService,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.formGroup = this.formBuilder.group({
      selectedEndpointsIds: this.formBuilder.control(
        (currentStepPayload.selectedEndpoints ?? []).map((p) => p.id),
        [Validators.required],
      ),
    });

    this.endpointService
      .v4ListEndpointPlugins()
      .pipe(
        takeUntil(this.unsubscribe$),
        map((endpointPlugins) =>
          endpointPlugins
            .filter((endpoint) => endpoint.supportedApiType === 'async')
            .sort((endpoint1, endpoint2) => {
              const name1 = endpoint1.name.toUpperCase();
              const name2 = endpoint2.name.toUpperCase();
              return name1 < name2 ? -1 : name1 > name2 ? 1 : 0;
            }),
        ),
      )
      .subscribe((endpointPlugins) => {
        this.endpoints = endpointPlugins.map((endpoint) => ({
          id: endpoint.id,
          name: endpoint.name,
          description: endpoint.description,
          isEnterprise: endpoint.id.endsWith('-advanced'),
          supportedListenerType: endpoint.supportedListenerType,
        }));

        this.changeDetectorRef.detectChanges();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save(): void {
    const selectedEndpointsIds = this.formGroup.getRawValue().selectedEndpointsIds ?? [];
    const selectedEndpoints = this.endpoints.map(({ id, name }) => ({ id, name })).filter((e) => selectedEndpointsIds.includes(e.id));

    this.stepService.addSecondaryStep({
      component: Step3Endpoints2ConfigComponent,
    });

    this.stepService.validStepAndGoNext((previousPayload) => ({
      ...previousPayload,
      selectedEndpoints: selectedEndpoints,
    }));
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }

  onMoreInfoClick(event, endpoint: ConnectorVM) {
    event.stopPropagation();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: endpoint.name,
          content: `${endpoint.description} <br> 🚧 More information coming soon 🚧`,
          confirmButton: `Ok`,
        },
        role: 'alertdialog',
        id: 'moreInfoDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }
}

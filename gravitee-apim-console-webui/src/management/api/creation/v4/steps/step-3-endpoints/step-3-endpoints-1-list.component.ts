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
import { of, Subject } from 'rxjs';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { Step3Endpoints2ConfigComponent } from './step-3-endpoints-2-config.component';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ConnectorPluginsV2Service } from '../../../../../../services-ngx/connector-plugins-v2.service';
import { ConnectorVM } from '../../models/ConnectorVM';
import {
  GioConnectorDialogComponent,
  GioConnectorDialogData,
} from '../../../../../../components/gio-connector-dialog/gio-connector-dialog.component';
import { IconService } from '../../../../../../services-ngx/icon.service';

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
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly matDialog: MatDialog,
    private readonly confirmDialog: MatDialog,
    private readonly stepService: ApiCreationStepService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly iconService: IconService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.formGroup = this.formBuilder.group({
      selectedEndpointsIds: this.formBuilder.control(
        (currentStepPayload.selectedEndpoints ?? []).map((p) => p.id),
        [Validators.required],
      ),
    });

    this.connectorPluginsV2Service
      .listEndpointPlugins()
      .pipe(
        takeUntil(this.unsubscribe$),
        map((endpointPlugins) =>
          endpointPlugins
            .filter((endpoint) => endpoint.supportedApiType === 'MESSAGE')
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
          icon: this.iconService.registerSvg(endpoint.id, endpoint.icon),
          deployed: endpoint.deployed,
        }));

        this.changeDetectorRef.detectChanges();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save() {
    const previousSelection = this.stepService.payload?.selectedEndpoints?.map((e) => e.id);
    const newSelection = this.formGroup.value.selectedEndpointsIds;

    if (previousSelection && !isEqual(newSelection, previousSelection)) {
      // When changing the entrypoint selection, all previously filled steps will be marked as invalid to force user to review the whole configuration.
      return this.confirmDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Are you sure?',
            content:
              'Changing the endpoints list has impact on all following configuration steps. You will have to review all previously entered data.',
            confirmButton: 'Validate',
            cancelButton: 'Discard',
          },
        })
        .afterClosed()
        .subscribe((confirmed) => {
          if (confirmed) {
            this.stepService.invalidateAllNextSteps();
            this.saveChanges();
          }
        });
    }
    return this.saveChanges();
  }

  private saveChanges(): void {
    const selectedEndpointsIds = this.formGroup.getRawValue().selectedEndpointsIds ?? [];
    const selectedEndpoints = this.endpoints
      .map(({ id, name, icon, deployed }) => ({ id, name, icon, deployed }))
      .filter((e) => selectedEndpointsIds.includes(e.id));

    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      selectedEndpoints,
    }));

    this.stepService.goToNextStep({ groupNumber: 3, component: Step3Endpoints2ConfigComponent });
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }

  onMoreInfoClick(event, endpoint: ConnectorVM) {
    event.stopPropagation();
    this.connectorPluginsV2Service
      .getEndpointPluginMoreInformation(endpoint.id)
      .pipe(
        takeUntil(this.unsubscribe$),
        catchError(() => of({})),
        tap((pluginMoreInformation) => {
          this.matDialog
            .open<GioConnectorDialogComponent, GioConnectorDialogData, boolean>(GioConnectorDialogComponent, {
              data: {
                name: endpoint.name,
                pluginMoreInformation,
              },
              role: 'alertdialog',
              id: 'moreInfoDialog',
            })
            .afterClosed()
            .pipe(takeUntil(this.unsubscribe$))
            .subscribe();
        }),
      )
      .subscribe();
  }
}

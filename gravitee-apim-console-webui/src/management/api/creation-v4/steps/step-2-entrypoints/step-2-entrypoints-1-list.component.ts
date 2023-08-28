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
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { Step2Entrypoints2ConfigComponent } from './step-2-entrypoints-2-config.component';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ConnectorVM, fromConnector } from '../../../../../entities/management-api-v2';
import { IconService } from '../../../../../services-ngx/icon.service';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';

@Component({
  selector: 'step-2-entrypoints-1-list',
  template: require('./step-2-entrypoints-1-list.component.html'),
  styles: [require('./step-2-entrypoints-1-list.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step2Entrypoints1ListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: FormGroup;

  public entrypoints: ConnectorVM[];

  public shouldUpgrade = false;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly confirmDialog: MatDialog,
    private readonly stepService: ApiCreationStepService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly iconService: IconService,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.formGroup = this.formBuilder.group({
      selectedEntrypointsIds: this.formBuilder.control(
        (currentStepPayload.selectedEntrypoints ?? []).map((p) => p.id),
        [Validators.required],
      ),
    });

    this.connectorPluginsV2Service
      .listAsyncEntrypointPlugins()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((entrypointPlugins) => {
        this.entrypoints = entrypointPlugins
          .map((entrypoint) => fromConnector(this.iconService, entrypoint))
          .sort((entrypoint1, entrypoint2) => {
            const name1 = entrypoint1.name.toUpperCase();
            const name2 = entrypoint2.name.toUpperCase();
            return name1 < name2 ? -1 : name1 > name2 ? 1 : 0;
          });
        this.shouldUpgrade = this.entrypoints.some((entrypoint) => !entrypoint.deployed);
        this.changeDetectorRef.detectChanges();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save() {
    const previousSelection = this.stepService.payload?.selectedEntrypoints?.map((e) => e.id);
    const newSelection = this.formGroup.value.selectedEntrypointsIds;

    if (previousSelection && !isEqual(newSelection, previousSelection)) {
      // When changing the entrypoint selection, all previously filled steps will be marked as invalid to force user to review the whole configuration.
      return this.confirmDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Are you sure?',
            content:
              'Changing the entrypoints list has impact on all following configuration steps. You will have to review all previously entered data.',
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

  goBack(): void {
    this.stepService.goToPreviousStep();
  }

  private saveChanges() {
    const selectedEntrypointsIds = this.formGroup.getRawValue().selectedEntrypointsIds ?? [];
    const selectedEntrypoints = this.entrypoints
      .map(({ id, name, supportedListenerType, supportedQos, icon, deployed }) => ({
        id,
        name,
        supportedListenerType,
        supportedQos,
        icon,
        deployed,
      }))
      .filter((e) => selectedEntrypointsIds.includes(e.id));

    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      selectedEntrypoints,
    }));

    return this.stepService.goToNextStep({
      groupNumber: 2,
      component: Step2Entrypoints2ConfigComponent,
    });
  }

  public onRequestUpgrade($event: MouseEvent) {
    this.licenseService.openDialog(
      { feature: ApimFeature.APIM_EN_MESSAGE_REACTOR, context: UTMTags.API_CREATION_MESSAGE_ENTRYPOINT },
      $event,
    );
  }
}

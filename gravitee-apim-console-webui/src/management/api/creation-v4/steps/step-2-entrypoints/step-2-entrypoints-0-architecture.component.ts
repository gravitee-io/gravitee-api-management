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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { Step2Entrypoints2ConfigComponent } from './step-2-entrypoints-2-config.component';
import { Step2Entrypoints1ListComponent } from './step-2-entrypoints-1-list.component';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import { ApiType, ConnectorPlugin } from '../../../../../entities/management-api-v2';
import { GioLicenseService } from '../../../../../shared/components/gio-license/gio-license.service';
import { GioLicenseDialog } from '../../../../../shared/components/gio-license/gio-license.dialog';

@Component({
  selector: 'step-2-entrypoints-0-architecture',
  template: require('./step-2-entrypoints-0-architecture.component.html'),
  styles: [require('./step-2-entrypoints-0-architecture.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step2Entrypoints0ArchitectureComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  form: FormGroup;
  private httpProxyEntrypoint: ConnectorPlugin;

  private initialValue: { type: ApiType[] };
  private hasLicense: boolean;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly stepService: ApiCreationStepService,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly confirmDialog: MatDialog,
    private readonly matDialog: MatDialog,
    private readonly iconService: IconService,
    private readonly licenseService: GioLicenseService,
    private readonly licenseDialog: GioLicenseDialog,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.connectorPluginsV2Service
      .listSyncEntrypointPlugins()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((entrypoints) => {
        this.httpProxyEntrypoint = entrypoints.find((e) => e.id === 'http-proxy');

        this.form = this.formBuilder.group({
          type: this.formBuilder.control(currentStepPayload.type ? [currentStepPayload.type] : null, [Validators.required]),
        });

        this.initialValue = this.form.getRawValue();
      });

    this.licenseService.hasLicense().then((hasLicense) => (this.hasLicense = hasLicense));
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goBack() {
    this.stepService.goToPreviousStep();
  }

  save() {
    const previousType = this.stepService.payload.type;
    const selectedType = this.form.value.type[0];

    if (previousType && selectedType !== previousType) {
      // When changing the type, all previously filled steps must be deleted to restart from scratch.
      this.confirmDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Are you sure?',
            content:
              'By changing the architecture type, all previously entered data in Endpoints, Security and Document steps will be reset.',
            confirmButton: 'Validate',
            cancelButton: 'Discard',
          },
        })
        .afterClosed()
        .subscribe((confirmed) => {
          if (confirmed) {
            this.stepService.removeAllNextSteps();
            this.form.value.type[0] === 'PROXY' ? this.doSaveSync() : this.doSaveAsync();
          } else {
            this.form.setValue(this.initialValue);
          }
        });
      return;
    }
    this.form.value.type[0] === 'PROXY' ? this.doSaveSync() : this.doSaveAsync();
  }

  private doSaveSync() {
    this.connectorPluginsV2Service
      .getEndpointPlugin('http-proxy')
      .pipe(
        tap((httpProxyEndpoint) => {
          this.stepService.validStep((previousPayload) => ({
            ...previousPayload,
            type: 'PROXY',
            selectedEntrypoints: [
              {
                id: this.httpProxyEntrypoint.id,
                name: this.httpProxyEntrypoint.name,
                icon: this.iconService.registerSvg(this.httpProxyEntrypoint.id, this.httpProxyEntrypoint.icon),
                supportedListenerType: this.httpProxyEntrypoint.supportedListenerType,
                deployed: this.httpProxyEntrypoint.deployed,
              },
            ],
            selectedEndpoints: [
              {
                id: httpProxyEndpoint.id,
                name: httpProxyEndpoint.name,
                icon: this.iconService.registerSvg(httpProxyEndpoint.id, httpProxyEndpoint.icon),
                deployed: httpProxyEndpoint.deployed,
              },
            ],
          }));
          this.stepService.goToNextStep({
            groupNumber: 2,
            component: Step2Entrypoints2ConfigComponent,
          });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private doSaveAsync() {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      type: 'MESSAGE',
    }));
    this.stepService.goToNextStep({
      groupNumber: 2,
      component: Step2Entrypoints1ListComponent,
    });
  }
}

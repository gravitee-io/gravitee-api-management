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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Observable, Subject } from 'rxjs';
import { map, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService, License } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { Step2Entrypoints2ConfigComponent } from './step-2-entrypoints-2-config.component';

import { AGENT_TO_AGENT } from '../../../../../entities/management-api-v2/api/v4/agentToAgent';
import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ApiType, ConnectorPlugin, ConnectorVM, fromConnector } from '../../../../../entities/management-api-v2';
import { IconService } from '../../../../../services-ngx/icon.service';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';
import { MCP_ENTRYPOINT_ID } from '../../../../../entities/entrypoint/mcp';
import { ApiCreationPayload } from '../../models/ApiCreationPayload';

@Component({
  selector: 'step-2-entrypoints-1-list',
  templateUrl: './step-2-entrypoints-1-list.component.html',
  styleUrls: ['./step-2-entrypoints-1-list.component.scss', '../api-creation-steps-common.component.scss'],
  standalone: false,
})
export class Step2Entrypoints1ListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: UntypedFormGroup;

  public entrypoints: ConnectorVM[];

  public shouldUpgrade = false;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;
  public architecture: ApiCreationPayload['architecture'];

  constructor(
    private readonly formBuilder: UntypedFormBuilder,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly matDialog: MatDialog,
    private readonly stepService: ApiCreationStepService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly iconService: IconService,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;
    const currentSelectedEntrypointIds = (currentStepPayload.selectedEntrypoints ?? []).map(p => p.id);
    this.architecture = currentStepPayload.architecture;
    this.license$ = this.licenseService.getLicense$();
    this.isOEM$ = this.licenseService.isOEM$();

    this.formGroup = this.formBuilder.group({
      selectedEntrypointsIds: this.formBuilder.control(currentSelectedEntrypointIds, [Validators.required]),
    });

    const listEntrypointPlugins: Record<ApiCreationPayload['architecture'], Observable<ConnectorPlugin[]>> = {
      MESSAGE: this.connectorPluginsV2Service.listAsyncEntrypointPlugins(),
      PROXY: this.connectorPluginsV2Service.listSyncEntrypointPlugins(),
      AI: this.connectorPluginsV2Service.listAIEntrypointPlugins(),
      // Not happening for Kafka, this step are skipped
      KAFKA: EMPTY,
    };

    const connectorPlugins$: Observable<ConnectorPlugin[]> = listEntrypointPlugins[currentStepPayload.architecture].pipe(
      map(plugins => {
        if (currentStepPayload.architecture === 'PROXY') {
          // For PROXY, we filter out the MCP entrypoint plugin. MCP is manageable after the API creation.
          return plugins.filter(p => p.id !== MCP_ENTRYPOINT_ID);
        }
        if (currentStepPayload.architecture === 'MESSAGE') {
          // For MESSAGE, we filter out the AGENT_TO_AGENT entrypoint plugin. A2A is manageable with AI architecture only.
          return plugins.filter(p => p.id !== AGENT_TO_AGENT.id);
        }
        return plugins;
      }),
    );

    connectorPlugins$.pipe(takeUntil(this.unsubscribe$)).subscribe(entrypointPlugins => {
      this.entrypoints = entrypointPlugins
        .map(entrypoint => fromConnector(this.iconService, entrypoint))
        .sort((entrypoint1, entrypoint2) => {
          const name1 = entrypoint1.name.toUpperCase();
          const name2 = entrypoint2.name.toUpperCase();
          return name1 < name2 ? -1 : name1 > name2 ? 1 : 0;
        });
      this.shouldUpgrade = this.connectorPluginsV2Service.selectedPluginsNotAvailable(currentSelectedEntrypointIds, this.entrypoints);

      this.changeDetectorRef.detectChanges();
    });

    this.formGroup
      .get('selectedEntrypointsIds')
      .valueChanges.pipe(
        tap(selectedEntrypointsIds => {
          this.shouldUpgrade = this.connectorPluginsV2Service.selectedPluginsNotAvailable(selectedEntrypointsIds, this.entrypoints);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save() {
    const previousSelection = this.stepService.payload?.selectedEntrypoints?.map(e => e.id);
    const newSelection = this.formGroup.value.selectedEntrypointsIds;

    if (previousSelection && !isEqual(newSelection, previousSelection)) {
      // When changing the entrypoint selection, all previously filled steps will be marked as invalid to force user to review the whole configuration.
      return this.matDialog
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
        .subscribe(confirmed => {
          if (confirmed) {
            this.stepService.invalidateAllNextSteps();
            this.saveChanges();
          } else {
            this.formGroup.setValue({ selectedEntrypointsIds: previousSelection });
          }
        });
    }
    this.saveChanges();
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }

  private saveChanges() {
    switch (this.architecture) {
      case 'PROXY':
        this.doSaveSync();
        break;
      case 'MESSAGE':
        this.doSaveAsync();
        break;
      case 'AI':
        this.doSaveAI();
        break;
    }
  }

  private doSaveSync() {
    const selectedEntrypointId = this.formGroup.getRawValue().selectedEntrypointsIds[0];
    const selectedEntrypoint = this.entrypoints.find(e => selectedEntrypointId.includes(e.id));

    // pre-select the endpoint associated to current proxy entrypoint
    this.connectorPluginsV2Service
      .getEndpointPlugin(selectedEntrypoint.id)
      .pipe(
        tap(proxyEndpoint => {
          this.stepService.validStep(previousPayload => ({
            ...previousPayload,
            type: 'PROXY',
            selectedEntrypoints: [
              {
                id: selectedEntrypoint.id,
                name: selectedEntrypoint.name,
                icon: this.iconService.registerSvg(proxyEndpoint.id, selectedEntrypoint.icon),
                supportedListenerType: selectedEntrypoint.supportedListenerType,
                deployed: selectedEntrypoint.deployed,
              },
            ],
            selectedEndpoints: [
              {
                id: proxyEndpoint.id,
                name: proxyEndpoint.name,
                icon: this.iconService.registerSvg(proxyEndpoint.id, proxyEndpoint.icon),
                deployed: proxyEndpoint.deployed,
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
      .filter(e => selectedEntrypointsIds.includes(e.id));

    this.stepService.validStep(previousPayload => ({
      ...previousPayload,
      type: 'MESSAGE',
      selectedEntrypoints,
    }));

    return this.stepService.goToNextStep({
      groupNumber: 2,
      component: Step2Entrypoints2ConfigComponent,
    });
  }

  private doSaveAI() {
    const selectedEntrypointsId = this.formGroup.getRawValue().selectedEntrypointsIds[0];
    const selectedEntrypoint = this.entrypoints.find(e => selectedEntrypointsId === e.id);

    const apiType: ApiType = selectedEntrypoint.id === AGENT_TO_AGENT.id ? 'MESSAGE' : selectedEntrypoint.supportedApiType;

    this.connectorPluginsV2Service
      .getEndpointPlugin(selectedEntrypoint.id)
      .pipe(
        tap(proxyEndpoint => {
          this.stepService.validStep(previousPayload => ({
            ...previousPayload,
            type: apiType,
            selectedEntrypoints: [
              {
                id: selectedEntrypoint.id,
                name: selectedEntrypoint.name,
                icon: this.iconService.registerSvg(proxyEndpoint.id, selectedEntrypoint.icon),
                supportedListenerType: selectedEntrypoint.supportedListenerType,
                deployed: selectedEntrypoint.deployed,
                selectedQos: 'NONE',
              },
            ],
            selectedEndpoints: [
              {
                id: proxyEndpoint.id,
                name: proxyEndpoint.name,
                icon: this.iconService.registerSvg(proxyEndpoint.id, proxyEndpoint.icon),
                deployed: proxyEndpoint.deployed,
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

  public onRequestUpgrade() {
    const selectedEntrypointsId = this.formGroup.getRawValue().selectedEntrypointsIds[0];
    const selectedEntrypoint = this.entrypoints.find(e => selectedEntrypointsId === e.id);

    if (selectedEntrypoint.supportedApiType === 'LLM_PROXY') {
      this.licenseService.openDialog({
        feature: ApimFeature.APIM_LLM_PROXY_REACTOR,
        context: UTMTags.API_CREATION_LLM_ENTRYPOINT,
      });
    } else {
      this.licenseService.openDialog({
        feature: ApimFeature.APIM_EN_MESSAGE_REACTOR,
        context: UTMTags.API_CREATION_MESSAGE_ENTRYPOINT,
      });
    }
  }
}

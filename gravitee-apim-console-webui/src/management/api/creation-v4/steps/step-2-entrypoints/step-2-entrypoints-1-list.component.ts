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
import {combineLatest, Observable, Subject} from 'rxjs';
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
import { LLM_PROXY, MCP_PROXY } from '../../../../../entities/management-api-v2/api/v4/aiTypes';

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
  public apiType: ApiType;

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
    const currentSelectedEntrypointIds = (currentStepPayload.selectedEntrypoints ?? []).map((p) => p.id);
    this.apiType = currentStepPayload.type;
    this.license$ = this.licenseService.getLicense$();
    this.isOEM$ = this.licenseService.isOEM$();

    this.formGroup = this.formBuilder.group({
      selectedEntrypointsIds: this.formBuilder.control(currentSelectedEntrypointIds, [Validators.required]),
    });

    const connectorPlugins$: Observable<ConnectorPlugin[]> = (
      currentStepPayload.isAISelected
        ? this.connectorPluginsV2Service.listAIEntrypointPlugins()
        : currentStepPayload.type === 'MESSAGE'
          ? this.connectorPluginsV2Service.listAsyncEntrypointPlugins()
          : this.connectorPluginsV2Service.listSyncEntrypointPlugins()
    ).pipe(
      map((plugins) => {
        // For PROXY, we filter out the MCP entrypoint plugin. MCP is manageable after the API creation.
        plugins = plugins.filter((p) => p.id !== MCP_ENTRYPOINT_ID);
        const allowedIds = [AGENT_TO_AGENT.id, LLM_PROXY.id, MCP_PROXY.id];
        return currentStepPayload.isAISelected
          ? plugins.filter((p) =>  allowedIds.includes(p.id))
          : plugins.filter((p) => !allowedIds.includes(p.id));
      }),
    );

    connectorPlugins$.pipe(takeUntil(this.unsubscribe$)).subscribe((entrypointPlugins) => {
      this.entrypoints = entrypointPlugins
        .map((entrypoint) => fromConnector(this.iconService, entrypoint))
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
        tap((selectedEntrypointsIds) => {
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
    const previousSelection = this.stepService.payload?.selectedEntrypoints?.map((e) => e.id);
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
        .subscribe((confirmed) => {
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
    this.isAISelected() ? this.doSaveAI():
      this.apiType === 'PROXY' ? this.doSaveSync() : this.doSaveAsync();
  }

  private doSaveSync() {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      type: 'PROXY',
    }));
    this.stepService.goToNextStep({
      groupNumber: 2,
      component: Step2Entrypoints1ListComponent,
    });
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
  private doSaveAI() {
    const selectedEntrypointsIds = this.formGroup.getRawValue().selectedEntrypointsIds ?? [];

    if(selectedEntrypointsIds.includes(AGENT_TO_AGENT.id)) {
      combineLatest([
        this.connectorPluginsV2Service.getEntrypointPlugin(AGENT_TO_AGENT.id),
        this.connectorPluginsV2Service.getEndpointPlugin(AGENT_TO_AGENT.id),
      ])
        .pipe(
          tap(([agentToAgentEntrypoint, agentToAgentEndpoint]) => {
            this.stepService.validStep((previousPayload) => ({
              ...previousPayload,
              selectedEntrypoints: [
                {
                  id: agentToAgentEntrypoint.id,
                  name: agentToAgentEntrypoint.name,
                  icon: this.iconService.registerSvg(agentToAgentEntrypoint.id, agentToAgentEntrypoint.icon),
                  supportedListenerType: agentToAgentEntrypoint.supportedListenerType,
                  deployed: agentToAgentEntrypoint.deployed,
                  selectedQos: 'NONE',
                },
              ],
              selectedEndpoints: [
                {
                  id: agentToAgentEndpoint.id,
                  name: agentToAgentEndpoint.name,
                  icon: this.iconService.registerSvg(agentToAgentEndpoint.id, agentToAgentEndpoint.icon),
                  supportedListenerType: agentToAgentEndpoint.supportedListenerType,
                  deployed: agentToAgentEndpoint.deployed,
                },
              ],
              type: 'MESSAGE', // We save the A2A or the Agent proxy as  MESSAGE only
              isA2ASelected: true,
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
    else {
      const selectedEntrypoints = this.entrypoints
        .map(({id, name, supportedListenerType, supportedQos, icon, deployed}) => ({
          id,
          name,
          supportedListenerType,
          supportedQos,
          icon,
          deployed,
        }))
        .filter((e) => {

          return selectedEntrypointsIds.includes(e.id);
        });
      this.connectorPluginsV2Service
        .getEndpointPlugin(selectedEntrypoints[0].id)
        .pipe(
          tap((proxyEndpoint) => {
            this.stepService.validStep((previousPayload) => ({
              ...previousPayload,
              selectedEntrypoints,
              type: proxyEndpoint.supportedApiType,
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
  }

  isAISelected() {
    return this.stepService.payload.isAISelected;
  }
  public onRequestUpgrade() {
    this.licenseService.openDialog({ feature: ApimFeature.APIM_EN_MESSAGE_REACTOR, context: UTMTags.API_CREATION_MESSAGE_ENTRYPOINT });
  }
}

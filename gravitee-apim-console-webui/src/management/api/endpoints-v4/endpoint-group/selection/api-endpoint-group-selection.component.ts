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
import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';
import { UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GioLicenseService, License } from '@gravitee/ui-particles-angular';

import { AGENT_TO_AGENT } from '../../../../../entities/management-api-v2/api/v4/agentToAgent';
import { ApiType, ConnectorVM, mapAndFilterBySupportedQos, Qos } from '../../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import {
  GioInformationDialogComponent,
  GioConnectorDialogData,
} from '../../../component/gio-information-dialog/gio-information-dialog.component';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';

@Component({
  selector: 'api-endpoints-group-selection',
  templateUrl: './api-endpoint-group-selection.component.html',
  styleUrls: ['./api-endpoint-group-selection.component.scss'],
  standalone: false,
})
export class ApiEndpointGroupSelectionComponent implements OnInit {
  private unsubscribe$: Subject<void> = new Subject<void>();

  @Input()
  public endpointGroupTypeForm: UntypedFormGroup;

  @Input()
  public apiType: ApiType;

  public endpoints: ConnectorVM[];
  @Input() requiredQos!: Qos[];

  public shouldUpgrade = false;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;

  constructor(
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly licenseService: GioLicenseService,
    private readonly iconService: IconService,
    private readonly matDialog: MatDialog,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.license$ = this.licenseService.getLicense$();
    this.isOEM$ = this.licenseService.isOEM$();
    this.connectorPluginsV2Service
      .listEndpointPluginsByApiType(this.apiType)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(endpointPlugins => {
        this.endpoints = mapAndFilterBySupportedQos(endpointPlugins, this.requiredQos, this.iconService);
        this.endpoints = this.endpoints.filter(endpoint => endpoint.id !== AGENT_TO_AGENT.id);
        this.cdr.detectChanges();
      });
  }

  onMoreInfoClick(event, endpoint: ConnectorVM) {
    event.stopPropagation();
    this.connectorPluginsV2Service
      .getEndpointPluginMoreInformation(endpoint.id)
      .pipe(
        catchError(() => of({})),
        switchMap(pluginMoreInformation =>
          this.matDialog
            .open<GioInformationDialogComponent, GioConnectorDialogData, boolean>(GioInformationDialogComponent, {
              data: {
                name: endpoint.name,
                information: pluginMoreInformation,
              },
              role: 'alertdialog',
              id: 'moreInfoDialog',
            })
            .afterClosed(),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public checkLicense(endpoint: ConnectorVM) {
    this.shouldUpgrade = !endpoint.deployed;
  }

  public onRequestUpgrade() {
    this.licenseService.openDialog({
      feature: ApimFeature.APIM_EN_MESSAGE_REACTOR,
      context: UTMTags.GENERAL_ENDPOINT_CONFIG,
    });
  }
}

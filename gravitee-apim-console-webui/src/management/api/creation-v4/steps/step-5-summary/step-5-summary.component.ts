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

import { Component, Inject, OnInit } from '@angular/core';
import { GioLicenseService, License } from '@gravitee/ui-particles-angular';
import { Observable } from 'rxjs';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';
import { Constants } from '../../../../../entities/Constants';

@Component({
  selector: 'step-5-summary',
  templateUrl: './step-5-summary.component.html',
  styleUrls: ['./step-5-summary.component.scss', '../api-creation-steps-common.component.scss'],
})
export class Step5SummaryComponent implements OnInit {
  public currentStepPayload: ApiCreationPayload;
  public paths: string[];
  public hosts: string[];
  public host: string;
  public port: number;
  public listenerTypes: string[];
  public entrypointsDeployable: boolean;
  public endpointsDeployable: boolean;
  public deployable: boolean;
  public shouldUpgrade: boolean;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;
  public hasReviewEnabled = this.constants.env?.settings?.apiReview?.enabled ?? false;

  public apiType: ApiCreationPayload['type'];

  constructor(
    private readonly stepService: ApiCreationStepService,
    public readonly licenseService: GioLicenseService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.currentStepPayload = this.stepService.payload;
    this.apiType = this.currentStepPayload.type;

    this.paths = this.currentStepPayload.paths?.map((path) => path.path);
    this.hosts = this.currentStepPayload.hosts?.map((host) => host.host);
    this.host = this.currentStepPayload.host?.host;
    this.port = this.currentStepPayload.port?.port;
    this.listenerTypes = [
      ...new Set(this.currentStepPayload.selectedEntrypoints.map(({ supportedListenerType }) => supportedListenerType)),
    ];

    this.entrypointsDeployable = this.currentStepPayload.selectedEntrypoints.every(({ deployed }) => deployed);
    this.endpointsDeployable = this.currentStepPayload.selectedEndpoints.every(({ deployed }) => deployed);
    this.deployable = this.entrypointsDeployable && this.endpointsDeployable;
    this.shouldUpgrade = !this.deployable;
    this.license$ = this.licenseService.getLicense$();
    this.isOEM$ = this.licenseService.isOEM$();
  }

  createApi({ deploy, askForReview }: { deploy: boolean; askForReview: boolean }) {
    this.stepService.validStep((payload) => ({ ...payload, deploy, askForReview }));
    this.stepService.finishStepper();
  }

  onChangeStepInfo(stepLabel: string) {
    this.stepService.goToStepLabel(stepLabel);
  }

  public onRequestUpgrade() {
    this.licenseService.openDialog({ feature: ApimFeature.APIM_EN_MESSAGE_REACTOR, context: UTMTags.API_CREATION_MESSAGE_SUMMARY });
  }
}

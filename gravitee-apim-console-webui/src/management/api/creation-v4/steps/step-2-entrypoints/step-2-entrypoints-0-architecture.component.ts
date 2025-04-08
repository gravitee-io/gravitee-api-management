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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { combineLatest, Observable, Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService, License } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { takeUntil, tap } from 'rxjs/operators';

import { Step2Entrypoints1ListComponent } from './step-2-entrypoints-1-list.component';
import { Step2Entrypoints2ConfigComponent } from './step-2-entrypoints-2-config.component';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ApiType } from '../../../../../entities/management-api-v2';
import { UTMTags, ApimFeature } from '../../../../../shared/components/gio-license/gio-license-data';
import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';

@Component({
  selector: 'step-2-entrypoints-0-architecture',
  templateUrl: './step-2-entrypoints-0-architecture.component.html',
  styleUrls: ['./step-2-entrypoints-0-architecture.component.scss', '../api-creation-steps-common.component.scss'],
  standalone: false,
})
export class Step2Entrypoints0ArchitectureComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private initialValue: { type: ApiType };

  public form: UntypedFormGroup;

  public isMissingMessageReactor$: Observable<boolean>;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;
  public isMissingNativeKafkaReactor$: Observable<boolean>;

  private messageLicenseOptions = { feature: ApimFeature.APIM_EN_MESSAGE_REACTOR, context: UTMTags.API_CREATION_TRY_MESSAGE };
  private nativeKafkaLicenseOptions = { feature: ApimFeature.APIM_NATIVE_KAFKA_REACTOR, context: UTMTags.API_CREATION_TRY_MESSAGE };

  constructor(
    private readonly formBuilder: UntypedFormBuilder,
    private readonly stepService: ApiCreationStepService,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly matDialog: MatDialog,
    private readonly iconService: IconService,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.form = this.formBuilder.group({
      type: this.formBuilder.control(this.getArchitectureOptionFromPayload(currentStepPayload), [Validators.required]),
    });

    this.initialValue = this.form.getRawValue();

    this.isMissingMessageReactor$ = this.licenseService.isMissingFeature$(this.messageLicenseOptions.feature);
    this.isMissingNativeKafkaReactor$ = this.licenseService.isMissingFeature$(this.nativeKafkaLicenseOptions.feature);

    this.license$ = this.licenseService.getLicense$();
    this.isOEM$ = this.licenseService.isOEM$();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goBack() {
    this.stepService.goToPreviousStep();
  }

  save() {
    if (this.hasArchitectureOptionChanged(this.stepService.payload)) {
      // When changing the type, all previously filled steps must be deleted to restart from scratch.
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Are you sure?',
            content: 'By changing the architecture type, all previously entered data in Endpoints and Security steps will be reset.',
            confirmButton: 'Validate',
            cancelButton: 'Discard',
          },
        })
        .afterClosed()
        .subscribe((confirmed) => {
          if (confirmed) {
            this.stepService.removeAllNextSteps();
            this.saveByApiType();
          } else {
            this.form.setValue(this.initialValue);
          }
        });
      return;
    }
    this.saveByApiType();
  }

  private getArchitectureOptionFromPayload(payload: ApiCreationPayload): string {
    if (payload.type === 'NATIVE') {
      return payload.selectedNativeType;
    }
    return payload.type ?? null;
  }

  private hasArchitectureOptionChanged(payload: ApiCreationPayload): boolean {
    const previousType = payload.type;
    const previousSelectedNativeType = payload.selectedNativeType;
    const selectedType = this.form.value.type;

    if (previousType === 'NATIVE') {
      return previousSelectedNativeType && previousSelectedNativeType !== selectedType;
    }

    return previousType && selectedType !== previousType;
  }

  private saveByApiType(): void {
    switch (this.form.value.type) {
      case 'PROXY':
        this.doSaveSync();
        break;
      case 'MESSAGE':
        this.doSaveAsync();
        break;
      case 'KAFKA':
        this.doSaveKafka();
        break;
    }
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

  private doSaveKafka() {
    combineLatest([
      this.connectorPluginsV2Service.getEntrypointPlugin('native-kafka'),
      this.connectorPluginsV2Service.getEndpointPlugin('native-kafka'),
    ])
      .pipe(
        tap(([nativeKafkaEntrypoint, nativeKafkaEndpoint]) => {
          this.stepService.validStep((previousPayload) => ({
            ...previousPayload,
            selectedEntrypoints: [
              {
                id: nativeKafkaEntrypoint.id,
                name: nativeKafkaEntrypoint.name,
                icon: this.iconService.registerSvg(nativeKafkaEntrypoint.id, nativeKafkaEntrypoint.icon),
                supportedListenerType: nativeKafkaEntrypoint.supportedListenerType,
                deployed: nativeKafkaEntrypoint.deployed,
              },
            ],
            selectedEndpoints: [
              {
                id: nativeKafkaEndpoint.id,
                name: nativeKafkaEndpoint.name,
                icon: this.iconService.registerSvg(nativeKafkaEndpoint.id, nativeKafkaEndpoint.icon),
                supportedListenerType: nativeKafkaEndpoint.supportedListenerType,
                deployed: nativeKafkaEndpoint.deployed,
              },
            ],
            type: 'NATIVE',
            selectedNativeType: 'KAFKA',
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

  public onRequestMessageUpgrade() {
    this.licenseService.openDialog(this.messageLicenseOptions);
  }

  public onRequestNativeKafkaUpgrade() {
    this.licenseService.openDialog(this.nativeKafkaLicenseOptions);
  }
}

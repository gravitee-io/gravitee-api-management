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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { forkJoin, Observable, Subject } from 'rxjs';
import { GioFormJsonSchemaComponent, GioJsonSchema, GioLicenseService, License } from '@gravitee/ui-particles-angular';
import { debounceTime, takeUntil, tap } from 'rxjs/operators';
import { omitBy } from 'lodash';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { Step3Endpoints1ListComponent } from '../step-3-endpoints/step-3-endpoints-1-list.component';
import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { Step3Endpoints2ConfigComponent } from '../step-3-endpoints/step-3-endpoints-2-config.component';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { KafkaHost, PathV4, Qos } from '../../../../../entities/management-api-v2';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';
import { RestrictedDomainService } from '../../../../../services-ngx/restricted-domain.service';
import { TcpHost } from '../../../../../entities/management-api-v2/api/v4/tcpHost';
import { KafkaHostData } from '../../../component/gio-form-listeners/gio-form-listeners-kafka/gio-form-listeners-kafka-host.component';

@Component({
  selector: 'step-2-entrypoints-2-config',
  templateUrl: './step-2-entrypoints-2-config.component.html',
  styleUrls: ['./step-2-entrypoints-2-config.component.scss', '../api-creation-steps-common.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Step2Entrypoints2ConfigComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: UntypedFormGroup;
  public selectedEntrypoints: { id: string; name: string; supportedListenerType: string; supportedQos?: Qos[] }[];
  public entrypointSchemas: Record<string, GioJsonSchema>;
  public hasHttpListeners: boolean;
  public hasTcpListeners: boolean;
  public hasKafkaListeners: boolean;
  public enableVirtualHost: boolean;
  public domainRestrictions: string[] = [];
  public shouldUpgrade = false;
  public license$: Observable<License>;
  public isOEM$: Observable<boolean>;

  private apiType: ApiCreationPayload['type'];

  constructor(
    private readonly formBuilder: UntypedFormBuilder,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly stepService: ApiCreationStepService,
    private readonly restrictedDomainService: RestrictedDomainService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;
    this.apiType = currentStepPayload.type;

    const paths = currentStepPayload.paths ?? [];
    const tcpHosts = currentStepPayload.hosts ?? [];
    const kafkaHost = currentStepPayload.host;

    this.restrictedDomainService
      .get()
      .pipe(
        tap((restrictedDomain) => {
          this.domainRestrictions = restrictedDomain.map((value) => value.domain) || [];
          this.enableVirtualHost = paths.find((path) => path.host !== undefined || path.overrideAccess !== undefined) != null;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
    this.formGroup = this.formBuilder.group({});

    this.initFormForSyncEntrypoints(currentStepPayload, paths, tcpHosts, kafkaHost);

    currentStepPayload.selectedEntrypoints.forEach(({ id, configuration, selectedQos }) => {
      this.formGroup.addControl(`${id}-config`, this.formBuilder.control(configuration ?? {}));
      this.formGroup.addControl(`${id}-qos`, this.formBuilder.control(selectedQos ?? 'AUTO'));
    });

    if (this.apiType === 'MESSAGE') {
      this.shouldUpgrade = currentStepPayload.selectedEntrypoints.some(({ deployed }) => !deployed);
      this.license$ = this.licenseService.getLicense$();
      this.isOEM$ = this.licenseService.isOEM$();
    }

    forkJoin(
      currentStepPayload.selectedEntrypoints.reduce(
        (map: Record<string, Observable<GioJsonSchema>>, { id }) => ({
          ...map,
          [id]: this.connectorPluginsV2Service.getEntrypointPluginSchema(id),
        }),
        {},
      ),
    )
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((schemas: Record<string, GioJsonSchema>) => {
        // Remove schema with empty input
        this.entrypointSchemas = omitBy(schemas, (schema) => !GioFormJsonSchemaComponent.isDisplayable(schema));
        this.selectedEntrypoints = currentStepPayload.selectedEntrypoints;
        this.changeDetectorRef.markForCheck();
      });
  }

  private initFormForSyncEntrypoints(currentStepPayload: ApiCreationPayload, paths: PathV4[], tcpHosts: TcpHost[], kafkaHost: KafkaHost) {
    this.hasHttpListeners =
      currentStepPayload.selectedEntrypoints.find((entrypoint) => entrypoint.supportedListenerType === 'HTTP') != null;
    if (this.hasHttpListeners) {
      this.formGroup.addControl('paths', this.formBuilder.control(paths, Validators.required));
    }
    this.hasTcpListeners = currentStepPayload.selectedEntrypoints.find((entrypoint) => entrypoint.supportedListenerType === 'TCP') != null;
    if (this.hasTcpListeners) {
      this.formGroup.addControl('hosts', this.formBuilder.control(tcpHosts, Validators.required));
    }
    this.hasKafkaListeners = currentStepPayload.selectedEntrypoints.some((entrypoint) => entrypoint.supportedListenerType === 'KAFKA');
    if (this.hasKafkaListeners) {
      const initialKafkaControlValue: KafkaHostData = kafkaHost;

      this.formGroup.addControl('kafka', this.formBuilder.control(initialKafkaControlValue, Validators.required));
    }

    this.formGroup.valueChanges.pipe(debounceTime(500), takeUntil(this.unsubscribe$)).subscribe(() => {
      // force change detection because the gio-form-listeners components contain a delayed async validators on the form control
      this.changeDetectorRef.detectChanges();
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save(): void {
    const pathsValue = this.formGroup.get('paths')?.value ?? [];
    const hostsValues = this.formGroup.get('hosts')?.value ?? [];
    const hostValue = this.formGroup.get('kafka')?.value;

    this.stepService.validStep((previousPayload) => {
      let paths: PathV4[];
      let hosts: TcpHost[];
      let host: KafkaHost;
      if (this.selectedEntrypoints.some((entrypoint) => entrypoint.supportedListenerType === 'TCP')) {
        hosts = hostsValues;
      } else if (this.selectedEntrypoints.some((entrypoint) => entrypoint.supportedListenerType === 'KAFKA')) {
        host = hostValue;
      } else {
        paths = this.enableVirtualHost
          ? // Remove host and overrideAccess from virualHost if is not necessary
            pathsValue.map(({ path, host, overrideAccess }) => ({ path, host, overrideAccess }))
          : // Clear private properties from gio-listeners-virtual-host component
            pathsValue.map(({ path }) => ({ path }));
      }

      const selectedEntrypoints: ApiCreationPayload['selectedEntrypoints'] = previousPayload.selectedEntrypoints.map((entrypoint) => ({
        ...entrypoint,
        configuration: this.formGroup.get(`${entrypoint.id}-config`)?.value,
        selectedQos: this.formGroup.get(`${entrypoint.id}-qos`)?.value,
      }));

      return {
        ...previousPayload,
        paths,
        hosts,
        host,
        selectedEntrypoints,
      };
    });

    switch (this.apiType) {
      case 'MESSAGE':
        this.stepService.goToNextStep({
          groupNumber: 3,
          component: Step3Endpoints1ListComponent,
        });
        break;
      case 'PROXY':
      case 'NATIVE':
        this.stepService.goToNextStep({
          groupNumber: 3,
          component: Step3Endpoints2ConfigComponent,
        });
        break;
    }
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }

  onRequestUpgrade() {
    this.licenseService.openDialog({
      feature: ApimFeature.APIM_EN_MESSAGE_REACTOR,
      context: UTMTags.API_CREATION_MESSAGE_ENTRYPOINT_CONFIG,
    });
  }
}

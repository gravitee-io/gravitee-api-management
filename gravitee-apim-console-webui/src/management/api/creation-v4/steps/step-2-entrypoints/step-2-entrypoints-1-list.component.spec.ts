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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GioLicenseService, LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { Step2Entrypoints1ListComponent } from './step-2-entrypoints-1-list.component';

import { GioTestingModule } from '../../../../../shared/testing';
import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import { ApimFeature, UTMTags } from '../../../../../shared/components/gio-license/gio-license-data';
import { fakeConnectorPlugin, ConnectorVM } from '../../../../../entities/management-api-v2';
import { ApiCreationV4Module } from '../../api-creation-v4.module';

describe('Step2Entrypoints1ListComponent', () => {
  let component: Step2Entrypoints1ListComponent;
  let fixture: ComponentFixture<Step2Entrypoints1ListComponent>;
  let licenseService: GioLicenseService;
  let stepService: ApiCreationStepService;
  let connectorPluginsService: ConnectorPluginsV2Service;

  const initComponent = async (architecture: 'MESSAGE' | 'PROXY' | 'AI' | 'KAFKA' = 'AI') => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ReactiveFormsModule, ApiCreationV4Module],
      providers: [
        {
          provide: ApiCreationStepService,
          useValue: {
            payload: {
              architecture,
              selectedEntrypoints: [],
            },
            validStep: jest.fn(),
            goToNextStep: jest.fn(),
            goToPreviousStep: jest.fn(),
            invalidateAllNextSteps: jest.fn(),
          },
        },
        {
          provide: ConnectorPluginsV2Service,
          useValue: {
            listAIEntrypointPlugins: jest.fn().mockReturnValue(of([])),
            listAsyncEntrypointPlugins: jest.fn().mockReturnValue(of([])),
            listSyncEntrypointPlugins: jest.fn().mockReturnValue(of([])),
            selectedPluginsNotAvailable: jest.fn().mockReturnValue(false),
            getEndpointPlugin: jest.fn().mockReturnValue(of(fakeConnectorPlugin())),
          },
        },
        {
          provide: IconService,
          useValue: {
            registerSvg: jest.fn((id: string) => `gio-literal:${id}`),
          },
        },
        {
          provide: MatDialog,
          useValue: {
            open: jest.fn().mockReturnValue({
              afterClosed: jest.fn().mockReturnValue(of(true)),
            }),
          },
        },
        {
          provide: GioLicenseService,
          useValue: {
            getLicense$: jest.fn().mockReturnValue(of({ tier: 'universe', features: [], packs: [] })),
            isOEM$: jest.fn().mockReturnValue(of(false)),
            openDialog: jest.fn(),
          },
        },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(Step2Entrypoints1ListComponent);
    component = fixture.componentInstance;
    licenseService = TestBed.inject(GioLicenseService);
    stepService = TestBed.inject(ApiCreationStepService);
    connectorPluginsService = TestBed.inject(ConnectorPluginsV2Service);
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('onRequestUpgrade', () => {
    beforeEach(async () => {
      await initComponent('AI');
      fixture.detectChanges();
    });

    it('should open license dialog for LLM_PROXY entrypoint', () => {
      const llmEntrypoint: ConnectorVM = {
        id: 'llm-proxy-entrypoint',
        name: 'LLM Proxy',
        supportedApiType: 'LLM_PROXY',
        icon: 'icon',
        deployed: true,
        description: 'LLM Proxy Description',
        isEnterprise: false,
        supportedListenerType: 'HTTP',
        supportedQos: ['NONE'],
      };

      component.entrypoints = [llmEntrypoint];

      component.formGroup.patchValue({
        selectedEntrypointsIds: ['llm-proxy-entrypoint'],
      });

      const openDialogSpy = jest.spyOn(licenseService, 'openDialog');

      component.onRequestUpgrade();

      expect(openDialogSpy).toHaveBeenCalledWith({
        feature: ApimFeature.APIM_LLM_PROXY_REACTOR,
        context: UTMTags.API_CREATION_LLM_ENTRYPOINT,
      });
    });

    it('should open license dialog for MESSAGE entrypoint (non-LLM)', () => {
      const messageEntrypoint: ConnectorVM = {
        id: 'message-entrypoint',
        name: 'Message',
        supportedApiType: 'MESSAGE',
        icon: 'icon',
        deployed: true,
        description: 'Message Description',
        isEnterprise: false,
        supportedListenerType: 'HTTP',
        supportedQos: ['AUTO'],
      };

      component.entrypoints = [messageEntrypoint];

      component.formGroup.patchValue({
        selectedEntrypointsIds: ['message-entrypoint'],
      });

      const openDialogSpy = jest.spyOn(licenseService, 'openDialog');

      component.onRequestUpgrade();

      expect(openDialogSpy).toHaveBeenCalledWith({
        feature: ApimFeature.APIM_EN_MESSAGE_REACTOR,
        context: UTMTags.API_CREATION_MESSAGE_ENTRYPOINT,
      });
    });

    it('should open license dialog for PROXY entrypoint (else branch)', () => {
      const proxyEntrypoint: ConnectorVM = {
        id: 'http-proxy',
        name: 'HTTP Proxy',
        supportedApiType: 'PROXY',
        icon: 'icon',
        deployed: true,
        description: 'HTTP Proxy Description',
        isEnterprise: false,
        supportedListenerType: 'HTTP',
        supportedQos: ['NONE'],
      };

      component.entrypoints = [proxyEntrypoint];

      component.formGroup.patchValue({
        selectedEntrypointsIds: ['http-proxy'],
      });

      const openDialogSpy = jest.spyOn(licenseService, 'openDialog');

      component.onRequestUpgrade();

      expect(openDialogSpy).toHaveBeenCalledWith({
        feature: ApimFeature.APIM_EN_MESSAGE_REACTOR,
        context: UTMTags.API_CREATION_MESSAGE_ENTRYPOINT,
      });
    });

    it('should handle multiple selected entrypoints (takes first)', () => {
      const llmEntrypoint: ConnectorVM = {
        id: 'llm-proxy-entrypoint',
        name: 'LLM Proxy',
        supportedApiType: 'LLM_PROXY',
        icon: 'icon',
        deployed: false,
        description: 'LLM Proxy Description',
        isEnterprise: false,
        supportedListenerType: 'HTTP',
        supportedQos: ['NONE'],
      };

      const messageEntrypoint: ConnectorVM = {
        id: 'message-entrypoint',
        name: 'Message',
        supportedApiType: 'MESSAGE',
        icon: 'icon',
        deployed: true,
        description: 'Message Description',
        isEnterprise: false,
        supportedListenerType: 'HTTP',
        supportedQos: ['NONE'],
      };

      component.entrypoints = [llmEntrypoint, messageEntrypoint];

      component.formGroup.patchValue({
        selectedEntrypointsIds: ['llm-proxy-entrypoint', 'message-entrypoint'],
      });

      const openDialogSpy = jest.spyOn(licenseService, 'openDialog');

      component.onRequestUpgrade();

      expect(openDialogSpy).toHaveBeenCalledWith({
        feature: ApimFeature.APIM_LLM_PROXY_REACTOR,
        context: UTMTags.API_CREATION_LLM_ENTRYPOINT,
      });
    });
  });

  describe('ngOnInit', () => {
    it('should initialize for AI architecture', async () => {
      await initComponent('AI');
      const aiEntrypoints = [
        fakeConnectorPlugin({ id: 'llm-proxy', name: 'LLM Proxy', supportedApiType: 'LLM_PROXY' }),
        fakeConnectorPlugin({ id: 'agent-to-agent', name: 'Agent to Agent', supportedApiType: 'MESSAGE' }),
      ];

      jest.spyOn(connectorPluginsService, 'listAIEntrypointPlugins').mockReturnValue(of(aiEntrypoints));

      fixture.detectChanges();

      expect(connectorPluginsService.listAIEntrypointPlugins).toHaveBeenCalled();
      expect(component.entrypoints).toBeDefined();
      expect(component.architecture).toBe('AI');
    });

    it('should initialize for MESSAGE architecture and filter out agent-to-agent', async () => {
      await initComponent('MESSAGE');
      const messageEntrypoints = [
        fakeConnectorPlugin({ id: 'http-get', name: 'HTTP GET', supportedApiType: 'MESSAGE' }),
        fakeConnectorPlugin({ id: 'agent-to-agent', name: 'Agent to Agent', supportedApiType: 'MESSAGE' }),
      ];

      jest.spyOn(connectorPluginsService, 'listAsyncEntrypointPlugins').mockReturnValue(of(messageEntrypoints));

      fixture.detectChanges();

      expect(component.entrypoints.find(e => e.id === 'agent-to-agent')).toBeUndefined();
      expect(component.entrypoints.find(e => e.id === 'http-get')).toBeDefined();
    });

    it('should initialize for PROXY architecture', async () => {
      await initComponent('PROXY');
      const proxyEntrypoints = [fakeConnectorPlugin({ id: 'http-proxy', name: 'HTTP Proxy', supportedApiType: 'PROXY' })];

      jest.spyOn(connectorPluginsService, 'listSyncEntrypointPlugins').mockReturnValue(of(proxyEntrypoints));

      fixture.detectChanges();

      expect(connectorPluginsService.listSyncEntrypointPlugins).toHaveBeenCalled();
    });
  });

  describe('goBack', () => {
    it('should call stepService.goToPreviousStep', async () => {
      await initComponent('AI');
      fixture.detectChanges();

      component.goBack();

      expect(stepService.goToPreviousStep).toHaveBeenCalled();
    });
  });
});

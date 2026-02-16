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
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { PolicyStudioConfigComponent } from './policy-studio-config.component';
import { PolicyStudioConfigModule } from './policy-studio-config.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeFlowConfigurationSchema } from '../../../../entities/flow/configurationSchema.fixture';
import { PolicyStudioService } from '../policy-studio.service';
import { toApiDefinition } from '../models/ApiDefinition';
import { fakeApiV2 } from '../../../../entities/management-api-v2';
import { Constants } from '../../../../entities/Constants';

describe('PolicyStudioConfigComponent', () => {
  let fixture: ComponentFixture<PolicyStudioConfigComponent>;
  let loader: HarnessLoader;
  let component: PolicyStudioConfigComponent;
  let httpTestingController: HttpTestingController;
  let policyStudioService: PolicyStudioService;

  const configurationSchema = fakeFlowConfigurationSchema();
  const api = fakeApiV2();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PolicyStudioConfigModule, MatIconTestingModule],
      providers: [
        {
          provide: Constants,
          useValue: {
            ...CONSTANTS_TESTING,
            org: {
              ...CONSTANTS_TESTING.org,
              settings: {
                ...CONSTANTS_TESTING.org.settings,
                emulateV4Engine: { enabled: true },
              },
            },
          },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PolicyStudioConfigComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    policyStudioService = TestBed.inject(PolicyStudioService);
    policyStudioService.setApiDefinition(toApiDefinition(api));

    fixture.detectChanges();

    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/flows/configuration-schema`).flush(configurationSchema);
  });

  describe('ngOnInit', () => {
    it('should setup properties', async () => {
      const apiDefinitionToExpect = toApiDefinition(api);
      expect(component.apiDefinition).toStrictEqual({
        id: apiDefinitionToExpect.id,
        name: apiDefinitionToExpect.name,
        origin: 'management',
        flows: apiDefinitionToExpect.flows,
        flow_mode: apiDefinitionToExpect.flow_mode,
        resources: apiDefinitionToExpect.resources,
        version: apiDefinitionToExpect.version,
        services: apiDefinitionToExpect.services,
        properties: apiDefinitionToExpect.properties,
        execution_mode: apiDefinitionToExpect.execution_mode,
      });
      expect(component.flowConfigurationSchema).toStrictEqual(configurationSchema);
    });
  });

  it('should emulate v4 engine', async () => {
    const activateSupportSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emulateV4Engine' }));
    expect(await activateSupportSlideToggle.isDisabled()).toEqual(false);

    let done = false;
    // Expect last apiDefinition
    policyStudioService.getApiDefinitionToSave$().subscribe(apiDefinition => {
      expect(apiDefinition.execution_mode).toEqual('v4-emulation-engine');
      done = true;
    });

    await activateSupportSlideToggle.check();

    expect(done).toEqual(true);
  });

  it('should disable field when origin is kubernetes', async () => {
    const api = fakeApiV2({
      definitionContext: { origin: 'KUBERNETES' },
    });
    policyStudioService.setApiDefinition(toApiDefinition(api));

    const activateSupportSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ name: 'emulateV4Engine' }));
    expect(await activateSupportSlideToggle.isDisabled()).toEqual(true);
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});

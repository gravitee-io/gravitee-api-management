/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness, HarnessLoader, HarnessPredicate } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import ApplicationComponent from './application.component';
import { fakeApplication } from '../../../entities/application/application.fixture';
import { ConfigurationPortalNext } from '../../../entities/configuration/configuration-portal-next';
import { fakeUserApplicationPermissions } from '../../../entities/permission/permission.fixtures';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { ConfigService } from '../../../services/config.service';
import { applicationListBreadcrumb } from '../applications/application-breadcrumbs';

type MembershipConfig = NonNullable<NonNullable<ConfigurationPortalNext['applications']>['membership']>;

const defaultMembership: MembershipConfig = {
  enabled: true,
  transferOwnership: { enabled: true },
  invitations: { enabled: true },
};

function createConfigService(membershipOverride?: Partial<MembershipConfig>) {
  return {
    configuration: {
      portalNext: {
        applications: {
          membership: {
            ...defaultMembership,
            ...membershipOverride,
          },
        },
      },
    },
  };
}

class TestIdHarness extends ComponentHarness {
  static hostSelector = '[data-testid]';

  static for(testId: string): HarnessPredicate<TestIdHarness> {
    return new HarnessPredicate(TestIdHarness, { selector: `[data-testid="${testId}"]` });
  }
}

async function setupApplicationComponent(configService = createConfigService()) {
  await TestBed.configureTestingModule({
    imports: [ApplicationComponent],
    providers: [provideRouter([]), provideNoopAnimations(), { provide: ConfigService, useValue: configService }],
  }).compileComponents();

  const fixture = TestBed.createComponent(ApplicationComponent);
  const breadcrumbService = TestBed.inject(BreadcrumbService);
  breadcrumbService.clear();
  const loader = TestbedHarnessEnvironment.loader(fixture);
  fixture.componentRef.setInput('application', fakeApplication({ id: 'app-1', name: 'My App' }));
  fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R'] }));
  return { fixture, breadcrumbService, loader };
}

describe('ApplicationComponent', () => {
  let fixture: ComponentFixture<ApplicationComponent>;
  let breadcrumbService: BreadcrumbService;
  let loader: HarnessLoader;

  beforeEach(async () => {
    ({ fixture, breadcrumbService, loader } = await setupApplicationComponent());
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should set breadcrumbs for application details', () => {
    fixture.detectChanges();
    expect(breadcrumbService.breadcrumbs()).toEqual([applicationListBreadcrumb(true), { id: 'application-app-1', label: 'My App' }]);
  });

  it('should render Members tab when user has MEMBER read permission', async () => {
    fixture.detectChanges();
    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-members-tab'))).not.toBeNull();
  });

  it('should render Invitations tab when user has MEMBER read permission', async () => {
    fixture.detectChanges();
    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-invitations-tab'))).not.toBeNull();
  });

  it('should not render Members tab when user lacks MEMBER read permission', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: [] }));
    fixture.detectChanges();
    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-members-tab'))).toBeNull();
  });

  it('should not render Invitations tab when user lacks MEMBER read permission', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: [] }));
    fixture.detectChanges();
    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-invitations-tab'))).toBeNull();
  });

  it('should not render Members tab when MEMBER has no R flag', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['U'] }));
    fixture.detectChanges();
    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-members-tab'))).toBeNull();
  });

  it('should not render Invitations tab when MEMBER has no R flag', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['U'] }));
    fixture.detectChanges();
    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-invitations-tab'))).toBeNull();
  });
});

describe('ApplicationComponent with disabled membership toggles', () => {
  let fixture: ComponentFixture<ApplicationComponent>;
  let loader: HarnessLoader;

  afterEach(() => {
    fixture.destroy();
  });

  it('should not render Members tab when member mapping toggle is disabled', async () => {
    ({ fixture, loader } = await setupApplicationComponent(createConfigService({ enabled: false })));
    fixture.detectChanges();

    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-members-tab'))).toBeNull();
  });

  it('should not render Invitations tab when invitations toggle is disabled', async () => {
    ({ fixture, loader } = await setupApplicationComponent(createConfigService({ invitations: { enabled: false } })));
    fixture.detectChanges();

    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-invitations-tab'))).toBeNull();
  });

  it('should not render Invitations tab when parent membership toggle is disabled', async () => {
    ({ fixture, loader } = await setupApplicationComponent(createConfigService({ enabled: false, invitations: { enabled: true } })));
    fixture.detectChanges();

    expect(await loader.getHarnessOrNull(TestIdHarness.for('application-invitations-tab'))).toBeNull();
  });
});

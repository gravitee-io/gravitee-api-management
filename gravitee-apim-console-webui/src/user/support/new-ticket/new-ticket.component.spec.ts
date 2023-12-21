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
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { NewTicketComponent } from './new-ticket.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { TicketsModule } from '../tickets.module';
import { Api, ApisResponse, fakeApiV2, fakeApiV4 } from '../../../entities/management-api-v2';
import { fakePagedResult, PagedResult } from '../../../entities/pagedResult';
import { Application } from '../../../entities/application/application';
import { fakeApplication } from '../../../entities/application/Application.fixture';

describe('NewTicketComponent', () => {
  let fixture: ComponentFixture<NewTicketComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, TicketsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // Focus-trap, set it to true to avoid warning
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(NewTicketComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create a ticket', async () => {
    const apiV2 = fakeApiV2({ id: '6e9d2b27-6bad-41cf-aa57-ffdfaf1199ae', name: 'API V2' });
    const apiV4 = fakeApiV4({ id: '9f3c07dc-0967-47cd-839e-a818e66492bb', name: 'API V4' });
    respondToGetAPIs([apiV2, apiV4]);
    const app1 = fakeApplication({ id: '7365661c-12c9-4661-977d-0ae4bb6c1ed6', name: 'App 1', owner: { displayName: 'Owner 1' } });
    const app2 = fakeApplication({ id: '658c6325-b500-410d-ba1b-56fccbca33b4' });
    respondToGetApplications([app1, app2]);

    // Select API
    const apisInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="api"]' }));
    await apisInput.open();
    const options = await apisInput.getOptions();
    expect(options.length).toEqual(3);
    await apisInput.clickOptions({ text: `${apiV2.name} (${apiV2.apiVersion})` });

    // Select Application
    const applicationsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="application"]' }));
    await applicationsInput.open();
    const appOptions = await applicationsInput.getOptions();
    expect(appOptions.length).toEqual(3);
    await applicationsInput.clickOptions({ text: `${app1.name} (${app1.owner.displayName})` });

    // Write subject
    const subjectInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="subject"]' }));
    await subjectInput.setValue('A simple ticket');

    // Write Content
    const contentInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="content"]' }));
    await contentInput.setValue('Aper etiam atque etiam has exponere, ut quaeque');

    // Activate Copy to sender
    const copyToSenderInput = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="copyToSender"]' }));
    await copyToSenderInput.check();

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
    await saveBar.clickSubmit();

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.baseURL}/platform/tickets`,
    });

    expect(req.request.body).toEqual({
      api: apiV2.id,
      application: app1.id,
      subject: 'A simple ticket',
      content: 'Aper etiam atque etiam has exponere, ut quaeque',
      copyToSender: true,
    });

    req.flush(null);
  });

  function respondToGetAPIs(apis: Api[]) {
    const response: ApisResponse = {
      data: apis,
    };

    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=1&perPage=100`,
      })
      .flush(response);
  }
  function respondToGetApplications(applications: Application[]) {
    const response: PagedResult<Application> = fakePagedResult(applications);

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=100`,
      })
      .flush(response);
  }
});

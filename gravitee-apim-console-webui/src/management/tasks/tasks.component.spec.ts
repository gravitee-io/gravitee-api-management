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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { TasksComponent } from './tasks.component';
import { TasksModule } from './tasks.module';
import { TasksHarness } from './tasks.harness';

import { PagedResult } from '../../entities/pagedResult';
import { Task } from '../../entities/task/task';
import { UIRouterState } from '../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../shared/testing';

describe('TasksComponent', () => {
  const fakeAjsState = {
    go: jest.fn(),
  };

  const responseData = {
    data: [
      {
        type: 'SUBSCRIPTION_APPROVAL',
        data: {
          id: '3c2f0130-d299-4314-af01-30d299431474',
          api: '9a3825da-64a8-43d4-b825-da64a8e3d42e',
          plan: '629c1b5e-8cee-3e14-bcf2-c2848311c993',
          application: 'f010021b-067b-4bd8-9002-1b067b5bd8e7',
          status: 'PENDING',
          request: '',
          configuration: {},
          consumerStatus: 'STARTED',
          subscribed_by: 'f83b92fc-0051-4a59-bb92-fc00518a591c',
          created_at: 1679319899727,
          updated_at: 1679319899727,
        },
        created_at: 1679319899727,
      },
      {
        type: 'USER_REGISTRATION_APPROVAL',
        data: {
          id: '02940b9a-c865-4ab3-940b-9ac8657ab3bd',
          organizationId: 'DEFAULT',
          firstname: 'Nicolas',
          lastname: 'Gera',
          email: 'nicolasgeraud@gmail.com',
          source: 'gravitee',
          sourceId: 'nicolasgeraud@gmail.com',
          status: 'PENDING',
          loginCount: 0,
          displayName: 'Nicolas Gera',
          created_at: 1654005242662,
          updated_at: 1654005242662,
          primary_owner: false,
          number_of_active_tokens: 0,
        },
        created_at: 1654005242662,
      },
      {
        type: 'IN_REVIEW',
        data: {
          id: 'a18f31cf-d466-4404-8f31-cfd4669404d0',
          referenceType: 'API',
          referenceId: 'c097d5ac-0e13-4d18-97d5-ac0e13bd18b7',
          type: 'REVIEW',
          state: 'IN_REVIEW',
          user: '7b3a0146-e6c7-450f-ba01-46e6c7e50f0b',
          createdAt: new Date(1657019699548),
        },
        created_at: 1657019699548,
      },
      {
        type: 'REQUEST_FOR_CHANGES',
        data: {
          id: '4de33282-73db-482f-a332-8273dba82f17',
          referenceType: 'API',
          referenceId: 'fcf8a3d9-c612-4ef8-b8a3-d9c6120ef844',
          type: 'REVIEW',
          state: 'REQUEST_FOR_CHANGES',
          user: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
          createdAt: new Date(1640176941509),
        },
        created_at: 1640176941509,
      },
      {
        type: 'PROMOTION_APPROVAL',
        data: {
          authorEmail: 'akbar@f1soft.com',
          apiName: 'swagger',
          sourceEnvironmentName: 'DM-\uD83E\uDD56',
          targetEnvironmentName: 'DM-\uD83E\uDD50',
          isApiUpdate: false,
          authorDisplayName: 'Akbar Khan',
          promotionId: '1d35552a-61eb-471d-b555-2a61eb071d19',
        },
        created_at: 1659113183017,
      },
    ],
    metadata: {
      'f010021b-067b-4bd8-9002-1b067b5bd8e7': {
        name: 'MyClient',
      },
      '9a3825da-64a8-43d4-b825-da64a8e3d42e': {
        name: 'API A',
      },
      '629c1b5e-8cee-3e14-bcf2-c2848311c993': {
        name: 'star',
        api: '9a3825da-64a8-43d4-b825-da64a8e3d42e',
      },
      'f74eda3c-7bb8-3360-a60a-1cc6a08e270e': {
        name: 'platinum',
        api: '9a3825da-64a8-43d4-b825-da64a8e3d42e',
      },
      'c097d5ac-0e13-4d18-97d5-ac0e13bd18b7': {
        name: 'just_api',
      },
      'fcf8a3d9-c612-4ef8-b8a3-d9c6120ef844': {
        name: 'marcEchoNodeInfos',
      },
    },
    page: {
      current: 1,
      size: 5,
      per_page: 5,
      total_pages: 1,
      total_elements: 5,
    },
  };
  const tasks = new PagedResult<Task>();
  tasks.populate(responseData);

  let fixture: ComponentFixture<TasksComponent>;
  let harness: TasksHarness;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [TasksComponent],
      imports: [NoopAnimationsModule, TasksModule, MatIconTestingModule, GioHttpTestingModule],
      providers: [{ provide: UIRouterState, useValue: fakeAjsState }],
    }).compileComponents();

    fixture = TestBed.createComponent(TasksComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TasksHarness);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectGetTasks() {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/user/tasks`, method: 'GET' }).flush(tasks);
  }

  describe('init', () => {
    beforeEach(async () => {
      await init();
      expectGetTasks();
    });

    it('should show tasks', async () => {
      const tasks = await harness.getTasks();
      expect(tasks.length).toEqual(5);

      expect(await tasks[0].getText()).toContain('Subscription');
      expect(await tasks[1].getText()).toContain('API promotion request');
      expect(await tasks[2].getText()).toContain('API review');
      expect(await tasks[3].getText()).toContain('User registration');
      expect(await tasks[4].getText()).toContain('API review');
    });

    it('should show only one button for simple tasks', async () => {
      const tasks = await harness.getTasks();
      expect(await (await tasks[0].getHarness(MatButtonHarness)).getText()).toEqual('Validate');
      expect(await (await tasks[2].getHarness(MatButtonHarness)).getText()).toEqual('Review');
      expect(await (await tasks[3].getHarness(MatButtonHarness)).getText()).toEqual('Validate');
      expect(await (await tasks[4].getHarness(MatButtonHarness)).getText()).toEqual('Make changes');
    });

    it('should show approve / reject buttons for API promotion', async () => {
      const tasks = await harness.getTasks();
      const buttons: MatButtonHarness[] = await tasks[1].getAllHarnesses(MatButtonHarness);
      expect(buttons.length).toEqual(2);
      expect(await buttons[0].getText()).toEqual('Accept');
      expect(await buttons[1].getText()).toEqual('Reject');
    });

    it('should send apiId and subscriptionId after validating subscription', async () => {
      const tasks = await harness.getTasks();
      expect(tasks.length).toEqual(5);

      expect(await tasks[0].getText()).toContain('Subscription');
      const validateButton = await tasks[0].getHarness(MatButtonHarness);
      await validateButton.click();
      expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.detail.portal.subscriptions.subscription', {
        apiId: responseData.data[0].data.api,
        subscriptionId: responseData.data[0].data.id,
      });
    });
  });
});

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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';

import { TopApisWidgetComponent } from './top-apis-widget.component';
import { TopApisWidgetHarness } from './top-apis-widget.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../testing';
import { Constants } from '../../../entities/Constants';

describe('TopApisWidgetComponent', () => {
  let fixture: ComponentFixture<TopApisWidgetComponent>;
  let componentHarness: TopApisWidgetHarness;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [],
      imports: [TopApisWidgetComponent, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TopApisWidgetComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopApisWidgetHarness);
    fixture.componentInstance.data = [
      {
        id: '7c316e07-7661-406b-b16e-077661f06b73',
        name: 'John Doe',
        count: 10000,
      },
      {
        id: '7c316e07-7661-406b-b16e-077661f06b73',
        name: 'APIs s',
        count: 2000,
      },
      {
        id: '7c316e07-7661-406b-b16e-077661f06b73',
        name: 'APIs sd2',
        count: 1500,
      },
    ];

    fixture.componentInstance.ngOnChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('table', () => {
    beforeEach(async () => {
      await init();
    });

    it('should have correct number of rows', async () => {
      const rows = await componentHarness.rowsNumber();
      expect(rows).toEqual(3);
    });
  });
});

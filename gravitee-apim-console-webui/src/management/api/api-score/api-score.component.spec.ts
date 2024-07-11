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
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatAccordionHarness } from '@angular/material/expansion/testing';

import { ApiScoreComponent } from './api-score.component';
import { ApiScoreHarness } from './api-score.harness';
import { ApiScoreModule } from './api-score.module';
import { ApiScore, ScoreIssue } from './api-score.model';

import { GioTestingModule } from '../../../shared/testing';

describe('ApiScoreComponent', () => {
  let fixture: ComponentFixture<ApiScoreComponent>;
  let componentHarness: ApiScoreHarness;
  // let httpTestingController: HttpTestingController;

  const scoreIssues: ScoreIssue[] = [
    {
      severity: 'warning',
      location: '9:134',
      recommendation: 'Operation “description” must be present and non-empty string.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '10:137',
      recommendation: 'Info object must have “contact” object.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '10:130',
      recommendation: 'Operation “description” must be present and non-empty string.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '10:155',
      recommendation: 'Operation mush have non-empty “tags” array.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '9:1',
      recommendation: 'Operation “description” must be present and non-empty string.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '10:1300',
      recommendation: 'Info object must have “contact” object.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '1:0',
      recommendation: 'Operation “description” must be present and non-empty string.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '10:3',
      recommendation: 'Operation mush have non-empty “tags” array.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '9:16',
      recommendation: 'Operation “description” must be present and non-empty string.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '100:13',
      recommendation: 'Info object must have “contact” object.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '160:13',
      recommendation: 'Operation “description” must be present and non-empty string.',
      path: 'paths/.get',
    },
    {
      severity: 'warning',
      location: '170:13',
      recommendation: 'Operation mush have non-empty “tags” array.',
      path: 'paths/.get',
    },
  ];

  const init = async (
    apiScore: ApiScore = {
      all: 0,
      errors: 0,
      warnings: 0,
      infos: 0,
      hints: 0,
      lastEvaluation: 1,
      scoreLists: [
        {
          name: 'API Definition',
          source: 'Gravitee API definition',
          issues: [...scoreIssues],
        },
        {
          name: 'Documentation page name one',
          source: 'Swagger',
          issues: [...scoreIssues],
        },
        {
          name: 'Documentation page name two',
          source: 'AsyncAPI',
          issues: [...scoreIssues],
        },
      ],
    },
  ) => {
    await TestBed.configureTestingModule({
      declarations: [ApiScoreComponent],
      imports: [ApiScoreModule, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiScoreComponent);
    // httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoreHarness);
    fixture.componentInstance.apiScore = apiScore;
    fixture.detectChanges();
  };

  describe('ApiScoreComponent', () => {
    describe('apis lists', () => {
      beforeEach(() => {
        init();
      });

      it('should display lists with api scores', async () => {
        const lists: MatAccordionHarness[] = await componentHarness.getAccordion();
        expect(lists).toHaveLength(3);
      });
    });
  });
});

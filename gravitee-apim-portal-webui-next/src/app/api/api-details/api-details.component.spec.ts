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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatChipHarness } from '@angular/material/chips/testing';
import { MatTabNavBarHarness } from '@angular/material/tabs/testing';

import { ApiDetailsComponent } from './api-details.component';
import { Api } from '../../../entities/api/api';
import { fakeApi } from '../../../entities/api/api.fixtures';
import { AppTestingModule } from '../../../testing/app-testing.module';

describe('ApiDetailsComponent', () => {
  let component: ApiDetailsComponent;
  let fixture: ComponentFixture<ApiDetailsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const init = async (api: Api) => {
    await TestBed.configureTestingModule({
      imports: [ApiDetailsComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDetailsComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;
    component.api = api;
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('MCP enabled', () => {
    beforeEach(async () => {
      await init(
        fakeApi({
          mcp: {
            mcpPath: '/mcp',
            tools: [],
          },
        }),
      );
    });

    it('should show Tools tab if api has mcp enabled', async () => {
      const tabs = await harnessLoader.getHarness(MatTabNavBarHarness);
      const links = await tabs.getLinks();
      expect(await links[2].getLabel()).toBe('MCP Tools');
    });

    it('should show MCP Server badge', async () => {
      const mcpServerChip = await harnessLoader.getHarnessOrNull(MatChipHarness.with({ text: /MCP/ }));
      expect(mcpServerChip).toBeTruthy();
    });
  });
});

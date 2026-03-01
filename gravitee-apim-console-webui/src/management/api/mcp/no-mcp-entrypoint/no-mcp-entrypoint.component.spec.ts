/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatCardHarness } from '@angular/material/card/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { NoMcpEntrypointComponent } from './no-mcp-entrypoint.component';

import { GioTestingModule } from '../../../../shared/testing';

describe('NoMcpEntrypointComponent', () => {
  let fixture: ComponentFixture<NoMcpEntrypointComponent>;
  let component: NoMcpEntrypointComponent;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoMcpEntrypointComponent, GioTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(NoMcpEntrypointComponent);
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should show "Unavailable" content when license is missing', async () => {
    fixture.componentRef.setInput('canEnableMcp', false);
    fixture.detectChanges();

    expect(await getTitle()).toEqual('MCP Tool Server unavailable');
    expect(await getBodyText()).toContain('MCP Tool Server is part of Gravitee Enterprise');
  });

  it('should show "Enable" content when license is present', async () => {
    fixture.componentRef.setInput('canEnableMcp', true);
    fixture.detectChanges();

    expect(await getTitle()).toEqual('Bring your tools to life by enabling MCP');
    expect(await getBodyText()).toContain('Once activated, you can configure, manage and integrate tools');
  });

  it('should emit output when clicking button and MCP is enabled', async () => {
    fixture.componentRef.setInput('canEnableMcp', true);
    fixture.detectChanges();

    const emitSpy = jest.spyOn(component.enableMcpEntrypoint, 'emit');

    const button = await harnessLoader.getHarness(MatButtonHarness);
    await button.click();

    expect(emitSpy).toHaveBeenCalledWith(true);
  });

  it('should open trial URL when clicking button and MCP is disabled', async () => {
    fixture.componentRef.setInput('canEnableMcp', false);
    fixture.detectChanges();

    const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

    const button = await harnessLoader.getHarness(MatButtonHarness);
    await button.click();

    expect(windowSpy).toHaveBeenCalledWith(
      expect.stringContaining('https://www.gravitee.io/self-hosted-trial'),
      '_blank',
      'noopener,noreferrer',
    );
  });

  /**
   * Helper functions to interact with the template
   */
  async function getTitle(): Promise<string> {
    const card = await harnessLoader.getHarness(MatCardHarness);
    return card.getTitleText();
  }

  async function getBodyText(): Promise<string> {
    const card = await harnessLoader.getHarness(MatCardHarness);
    return card.getSubtitleText();
  }
});

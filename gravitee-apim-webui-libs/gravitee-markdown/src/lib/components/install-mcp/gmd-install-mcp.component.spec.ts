/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdInstallMcpComponent } from './gmd-install-mcp.component';
import { GmdInstallMcpComponentHarness } from './gmd-install-mcp.component.harness';

describe('GmdInstallMcpComponent', () => {
  let fixture: ComponentFixture<GmdInstallMcpComponent>;
  let harness: GmdInstallMcpComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GmdInstallMcpComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(GmdInstallMcpComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, GmdInstallMcpComponentHarness);
  });

  it('should render a placeholder when neither explicit inputs nor context are available', async () => {
    fixture.detectChanges();

    expect(await harness.getPlaceholderText()).toContain('Provide a server name and URL');
  });

  it('should filter the available clients', async () => {
    fixture.componentRef.setInput('name', 'weather');
    fixture.componentRef.setInput('url', 'https://api.example.com/mcp');
    fixture.componentRef.setInput('clients', 'cursor');
    fixture.detectChanges();

    expect(await harness.hasClient('cursor')).toBe(true);
    expect(await harness.hasClient('vscode')).toBe(false);
    expect(await harness.hasClient('claude-desktop')).toBe(false);
  });

  it('should switch installers when a different client tab is selected', async () => {
    fixture.componentRef.setInput('name', 'weather');
    fixture.componentRef.setInput('url', 'https://api.example.com/mcp');
    fixture.detectChanges();

    expect(await harness.getInstallHref()).toContain('cursor://anysphere.cursor-deeplink');

    await harness.clickClient('vscode');
    expect(await harness.getInstallHref()).toContain('vscode:mcp/install?');
  });

  it('should not render a default auth header in the snippet for remote servers', async () => {
    fixture.componentRef.setInput('name', 'weather');
    fixture.componentRef.setInput('url', 'https://api.example.com/mcp');
    fixture.detectChanges();

    expect(await harness.getSnippetText()).not.toContain('"Authorization"');
  });

  it('should not render the web installer fallback button', async () => {
    fixture.componentRef.setInput('name', 'weather');
    fixture.componentRef.setInput('url', 'https://api.example.com/mcp');
    fixture.detectChanges();

    expect(await harness.getFallbackHref()).toBeNull();
  });

  it('should update the copy label when the snippet is copied', async () => {
    fixture.componentRef.setInput('name', 'weather');
    fixture.componentRef.setInput('url', 'https://api.example.com/mcp');
    fixture.detectChanges();

    fixture.componentInstance.onSnippetCopied();
    fixture.detectChanges();

    expect(await harness.getCopyButtonText()).toBe('Copied');
  });
});

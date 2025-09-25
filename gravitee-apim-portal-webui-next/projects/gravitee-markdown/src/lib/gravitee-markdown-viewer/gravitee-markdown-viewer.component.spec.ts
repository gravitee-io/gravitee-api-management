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

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GraviteeMarkdownViewerHarness } from './gravitee-markdown-viewer.harness';
import { GraviteeMarkdownViewerModule } from './gravitee-markdown-viewer.module';

@Component({
  selector: 'gmd-test-component',
  imports: [GraviteeMarkdownViewerModule],
  template: `<gmd-viewer [content]="content" />`,
})
class TestComponent {
  public content = '';
}

describe('GraviteeMarkdownViewerComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let harness: GraviteeMarkdownViewerHarness;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GraviteeMarkdownViewerHarness);

    fixture.detectChanges();
  });

  it('should create', async () => {
    expect(component).toBeTruthy();
    expect(await harness.getRenderedHtml()).toBe('');
  });

  describe('Markdown rendering', () => {
    it('should render markdown content', async () => {
      component.content = '# Hello, World!';
      fixture.detectChanges();

      expect(await harness.getRenderedHtml()).toContain('<h1 id="hello-world">Hello, World!</h1>');
    });

    it('should render images', async () => {
      component.content = '![Gravitee Logo](https://example.com/gravitee-logo.png)';
      fixture.detectChanges();

      expect(await harness.getRenderedHtml()).toContain('<p><img src="https://example.com/gravitee-logo.png" alt="Gravitee Logo"></p>');
    });

    it('should render links', async () => {
      component.content = '[Links](https://gravitee.io)';
      fixture.detectChanges();

      expect(await harness.getRenderedHtml()).toContain('<p><a href="https://gravitee.io">Links</a></p>');
    });

    it('should render anchor', async () => {
      component.content = '[anchor](#anchor)';
      fixture.detectChanges();

      expect(await harness.getRenderedHtml()).toContain('<p><a class="anchor" href="#anchor">anchor</a></p>');
    });
  });

  describe('Custom components rendering', () => {
    it('should render grid', async () => {
      component.content = '<gmd-grid columns="2">Grid content</gmd-grid>';
      fixture.detectChanges();

      const renderedHtml = await harness.getRenderedHtml();
      expect(renderedHtml).toContain('grid');
      expect(renderedHtml).toContain('Grid content');
    });

    it('should render cell', async () => {
      component.content = '<gmd-cell>Cell content</gmd-cell>';
      fixture.detectChanges();

      const renderedHtml = await harness.getRenderedHtml();
      expect(renderedHtml).toContain('cell');
      expect(renderedHtml).toContain('Cell content');
    });
  });
});

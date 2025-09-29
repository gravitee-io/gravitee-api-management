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

  describe('DOM sanitization', () => {
    const xssAttackCases = [
      {
        name: 'script tag injection',
        payload: '<script>alert(123)</script>',
        expectedToBeSanitized: true,
      },
      {
        name: 'img onerror injection',
        payload: '<img src=a onerror=alert(document.domain)>',
        expectedToBeSanitized: true,
      },
      {
        name: 'javascript: protocol in href',
        payload: '<a href="javascript:alert(1)">Click me</a>',
        expectedToBeSanitized: true,
      },
      {
        name: 'onclick event handler',
        payload: '<div onclick="alert(1)">Click me</div>',
        expectedToBeSanitized: true,
      },
      {
        name: 'onload event handler',
        payload: '<img src="x" onload="alert(1)">',
        expectedToBeSanitized: true,
      },
      {
        name: 'iframe with javascript src',
        payload: '<iframe src="javascript:alert(1)"></iframe>',
        expectedToBeSanitized: true,
      },
      {
        name: 'form with javascript action',
        payload: '<form action="javascript:alert(1)"><input type="submit"></form>',
        expectedToBeSanitized: true,
      },
      {
        name: 'object with javascript data',
        payload: '<object data="javascript:alert(1)"></object>',
        expectedToBeSanitized: true,
      },
      {
        name: 'embed with javascript src',
        payload: '<embed src="javascript:alert(1)">',
        expectedToBeSanitized: true,
      },
      {
        name: 'svg with script',
        payload: '<svg><script>alert(1)</script></svg>',
        expectedToBeSanitized: true,
      },
      {
        name: 'data URI with javascript',
        payload: '<iframe src="data:text/html,<script>alert(1)</script>"></iframe>',
        expectedToBeSanitized: true,
      },
      {
        name: 'legitimate content should not be sanitized',
        payload: '<div>Hello World</div>',
        expectedToBeSanitized: false,
      },
      {
        name: 'legitimate img tag should not be sanitized',
        payload: '<img src="https://example.com/image.jpg" alt="Test">',
        expectedToBeSanitized: false,
      },
      {
        name: 'legitimate link should not be sanitized',
        payload: '<a href="https://example.com">Test Link</a>',
        expectedToBeSanitized: false,
      },
    ];

    xssAttackCases.forEach(({ name, payload, expectedToBeSanitized }) => {
      it(`should ${expectedToBeSanitized ? 'sanitize' : 'preserve'} ${name}`, async () => {
        const content = `<div>Hello</div>${payload}<div>World</div>`;
        component.content = content;
        fixture.detectChanges();

        const renderedHtml = await harness.getRenderedHtml();

        if (expectedToBeSanitized) {
          // The malicious payload should be sanitized/removed
          expect(renderedHtml).not.toContain(payload);
          expect(renderedHtml).toContain('<div>Hello</div>');
          expect(renderedHtml).toContain('<div>World</div>');
        } else {
          // Legitimate content should be preserved
          expect(renderedHtml).toContain(payload);
        }
      });
    });
  });
});

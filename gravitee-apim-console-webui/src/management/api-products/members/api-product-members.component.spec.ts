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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApiProductMembersComponent } from './api-product-members.component';

describe('ApiProductMembersComponent', () => {
  let fixture: ComponentFixture<ApiProductMembersComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiProductMembersComponent, MatIconTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductMembersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should render the Members section with title and description', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="api_product_members_title"]')?.textContent?.trim()).toBe('Members');
    expect(compiled.querySelector('[data-testid="api_product_members_description"]')?.textContent).toContain(
      'Manage who can interact with your API Product',
    );
  });

  it('should render picture, Name and Role column headers in the members table', async () => {
    const table = await loader.getHarness(MatTableHarness);
    const headerRows = await table.getHeaderRows();
    const cells = await headerRows[0].getCells();
    const headerTexts = await Promise.all(cells.map(c => c.getText()));
    expect(headerTexts).toEqual(['', 'Name', 'Role']);
  });

  it('should show an empty state when there are no members', async () => {
    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows.length).toBe(0);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.mat-mdc-no-data-row')?.textContent).toContain('No members found');
  });
});

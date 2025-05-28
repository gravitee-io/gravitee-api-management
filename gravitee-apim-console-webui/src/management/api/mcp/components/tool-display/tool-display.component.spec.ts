import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToolDisplayComponent } from './tool-display.component';
import {ToolDisplayHarness} from "./tool-display.harness";
import {TestbedHarnessEnvironment} from "@angular/cdk/testing/testbed";

describe('ToolDisplayComponent', () => {
  let fixture: ComponentFixture<ToolDisplayComponent>;
  let harness: ToolDisplayHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolDisplayComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToolDisplayComponent);

    fixture.componentRef.setInput('tool', {
      name: 'Test Tool',
      description: 'A test tool',
      inputSchema: { type: 'object' },
    });

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ToolDisplayHarness);
    fixture.detectChanges();
  });

  it('should display tool when provided', async () => {
    expect(await harness.getToolSchema()).toContain('Test Tool');
  });
});

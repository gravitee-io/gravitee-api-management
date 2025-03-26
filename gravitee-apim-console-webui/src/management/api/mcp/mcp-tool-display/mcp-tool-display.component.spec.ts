import { ComponentFixture, TestBed } from '@angular/core/testing';

import { McpToolDisplayComponent } from './mcp-tool-display.component';

describe('McpToolDisplayComponent', () => {
  let component: McpToolDisplayComponent;
  let fixture: ComponentFixture<McpToolDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [McpToolDisplayComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(McpToolDisplayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

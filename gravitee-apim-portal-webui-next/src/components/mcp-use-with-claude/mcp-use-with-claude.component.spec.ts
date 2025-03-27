import { ComponentFixture, TestBed } from '@angular/core/testing';

import { McpUseWithClaudeComponent } from './mcp-use-with-claude.component';

describe('McpUseWithClaudeComponent', () => {
  let component: McpUseWithClaudeComponent;
  let fixture: ComponentFixture<McpUseWithClaudeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [McpUseWithClaudeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(McpUseWithClaudeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

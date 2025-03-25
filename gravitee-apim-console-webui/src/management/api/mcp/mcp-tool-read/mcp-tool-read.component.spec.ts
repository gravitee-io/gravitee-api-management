import { ComponentFixture, TestBed } from '@angular/core/testing';

import { McpToolReadComponent } from './mcp-tool-read.component';

describe('McpToolReadComponent', () => {
  let component: McpToolReadComponent;
  let fixture: ComponentFixture<McpToolReadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [McpToolReadComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(McpToolReadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

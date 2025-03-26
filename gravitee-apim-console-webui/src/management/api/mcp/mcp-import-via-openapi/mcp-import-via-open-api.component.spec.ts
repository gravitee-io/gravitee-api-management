import { ComponentFixture, TestBed } from '@angular/core/testing';

import { McpImportViaOpenApiComponent } from './mcp-import-via-open-api.component';

describe('McpImportViaOpenapiComponent', () => {
  let component: McpImportViaOpenApiComponent;
  let fixture: ComponentFixture<McpImportViaOpenApiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [McpImportViaOpenApiComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(McpImportViaOpenApiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

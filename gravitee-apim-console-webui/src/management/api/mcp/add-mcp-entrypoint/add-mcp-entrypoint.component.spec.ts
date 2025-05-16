import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddMcpEntrypointComponent } from './add-mcp-entrypoint.component';

describe('AddMcpEntrypointComponent', () => {
  let component: AddMcpEntrypointComponent;
  let fixture: ComponentFixture<AddMcpEntrypointComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddMcpEntrypointComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddMcpEntrypointComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

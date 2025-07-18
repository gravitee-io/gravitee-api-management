import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GraviteeMarkdownViewerComponent } from './gravitee-markdown-viewer.component';

describe('GraviteeMarkdownViewerComponent', () => {
  let component: GraviteeMarkdownViewerComponent;
  let fixture: ComponentFixture<GraviteeMarkdownViewerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeMarkdownViewerComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeMarkdownViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render markdown content', () => {
    const testContent = '# Hello World\n\nThis is a **test** markdown content.';
    component.content = testContent;
    fixture.detectChanges();
    
    expect(component.renderedContent).toContain('<h1>Hello World</h1>');
    expect(component.renderedContent).toContain('<strong>test</strong>');
  });

  it('should handle empty content', () => {
    component.content = '';
    fixture.detectChanges();
    
    expect(component.renderedContent).toBe('');
  });
}); 
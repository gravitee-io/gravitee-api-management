import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { GraviteeMarkdownEditorComponent } from './gravitee-markdown-editor.component';

describe('GraviteeMarkdownEditorComponent', () => {
  let component: GraviteeMarkdownEditorComponent;
  let fixture: ComponentFixture<GraviteeMarkdownEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeMarkdownEditorComponent, FormsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeMarkdownEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle content changes', () => {
    const testContent = '# Hello World\n\nThis is a **test** markdown content.';
    component.onContentChange(testContent);
    
    expect(component.content).toBe(testContent);
  });

  it('should implement ControlValueAccessor', () => {
    const mockOnChange = jest.fn();
    const mockOnTouched = jest.fn();
    
    component.registerOnChange(mockOnChange);
    component.registerOnTouched(mockOnTouched);
    
    component.onContentChange('test content');
    
    expect(mockOnChange).toHaveBeenCalledWith('test content');
    expect(mockOnTouched).toHaveBeenCalled();
  });
}); 
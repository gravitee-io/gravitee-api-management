import { Component } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';

@Component({
  selector: 'openapi-editor',
  templateUrl: './openapi-editor.component.html',
  styleUrl: './openapi-editor.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: OpenApiEditorComponent,
      multi: true,
    },
  ],
  imports: [FormsModule, GioMonacoEditorModule],
})
export class OpenApiEditorComponent implements ControlValueAccessor {
  _value = '';
  private _disabled = false;

  _onChange: (value: string) => void = () => ({});
  _onTouched: () => void = () => ({});

  registerOnChange(fn: (value: string) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  writeValue(content: string): void {
    this._value = content ?? '';
  }

  get disabled(): boolean {
    return this._disabled;
  }

  setDisabledState(isDisabled: boolean): void {
    this._disabled = isDisabled;
  }
}

import { AbstractControl, ValidatorFn } from '@angular/forms';

function isNullOrEmpty(value: string | Array<string>) {
  if (Array.isArray(value)) {
    return value.filter((v) => v != null).length === 0;
  }
  return value == null || value.trim() === '';
}

export class GvValidators {

  static dateRange = (control: AbstractControl): { [key: string]: any } | null => {
    let error = null;
    if (control.value) {
      if (control.value.length !== 2) {
        error = { dateRangeError: { value: control.value } };
      } else {
        const from = control.value[0];
        const to = control.value[1];

        if ((from && !to) ||
          (!from && to) ||
          from && to && from === to) {
          error = { dateRangeError: { value: control.value } };
        }
      }
    }
    return error;
  };


  static oneRequired(field: AbstractControl): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const forbidden = isNullOrEmpty(control.value) && isNullOrEmpty(field.value);
      return forbidden ? { oneRequired: { value: control.value } } : null;
    };

  }

  static sameValueValidator(field: AbstractControl): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const forbidden = field.valid && field.value !== control.value;
      return forbidden ? { passwordError: { value: control.value } } : null;
    };
  }


}

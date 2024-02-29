import { FormBuilder, FormGroup, Validators } from '@angular/forms';

this.form = this.form.group({
  plan: [''],
  cacheName: [''],
  idleTime: [0, [Validators.pattern('^[0-9]*$')]],
  liveTime: [0, [Validators.pattern('^[0-9]*$')]],
  maxEntries: [''],
});

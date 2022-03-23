/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';

import {
  Application,
  ApplicationService,
  PermissionsService,
  ReferenceMetadata,
  ReferenceMetadataFormatType,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../../services/notification.service';

import '@gravitee/ui-components/wc/gv-table';

@Component({
  selector: 'app-application-metadata',
  templateUrl: './application-metadata.component.html',
  styleUrls: ['./application-metadata.component.css'],
})
export class ApplicationMetadataComponent implements OnInit {
  constructor(
    private applicationService: ApplicationService,
    private formBuilder: FormBuilder,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private permissionService: PermissionsService,
    private ref: ChangeDetectorRef,
  ) {
    this.resetAddMetadata();
    this.metadataToDelete = [];
  }

  hasCreatePermission = false;
  hasUpdatePermission = false;
  application: Application;
  formats: Array<ReferenceMetadataFormatType>;
  metadata: Array<ReferenceMetadata>;
  metadataToDelete: Array<ReferenceMetadata>;
  metadataOptions: any;
  tableTranslations: any[];

  addMetadataForm: FormGroup;
  updateMetadataForms: Record<string, FormGroup>;

  async ngOnInit() {
    this.application = this.route.snapshot.data.application;
    if (this.application) {
      await this.initPermissions();

      this.formats = Object.values(ReferenceMetadataFormatType);

      this.translateService
        .get([
          i18n('application.metadata.key'),
          i18n('application.metadata.name'),
          i18n('application.metadata.format'),
          i18n('application.metadata.value'),
          i18n('application.metadata.list.remove.title'),
          i18n('application.metadata.list.add.title'),
        ])
        .toPromise()
        .then(translations => {
          this.tableTranslations = Object.values(translations);
          this.metadataOptions = this._buildMetadataOptions();
          this.loadMetadataTable();
        });
    }
  }

  _buildMetadataOptions() {
    let data: any[] = [];
    if (this.canUpdate) {
      data = [
        ...data,
        this._renderKey(this.tableTranslations[0]),
        this._renderName(this.tableTranslations[1]),
        this._renderFormat(this.tableTranslations[2]),
        this._renderValue(this.tableTranslations[3]),
        this._renderAction(this.tableTranslations[4], this.tableTranslations[5]),
      ];
    } else {
      data = [
        { field: 'key', label: this.tableTranslations[0] },
        { field: 'name', label: this.tableTranslations[1] },
        { field: 'format', label: this.tableTranslations[2] },
        { field: 'value', label: this.tableTranslations[3] },
      ];
    }

    return {
      data,
    };
  }

  _isAddFormLine(line) {
    return this.hasCreatePermission && line._new === true && line.key == null;
  }

  _isNewLine(line) {
    return this.hasCreatePermission && line._new === true && line.key != null;
  }

  _renderAction(removeLabel: string, addLabel: string) {
    return {
      type: 'gv-button',
      width: '140px',
      attributes: {
        onClick: (item, event, target) => (this._isAddFormLine(item) ? this.addMetadata(item, target) : this.removeMetadata(item, target)),
        innerHTML: item => {
          return this._isAddFormLine(item) ? addLabel : removeLabel;
        },
        danger: item => !this._isAddFormLine(item),
        outlined: true,
        disabled: item => this._isAddFormLine(item) && this._disabledNewLine(item),
        icon: item => (this._isAddFormLine(item) ? 'code:plus' : 'home:trash'),
      },
    };
  }

  _renderKey(keyLabel: any) {
    return {
      field: 'key',
      label: keyLabel,
      style: item => {
        if (this._isNewLine(item)) {
          return 'visibility: hidden;';
        }
        return '';
      },
    };
  }

  _renderName(nameLabel: any) {
    return {
      field: 'name',
      label: nameLabel,
      type: item => (this._isAddFormLine(item) ? 'gv-input' : 'div'),
      attributes: {
        placeholder: 'Nom de la donnée',
        required: true,
        innerText: item => (this._isAddFormLine(item) ? '' : item.name),
        'ongv-input:input': this._onInput.bind(this),
      },
    };
  }

  _renderFormat(formatLabel: any) {
    return {
      field: 'format',
      label: formatLabel,
      type: 'gv-select',
      format: (v: string) => v.toUpperCase(),
      attributes: {
        options: this.formats,
        'ongv-select:select': item => {
          item.value = null;
          if (this._isAddFormLine(item)) {
            this.addMetadataForm.get('value').setValidators(this.getValidators(item));
            this.addMetadataForm.get('value').setValue(this._getDefaultValue(item));
            this.addMetadataForm.get('format').setValue(item.format);
          } else {
            const form = this._getUpdateForm(item);
            form.get('value').setValidators(this.getValidators(item));
            form.patchValue(item);
          }
          this.metadataOptions = this._buildMetadataOptions();
          this.ref.detectChanges();
        },
      },
    };
  }

  _getUpdateForm(metadata: ReferenceMetadata): FormGroup {
    if (metadata.key == null) {
      metadata.key = `${new Date().getTime()}`;
      this.updateMetadataForms[metadata.key] = this.formBuilder.group({
        name: new FormControl(metadata.name, Validators.required),
        format: new FormControl(metadata.format, Validators.required),
        value: new FormControl(metadata.value, Validators.required),
        new: new FormControl(true),
      });
    } else if (this.updateMetadataForms[metadata.key] == null) {
      this.updateMetadataForms[metadata.key] = this.formBuilder.group({
        name: new FormControl(metadata.name, Validators.required),
        format: new FormControl(metadata.format, Validators.required),
        value: new FormControl(metadata.value, Validators.required),
      });
    }
    return this.updateMetadataForms[metadata.key];
  }

  _removeUpdateForm(metadata: ReferenceMetadata) {
    if (metadata.key != null && this.updateMetadataForms[metadata.key] != null) {
      delete this.updateMetadataForms[metadata.key];
    }
  }

  _onInput(item, event, target) {
    if (this._isAddFormLine(item)) {
      this.addMetadataForm.patchValue(item);
      target.parentElement.parentElement.querySelector('gv-button').disabled = this.addMetadataForm.invalid;
    } else {
      this._getUpdateForm(item).patchValue(item);
      this.ref.detectChanges();
    }
  }

  _onChecked(item, event, target) {
    if (target.hasAttribute('checked')) {
      item.value = 'true';
    } else {
      item.value = 'false';
    }
    this._onInput(item, event, target);
  }

  private _onDatePicked(item, event, target) {
    if (target.value) {
      item.value = this.formatDate(target.value);
    } else {
      item.value = null;
    }
    this._onInput(item, event, target);
  }

  private getValidators(item): ValidatorFn[] {
    if (item.format.toUpperCase() === 'BOOLEAN') {
      return [];
    }
    const validators = [Validators.required];
    if (item.format.toUpperCase() === 'MAIL') {
      validators.push(Validators.email);
    } else if (item.format.toUpperCase() === 'URL') {
      validators.push(Validators.pattern('(https?://.w*)(:\\d*)?\\/?(.*)'));
    }
    return validators;
  }

  private _getDefaultValue(item: any) {
    if (item.format.toUpperCase() === 'BOOLEAN') {
      return false;
    }
    return null;
  }

  private _renderValue(valueLabel: any) {
    return {
      field: 'value',
      label: valueLabel,
      type: item => {
        switch (item.format.toUpperCase()) {
          case 'BOOLEAN':
            return 'gv-checkbox';
          case 'DATE':
            return 'gv-date-picker';
          default:
            return 'gv-input';
        }
      },
      attributes: {
        value: item => {
          if (item.format.toUpperCase() === 'DATE' && item.value != null) {
            return Date.parse(item.value);
          }
          return item.value;
        },
        required: item => item.format.toUpperCase() !== 'BOOLEAN',
        'ongv-input:input': this._onInput.bind(this),
        'ongv-date-picker:input': this._onDatePicked.bind(this),
        'ongv-checkbox:input': this._onChecked.bind(this),
        checked: item => {
          if (item.format.toUpperCase() === 'BOOLEAN') {
            return item.value === true || item.value === 'true' ? true : false;
          }
          return false;
        },
        type: item => {
          if (item.format.toUpperCase() === 'MAIL') {
            return 'email';
          } else if (item.format.toUpperCase() === 'NUMERIC') {
            return 'number';
          } else if (item.format.toUpperCase() === 'URL') {
            return 'url';
          } else if (item.format.toUpperCase() === 'STRING') {
            return 'text';
          }
          return null;
        },
      },
    };
  }

  async initPermissions() {
    const permissions = await this.permissionService.getCurrentUserPermissions({ applicationId: this.application.id }).toPromise();

    if (permissions) {
      const metadataPermissions = permissions.METADATA;
      if (metadataPermissions && metadataPermissions.length > 0) {
        this.hasCreatePermission = metadataPermissions.includes('C');
        this.hasUpdatePermission = metadataPermissions.includes('U');
      }
    }
  }

  loadMetadataTable() {
    return this.applicationService
      .getMetadataByApplicationId({ applicationId: this.application.id, size: -1 })
      .toPromise()
      .then(metadataResponse => {
        this.updateMetadataForms = {};
        if (this.hasCreatePermission) {
          // eslint-disable-next-line @typescript-eslint/ban-ts-comment
          // @ts-ignore
          this.metadata = [{ _new: true, format: 'STRING', name: '', value: '', key: null }, ...metadataResponse.data];
        } else {
          this.metadata = metadataResponse.data;
        }
      });
  }

  resetAddMetadata() {
    this.addMetadataForm = this.formBuilder.group({
      name: new FormControl(null, Validators.required),
      format: new FormControl('STRING', Validators.required),
      value: new FormControl(null, Validators.required),
    });
  }

  removeMetadata(metadata: ReferenceMetadata, target: Element) {
    target.setAttribute('loading', 'true');
    this._removeUpdateForm(metadata);
    this.metadata = this.metadata.filter(m => m.key !== metadata.key);
    if (!this._isNewLine(metadata)) {
      this.metadataToDelete.push(metadata);
    }
    this.ref.detectChanges();
    target.removeAttribute('loading');
  }

  addMetadata(metadata: ReferenceMetadata, target: Element) {
    if (this.addMetadataForm.valid) {
      target.setAttribute('loading', 'true');
      const metadataToAdd = { ...metadata, ...this.addMetadataForm.getRawValue() };
      metadata.value = null;
      metadata.name = null;
      metadata.format = 'STRING';
      this.metadata = [...this.metadata, metadataToAdd];
      this._getUpdateForm(metadataToAdd).patchValue(metadataToAdd);
      this.resetAddMetadata();
      this.ref.detectChanges();
      target.removeAttribute('loading');
    }
  }

  formatDate(date) {
    if (date != null) {
      return new Date(date).toISOString().split('T')[0];
    }
    return null;
  }

  updateMetadata(event) {
    if (this.canUpdate()) {
      event.target.loading = true;
      const formKeys = Object.keys(this.updateMetadataForms);

      const updatePromises: Promise<any>[] = formKeys.map(metadataId => {
        const form = this.updateMetadataForms[metadataId];

        if (form.get('new')) {
          form.removeControl('new');
          return this.applicationService
            .createApplicationMetadata({
              applicationId: this.application.id,
              referenceMetadataInput: this.updateMetadataForms[metadataId].getRawValue(),
            })
            .toPromise();
        } else {
          return this.applicationService
            .updateApplicationMetadataByApplicationIdAndMetadataId({
              applicationId: this.application.id,
              metadataId,
              referenceMetadataInput: this.updateMetadataForms[metadataId].getRawValue(),
            })
            .toPromise();
        }
      });

      const deletePromises: Promise<any>[] = this.metadataToDelete.map(metadata => {
        return this.applicationService
          .deleteApplicationMetadata({
            applicationId: this.application.id,
            metadataId: metadata.key,
          })
          .toPromise();
      });

      Promise.all([...updatePromises, ...deletePromises])
        .then(() => {
          this.metadataToDelete = [];
          return this.loadMetadataTable();
        })
        .then(() => {
          this.notificationService.success(i18n('application.metadata.update.success'));
        })
        .finally(() => {
          event.target.loading = false;
        });
    }
  }

  canUpdate() {
    if (this.metadataToDelete.length > 0) {
      return true;
    }
    if (this.updateMetadataForms) {
      const forms = Object.values(this.updateMetadataForms);
      return this.hasUpdatePermission && forms.length > 0 && forms.find(form => form.invalid) == null;
    }
    return false;
  }

  canReset() {
    if (this.metadataToDelete.length > 0) {
      return true;
    }
    if (this.updateMetadataForms) {
      const forms = Object.values(this.updateMetadataForms);
      return this.hasUpdatePermission && forms.length > 0;
    }
    return false;
  }

  hasMetadata() {
    return this.metadata != null && this.metadata.length > 0;
  }

  private _disabledNewLine(_: any) {
    return this.addMetadataForm.invalid;
  }

  reset() {
    return this.loadMetadataTable();
  }
}

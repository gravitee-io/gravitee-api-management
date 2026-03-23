import type { WidgetProps, FieldTemplateProps, ArrayFieldTemplateProps, ObjectFieldTemplateProps } from '@rjsf/utils';
import { Input } from '@baros/components/ui/input';
import { Label } from '@baros/components/ui/label';
import { Select } from '@baros/components/ui/select';
import { Button } from '@baros/components/ui/button';
import { Plus } from 'lucide-react';
import type { ThemeProps } from '@rjsf/core';

function TextWidget({ id, value, required, disabled, readonly, onChange, label }: WidgetProps) {
  return (
    <Input
      id={id}
      value={value ?? ''}
      required={required}
      disabled={disabled || readonly}
      onChange={(e) => onChange(e.target.value)}
      placeholder={label}
    />
  );
}

function SelectWidget({ id, value, required, disabled, readonly, onChange, options }: WidgetProps) {
  const { enumOptions } = options;
  return (
    <Select
      id={id}
      value={value ?? ''}
      required={required}
      disabled={disabled || readonly}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">Select...</option>
      {(enumOptions as Array<{ value: string; label: string }> | undefined)?.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </Select>
  );
}

function CheckboxWidget({ id, value, disabled, readonly, onChange, label }: WidgetProps) {
  return (
    <div className="flex items-center gap-2">
      <input
        type="checkbox"
        id={id}
        checked={value ?? false}
        disabled={disabled || readonly}
        onChange={(e) => onChange(e.target.checked)}
        className="h-4 w-4 rounded border border-input"
      />
      {label && (
        <Label htmlFor={id} className="font-normal">
          {label}
        </Label>
      )}
    </div>
  );
}

function FieldTemplate({ id, label, required, children, errors, description, schema }: FieldTemplateProps) {
  if (schema.type === 'object' || schema.type === 'array') {
    return <div>{children}</div>;
  }
  return (
    <div className="mb-4">
      {label && schema.type !== 'boolean' && (
        <Label htmlFor={id} className="mb-1.5 block">
          {label}
          {required && <span className="text-destructive ml-0.5">*</span>}
        </Label>
      )}
      {description && <p className="text-xs text-muted-foreground mb-1.5">{description}</p>}
      {children}
      {errors}
    </div>
  );
}

function ObjectFieldTemplate({ title, properties, description }: ObjectFieldTemplateProps) {
  return (
    <div className="space-y-1">
      {title && <h4 className="text-sm font-medium mb-2">{title}</h4>}
      {description && <p className="text-xs text-muted-foreground mb-2">{description}</p>}
      {properties.map((prop) => (
        <div key={prop.name}>{prop.content}</div>
      ))}
    </div>
  );
}

function ArrayFieldTemplate({ title, items, canAdd, onAddClick }: ArrayFieldTemplateProps) {
  return (
    <div className="mb-4">
      {title && <Label className="mb-1.5 block">{title}</Label>}
      <div className="space-y-2">
        {items.map((item, index) => (
          <div key={index} className="rounded-md border border-input p-3">
            {item}
          </div>
        ))}
      </div>
      {canAdd && (
        <Button type="button" variant="outline" size="sm" className="mt-2" onClick={onAddClick}>
          <Plus className="h-4 w-4 mr-1" />
          Add
        </Button>
      )}
    </div>
  );
}

export const barosTheme: ThemeProps = {
  widgets: {
    TextWidget,
    SelectWidget,
    CheckboxWidget,
  },
  templates: {
    FieldTemplate,
    ObjectFieldTemplate,
    ArrayFieldTemplate,
  },
};

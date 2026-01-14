# Select Component

Dropdown select input with label, validation, and error display. It is designed for GMD forms and can emit field state when `fieldKey` is provided.

## Usage

### Basic

```html
<gmd-select name="plan" label="Plan" options="Basic,Pro,Enterprise"></gmd-select>
```

### Required

```html
<gmd-select name="country" label="Country" required="true" options="US,CA,UK"></gmd-select>
```

### Field key for form state

```html
<gmd-select name="plan" label="Plan" fieldKey="plan" required="true" options="Basic,Pro,Enterprise"></gmd-select>
```

## Properties

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `fieldKey` | `string \| undefined` | `undefined` | Key used to emit form state events. |
| `name` | `string` | `''` | Select name and id. |
| `label` | `string \| undefined` | `undefined` | Label shown above the select. |
| `value` | `string \| undefined` | `undefined` | Initial value. |
| `required` | `boolean` | `false` | Marks the field as required. |
| `options` | `string` | `''` | Comma-separated values or JSON array string. |
| `disabled` | `boolean` | `false` | Disables the select. |

## Options format

- Comma-separated: `"Basic,Pro,Enterprise"`
- JSON array: `'["Basic","Pro","Enterprise"]'`

## Theming

```scss
@use '@gravitee/gravitee-markdown' as gmd;

.my-scope {
  @include gmd.select-overrides((
    // Label styling
    label-text-color: #111827,
    label-text-size: 0.875rem,
    label-text-weight: 500,

    // Required indicator
    required-text-color: #dc2626,

    // Field styling
    field-text-color: #111827,
    field-text-size: 1rem,
    field-outline-width: 1px,
    field-outline-color: #94a3b8,
    field-outline-radius: 0.375rem,
    field-background-color: #ffffff,
    field-focus-outline-color: #2563eb,
    field-disabled-background-color: #f5f5f5,

    // Error messages
    error-text-color: #dc2626,
    error-text-size: 0.8125rem,
  ));
}
```

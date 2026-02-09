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

| Property   | Type                  | Default     | Description                                  |
| ---------- | --------------------- | ----------- | -------------------------------------------- |
| `fieldKey` | `string \| undefined` | `undefined` | Key used to emit form state events.          |
| `name`     | `string`              | `''`        | Select name and id.                          |
| `label`    | `string \| undefined` | `undefined` | Label shown above the select.                |
| `value`    | `string \| undefined` | `undefined` | Initial value.                               |
| `required` | `boolean`             | `false`     | Marks the field as required.                 |
| `options`  | `string`              | `''`        | Comma-separated values or JSON array string. |
| `disabled` | `boolean`             | `false`     | Disables the select.                         |

## Options format

- Comma-separated: `"Basic,Pro,Enterprise"`
- JSON array: `'["Basic","Pro","Enterprise"]'`

## Theming

```scss
@use '@gravitee/gravitee-markdown' as gmd;

.my-scope {
  @include gmd.select-overrides(
    (
      // Label styling
      outlined-label-text-color: #111827,
      outlined-label-text-size: 0.875rem,
      outlined-label-text-weight: 500,

      // Field styling
      outlined-input-text-color: #111827,
      container-text-size: 1rem,
      outlined-outline-width: 1px,
      outlined-outline-color: #94a3b8,
      outlined-container-shape: 0.375rem,
      outlined-container-color: #ffffff,
      outlined-focus-outline-color: #2563eb,
      outlined-disabled-container-color: #f5f5f5,

      // Error messages (also used for required indicator)
      error-text-color: #dc2626,
      subscript-text-size: 0.8125rem
    )
  );
}
```

# Radio Component

Radio group input with label, options, validation, and error display. It is designed for GMD forms and can emit field state when `fieldKey` is provided.

## Usage

### Basic

```html
<gmd-radio name="plan" label="Plan" options="Basic,Pro,Enterprise"></gmd-radio>
```

### Required

```html
<gmd-radio name="plan" label="Plan" required="true" options="Basic,Pro,Enterprise"></gmd-radio>
```

### Readonly

```html
<gmd-radio name="plan" label="Plan" value="Pro" readonly="true" options="Basic,Pro,Enterprise"></gmd-radio>
```

### Field key for form state

```html
<gmd-radio name="plan" label="Plan" fieldKey="plan" required="true" options="Basic,Pro,Enterprise"></gmd-radio>
```

## Properties

| Property   | Type                  | Default     | Description                                                   |
| ---------- | --------------------- | ----------- | ------------------------------------------------------------- |
| `fieldKey` | `string \| undefined` | `undefined` | Key used to emit form state events.                           |
| `name`     | `string`              | `''`        | Radio name and id.                                            |
| `label`    | `string \| undefined` | `undefined` | Label shown above the group.                                  |
| `value`    | `string \| undefined` | `undefined` | Initial value.                                                |
| `required` | `boolean`             | `false`     | Marks the field as required.                                  |
| `options`  | `string`              | `''`        | Comma-separated values or JSON array string.                  |
| `readonly` | `boolean`             | `false`     | Makes the radio group read-only (focusable, value submitted). |
| `disabled` | `boolean`             | `false`     | Disables the group.                                           |

## Options format

- Comma-separated: `"Basic,Pro,Enterprise"`
- JSON array: `'["Basic","Pro","Enterprise"]'`

## Theming

```scss
@use '@gravitee/gravitee-markdown' as gmd;

.my-scope {
  @include gmd.radio-overrides(
    (
      // Label styling
      outlined-label-text-color: #111827,
      outlined-label-text-size: 0.875rem,
      outlined-label-text-weight: 500,

      // Error messages (also used for required indicator)
      error-text-color: #dc2626,
      subscript-text-size: 0.8125rem
    )
  );
}
```

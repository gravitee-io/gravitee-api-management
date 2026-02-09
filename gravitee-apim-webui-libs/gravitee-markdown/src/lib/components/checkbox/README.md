# Checkbox Component

Single checkbox input with label, validation, and error display. It is designed for GMD forms and can emit field state when `fieldKey` is provided.

## Usage

### Basic

```html
<gmd-checkbox name="terms" label="Accept terms"></gmd-checkbox>
```

### Required

```html
<gmd-checkbox name="terms" label="Accept terms" required="true"></gmd-checkbox>
```

### Readonly

```html
<gmd-checkbox name="terms" label="Accept terms" value="true" readonly="true"></gmd-checkbox>
```

### Field key for form state

```html
<gmd-checkbox name="terms" label="Accept terms" fieldKey="terms" required="true"></gmd-checkbox>
```

## Properties

| Property   | Type                  | Default     | Description                                                |
| ---------- | --------------------- | ----------- | ---------------------------------------------------------- |
| `fieldKey` | `string \| undefined` | `undefined` | Key used to emit form state events.                        |
| `name`     | `string`              | `''`        | Checkbox name and id.                                      |
| `label`    | `string \| undefined` | `undefined` | Label shown next to the checkbox.                          |
| `value`    | `string \| undefined` | `undefined` | Initial value (`"true"` or `"false"`).                     |
| `required` | `boolean`             | `false`     | Marks the field as required.                               |
| `readonly` | `boolean`             | `false`     | Makes the checkbox read-only (focusable, value submitted). |
| `disabled` | `boolean`             | `false`     | Disables the checkbox.                                     |

## Theming

The checkbox component can be customized using SCSS overrides. Use the mixin to override available tokens:

```scss
@use '@gravitee/gravitee-markdown' as gmd;

.my-scope {
  @include gmd.checkbox-overrides(
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

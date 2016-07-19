ui-codemirror add-on
====================

This ui-codemirror add-on uses as the name implies the ui-codemirror plugin to
provide a CodeMirror editor for schema form. [codemirror](http://codemirror.net)
as well as [ui-codemirror](https://github.com/angular-ui/ui-codemirror) is used.

CodeMirror is highly customizable and this add-on takes an options object via
`codemirrorOptions` in the form. More info below at [Options](#Options).


## Installation

The editor is an add-on to the Bootstrap decorator. To use it, just include
`bootstrap-ui-codemirror.min.js` *after* `dist/bootstrap-decorator.min.js`.

Easiest way is to install is with bower, this will also include dependencies:
```bash
$ bower install angular-schema-form-ui-codemirror
```

You'll need to load a few additional files to use the editor:

**Be sure to load this projects files after you load angular schema form**

1. Angular
2. The [CodeMirror](http://codemirror.net) source file
3. The [ui-codemirror](https://github.com/angular-ui/ui-codemirror) source file
4. **Angular Schema Form**

### Example

```HTML
<script type="text/javascript" src="/bower_components/angular/angular.min.js"></script>
<script type="text/javascript" src="/bower_components/angular-sanitize/angular-sanitize.min.js"></script>
<script type="text/javascript" src="/bower_components/codemirror/lib/codemirror.js"></script>
<script type="text/javascript" src="/bower_components/angular-ui-codemirror/ui-codemirror.js"></script>

<script type="text/javascript" src="/bower_components/angular-schema-form/schema-form.min.js"></script>
<script type="text/javascript" src="/bower_components/angular-schema-form-ui-codemirror/bootstrap-ui-codemirror.js"></script>

```

## Usage

The codemirror add-on adds a new form type, `codemirror`.

|  Form Type     |   Becomes            |
|:---------------|:--------------------:|
|  codemirror    |  a CodeMirror widget |



## Options

The `codemirror` field takes one option, `codemirrorOptions`. This is an object
with any and all options availible to CodeMirror. A full list of these can be
found [here](http://codemirror.net/doc/manual.html#config).


### Example

```javascript
{
  "key": "content",
  "type": "codemirror",
  "codemirrorOptions": {
    "lineWrapping": true,
    "lineNumbers": true,
    "mode": "xml"
  }
}
```

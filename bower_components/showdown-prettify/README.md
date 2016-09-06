Showdown's Prettify Extension
==========================

[![Build Status](https://travis-ci.org/showdownjs/prettify-extension.svg)](https://travis-ci.org/showdownjs/prettify-extension) [![npm version](https://badge.fury.io/js/showdown-prettify.svg)](http://badge.fury.io/js/showdown-prettify) [![npm version](https://badge.fury.io/bo/showdown-prettify.svg)](http://badge.fury.io/bo/showdown-prettify)

------

**An extension to add [Google Prettify](http://code.google.com/p/google-code-prettify/) hints to [showdown](https://github.com/showdownjs/showdown)'s HTML output**


## Installation

### With [npm](http://npmjs.org)

    npm install showdown-prettify

### With [bower](http://bower.io/)

    bower install showdown-prettify

### Manual

You can also [download the latest release zip or tarball](https://github.com/showdownjs/prettify-extension/releases) and include it in your webpage, after showdown:

    <script src="showdown.min.js">
    <script src="showdown-prettify.min.js">

### Enabling the extension

After including the extension in your application, you just need to enable it in showdown.

    var converter = new showdown.Converter({extensions: ['prettify']});

## Example

```javascript
var converter = new showdown.Converter({extensions: ['prettify']}),
    input = "Here's a simple hello world in javascript:\n" +
            "\n" +
            "    alert('Hello World!');\n" +
            "\n" +
            "The `alert` function is a build-in global from `window`.";
    html = converter.makeHtml(input);
    console.log(html);
```

This should output the equivalent to:

```html
<p>Here's a simple hello world in javascript:</p>

<pre class="prettyprint linenums" tabIndex="0"><code data-inner="1">alert('Hello World!');
</code></pre>

<p>The <code class="prettyprint">alert</code> function is a build-in global from <code class="prettyprint">window</code>.</p>
```

## License
These files are distributed under BSD license. For more information, 
please check the [LICENSE file](https://github.com/showdownjs/prettify-extension/blob/master/LICENSE) in the source code.

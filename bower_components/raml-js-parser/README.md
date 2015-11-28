# RAML Parser
[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/raml-org/raml-js-parser?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/raml-org/raml-js-parser.png)](https://travis-ci.org/raml-org/raml-js-parser)
[![Dependency Status](https://david-dm.org/raml-org/raml-js-parser.png)](https://david-dm.org/raml-org/raml-js-parser)

This project contains a RAML parser capable (at this moment) of parsing version 0.8
of the [RAML specification](http://raml.org/spec.html). The parser is written in CoffeeScript and its capable
of running inside NodeJS as well as in-browser.

## Versioning

The RAML parser is versioned in the following manner:

```
x.y.z
```

in which *x.y* denotes the version of the [RAML specification](http://raml.org/spec.html)
and *z* is the version of the parser.

So *0.1.2* is the 2nd revision of the parser for the *0.1* version
of the [RAML specification](http://raml.org/spec.html).

### Contributing
If you are interested in contributing some code to this project, thanks! Please submit a [Contributors Agreement](https://api-notebook.anypoint.mulesoft.com/notebooks#bc1cf75a0284268407e4) acknowledging that you are transferring ownership.

To discuss this project, please use its [github issues](https://github.com/raml-org/raml-js-parser/issues) or the [RAML forum](http://forums.raml.org/).

## Usage for NodeJS
  
### Load

Loading a RAML file is as easy as follows:

```javascript
  var raml = require('raml-parser');
  
  raml.loadFile('myAPI.raml').then( function(data) {
    console.log(data);
  }, function(error) {
    console.log('Error parsing: ' + error);
  });
```

You can alternatively load from a string containing the api definition:

```javascript
  var raml = require('raml-parser');
  
  var definition = [
    '#%RAML 0.8',
    '---',
    'title: MyApi',
    'baseUri: http://myapi.com',
    '/Root:'
  ].join('\n');
  
  raml.load(definition).then( function(data) {
    console.log(data);
  }, function(error) {
    console.log('Error parsing: ' + error);
  });
```

The shape of the returned object is (unofficially) documented in this [Typescript interface](https://github.com/aldonline/raml-typescript).

### Abstract Syntax Tree

Generating an AST from a RAML file is as easy as follows:

```javascript
  var raml = require('raml-parser');
  
  var myAPI;
  raml.composeFile('myAPI.raml').then( function(rootNode) {
    console.log('Root Node: ' + rootNode)
  }, function(error) {
    console.log('Error parsing: ' + error);
  });
```

you can also alternatively generate an AST from a string containing the api definition:

```javascript
  var raml = require('raml-parser');
  
  var definition = [
    '#%RAML 0.8',
    '---',
    'title: MyApi',
    'baseUri: http://myapi.com',
    '/Root:'
  ].join('\n');
  
  raml.compose(definition).then( function(rootNode) {
    console.log('Root Node: ' + rootNode)
  }, function(error) {
    console.log('Error parsing: ' + error);
  });
```

## Usage for In-Browser

Using the RAML parser from inside the browser requires the user to actually
include the RAML javascript file in a script tag as follows:

```html
<script src="raml-parser.min.js"></script>
```

from there on the usage is pretty much the same as NodeJS, the script
defines a *RAML.Parser* object globally which can be used as follows:

```javascript
RAML.Parser.loadFile('http://localhost:9001/myAPI.raml').then( function(data) {
  console.log(data)
}, function(error) {
  console.log('Error parsing: ' + error);
});
```

Notice that the in-browser version can fetch remote API definitions via XHR.

//
//  DocHelper Extension
//  !> strike-through   ->  <p class="tip">strike-through</del>
//

(function (extension) {
    'use strict';
  
    // UML - Universal Module Loader
    // This enables the extension to be loaded in different environments
    if (typeof showdown !== 'undefined') {
      // global (browser or nodejs global)
      extension(showdown);
    } else if (typeof define === 'function' && define.amd) {
      // AMD
      define(['showdown'], extension);
    } else if (typeof exports === 'object') {
      // Node, CommonJS-like
      module.exports = extension(require('showdown'));
    } else {
      // showdown was not found so we throw
      throw Error('Could not find showdown library');
    }
  
  }(function (showdown) {
    'use strict';
  
    showdown.extension('docHelper', function() {
      'use strict';
  
      return [
        {
            type:    'lang',
            regex:   '(!){2}(>){1}',
            replace: function (match, prefix, content) {
              return '<p class="box alert">';
            }
        },
        {
          type:    'lang',
          regex:   '(!){1}(>){1}',
          replace: function (match, prefix, content) {
            return '<p class="box warn">';
          }
        },
        {
            type:    'lang',
            regex:   '(\\?){1}(>){1}',
            replace: function (match, prefix, content) {
              return '<p class="box tip">';
            }
        }
      ];
    });
  }));
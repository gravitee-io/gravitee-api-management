describe('v-pane-header directive', function () {

  var $compile;
  var $rootScope;
  var accordionConfig;
  var scope;

  var generateTemplate = function (options) {
    var dafaults = {
      transcludedContent: ''
    };

    if (options) {
      angular.extend(dafaults, options);
    }

    var template = '<v-accordion>\n';
        template += '<v-pane>\n';
        template += '<v-pane-header>' + dafaults.transcludedContent + '</v-pane-header>\n';
        template += '<v-pane-content></v-pane-content>\n';
        template += '</v-pane>\n';
        template += '</v-accordion>';

    return template;
  };



  beforeEach(module('vAccordion'));

  beforeEach(inject(function (_$rootScope_, _$compile_, _accordionConfig_) {
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    $compile = _$compile_;
    accordionConfig = _accordionConfig_;
  }));

  beforeEach(function() {
    this.addMatchers({
      toBeActive: function (checkFocus) {
        var fails = [];
        var actual = this.actual;
        this.message = function() {
          return 'Expected ' + angular.mock.dump(actual) + (this.isNot ? ' not ' : ' ') + 
            'to be the active tab. Failures: ' + fails.join(', ');
        };

        if (actual.attr('aria-selected') != 'true') {
          fails.push('aria-selected is not true');
        } 
        if (actual.attr('tabindex') != '0') {
          fails.push('tabindex is not 0');
        }
        return fails.length === 0;
      }
    });
  });

  afterEach(function () {
    scope.$destroy();
  });
  


  it('should throw an error if `v-pane` directive controller can\'t be found', function () {
    var template = '<v-pane-header></v-pane-header>';

    expect(function () { $compile(template)(scope); }).toThrow();
  });


  it('should transclude scope and create inner `div` wrapper', function () {
    var message = 'Hello World!';

    var template = generateTemplate({ transcludedContent: '{{ message }}' });

    var accordion = $compile(template)(scope);
    var paneHeader = accordion.find('v-pane-header');

    scope.message = message;
    scope.$digest();

    expect(paneHeader.html()).toContain(message);
    expect(paneHeader.html()).toContain('</div>');
  });


  it('should have `role` and `tabindex` attribute', function () {
    var template = generateTemplate();

    var accordion = $compile(template)(scope);
    var paneHeader = accordion.find('v-pane-header');

    expect(paneHeader.attr('role')).toBe('tab');
    expect(paneHeader.attr('tabindex')).toBe('-1');
  });


  it('should toggle the pane on click', function () {
    var template =  generateTemplate();

    var accordion = $compile(template)(scope);
    var pane = accordion.find('v-pane');
    var paneHeader = accordion.find('v-pane-header');

    var paneIsolateScope = pane.isolateScope();
        paneIsolateScope.$digest();

    expect(paneIsolateScope.isExpanded).toBe(false);
    expect(paneHeader.attr('aria-selected')).toBe('false');
    expect(paneHeader.attr('tabindex')).toBe('-1');

    paneHeader.click();

    expect(paneIsolateScope.isExpanded).toBe(true);
    expect(paneHeader.attr('aria-selected')).toBe('true');
    expect(paneHeader.attr('tabindex')).toBe('0');
  });

});
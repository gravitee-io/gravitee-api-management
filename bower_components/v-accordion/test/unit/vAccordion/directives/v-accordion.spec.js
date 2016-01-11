describe('v-accordion directive', function () {

  var $compile;
  var $rootScope;
  var accordionConfig;
  var scope;

  var generateTemplate = function (options) {
    var dafaults = {
      allowMultiple: false,
      customControl: false,
      expandCb: false,
      collapseCb: false,
      transcludedContent: ''
    };

    if (options) {
      angular.extend(dafaults, options);
    }

    var template = '<v-accordion';
        template += (dafaults.allowMultiple) ? ' multiple' : '';
        template += (dafaults.customControl) ? ' control="customControl"' : '';
        template += (dafaults.expandCb) ? ' onexpand="onExpand(index)"' : '';
        template += (dafaults.collapseCb) ? ' oncollapse="onCollapse(index)"' : '';
        template += '>\n';
        template += dafaults.transcludedContent;
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

  afterEach(function () {
    scope.$destroy();
  });



  it('should transclude scope', function () {
    var message = 'Hello World!';

    var template = generateTemplate({ transcludedContent: '{{ message }}' });
    var accordion = $compile(template)(scope);

    scope.message = message;
    scope.$digest();

    expect(accordion.html()).toContain(message);
  });


  it('should allow multiple selections if the `multiple` attribute is defined', function () {
    var template = generateTemplate({ allowMultiple: true });
    var accordion = $compile(template)(scope);

    expect(accordion.isolateScope().allowMultiple).toBe(true);
  });


  it('should add the ARIA `tablist` role', function () {
    var template = generateTemplate();
    var accordion = $compile(template)(scope);

    expect(accordion.attr('role')).toBe('tablist');
  });


  it('should set the `aria-multiselectable` attribute to `true` if `multiple` attribute is defined', function () {
    var template = generateTemplate({ allowMultiple: true });
    var accordion = $compile(template)(scope);

    expect(accordion.attr('aria-multiselectable')).toBe('true');
  });


  it('should extend custom control object', function () {
    scope.customControl = { someProperty: 'test' };

    var template = generateTemplate({ customControl: true });
    var accordion = $compile(template)(scope);

    expect(accordion.isolateScope().internalControl.someProperty).toBeDefined();
    expect(accordion.isolateScope().internalControl.someProperty).toBe('test');
  });


  it('should throw an error if the API method is overriden', function () {
    scope.customControl = { toggle: function () {} };

    var template = generateTemplate({ customControl: true });

    expect(function () { $compile(template)(scope) }).toThrow();
  });


  describe('controller', function () {

    var accordion;
    var isolateScope;
    var controller;

    var generatePanes = function (length) {
      var samplePanes = [];

      for (var i = 0; i < length; i++) {
        var samplePane = {
          header: 'Pane header #' + i,
          content: 'Pane content #' + i
        };

        samplePanes.push(samplePane);
      }

      return samplePanes;
    };



    beforeEach(function () {
      var options = {
        allowMultiple: true,
        customControl: true,
        expandCb: true,
        collapseCb: true
      };

      var template = generateTemplate( options );
      accordion = $compile(template)(scope);
      $rootScope.$digest();

      isolateScope = accordion.isolateScope();
      controller = accordion.controller('vAccordion');
    });



    it('should add new pane object to `panes` array', function () {
      var samplePane = generatePanes(1)[0];

      expect(isolateScope.panes.length).toBe(0);
      controller.addPane(samplePane);
      expect(isolateScope.panes.length).toBeGreaterThan(0);
    });


    it('should expand pane and call `onExpand` callback', function () {
      var samplePanes = generatePanes(5);
      var paneToExpandIndex = 0;
      var paneToExpand = samplePanes[paneToExpandIndex];

      for (var i = 0; i < samplePanes.length; i++) {
        controller.addPane(samplePanes[i]);
      };

      scope.onExpand = jasmine.createSpy('onExpand spy');
      scope.$digest();

      expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeFalsy();
      controller.expand(paneToExpand);
      expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeTruthy();

      expect(scope.onExpand).toHaveBeenCalled();
    });


    it('should collapse pane and call `onCollapse` callback', function () {
      var samplePanes = generatePanes(5);
      var paneToExpandIndex = 0;
      var paneToExpand = samplePanes[paneToExpandIndex];
          paneToExpand.isExpanded = true;

      for (var i = 0; i < samplePanes.length; i++) {
        controller.addPane(samplePanes[i]);
      };

      scope.onCollapse = jasmine.createSpy('onCollapse spy');
      scope.$digest();

      expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeTruthy();
      controller.collapse(paneToExpand);
      expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeFalsy();

      expect(scope.onCollapse).toHaveBeenCalled();
    });

  });

});
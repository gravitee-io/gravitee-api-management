addLegend: function(svg) {
  var items = svg.append("g")
    .attr("id", "n3-pie-legend");
  
  return this;
},

updateLegend: function(svg, data, dimensions, options) {
  if (options.mode === "gauge") {
    this.updateGaugeLegend(svg, data, dimensions, options);
  } else {
    this.updateRegularLegend(svg, data, dimensions, options);
  }
  
  return this;
},

updateRegularLegend: function(svg, data, dimensions, options) {
  var that = this;
  var radius = this.getRadius(dimensions);
  var availableWidth = radius - options.thickness;
  
  var legendHalfHeight = data.length*availableWidth/10;
  
  var scale = d3.scale.linear()
    .range([-legendHalfHeight, legendHalfHeight])
    .domain([0, data.length-1])
    .nice()
  
  
  var items = svg.selectAll("#n3-pie-legend")
    .selectAll(".legend-item")
      .data(data, function(d) {return d.label;});
  
  items.enter()
    .append("text")
      .classed("legend-item", true)
      .on("mouseover", this.onMouseOver(svg))
      .on("mouseout", this.onMouseOut(svg))
  
  items
    .text(this.getLegendLabelFunction(availableWidth))
    .attr({
      "text-anchor": "middle",
      "transform": function(d, i) {
        return "translate(0, " + scale(i) + ")";
      }
    })
    .style({
      "font-family": "monospace",
      "font-size": Math.max(12, availableWidth/10) + "px",
      "fill": function(d) {return d.color;},
      "fill-opacity": 0.8
    });

  items.exit().remove();
},

updateGaugeLegend: function(svg, data, dimensions, options) {
  var size = (this.getRadius(dimensions) - options.thickness)/2;
  
  if (size < 0) {
    return;
  }
  
  var legend = svg.selectAll("#n3-pie-legend");
  
  var title = legend.selectAll(".legend-title")
    .data(data.filter(function(s) {return !s.__isComplement;}));
  
  title.enter()
    .append("text")
    .attr({
      "class": "legend-title",
      "text-anchor": "middle",
      "y": -size/4 + "px"
    })
    .style({
      "font-size": Math.max(size/2, 12) + "px",
      "fill": function(d) {return d.color;},
      "fill-opacity": 0.8
    })
  ;
  
  title.text(function(d) {return d.label;});
  
  title.exit().remove();
  
  var value = legend.selectAll(".legend-value")
    .data(data.filter(function(s) {return !s.__isComplement;}));
  
  value.enter()
    .append("text")
    .attr({
      "class": "legend-value",
      "text-anchor": "middle",
      "y": size/1.5 + "px" 
    })
    .style({
      "font-size": size + "px",
      "fill": function(d) {return d.color;},
      "fill-opacity": 0.8
    })
  ;
  
  value
    .text(function(d) {return d.value + (d.suffix || '');});
  
  value.exit().remove();
},

getLegendLabelFunction: function(availableWidth) {
  var that = this;
  
  return function(datum, index) {
    if (availableWidth < 40) {
      return "";
    } else if (availableWidth < 100) {
      return datum.label;
    }
    
    return that.getLegendLabel(datum.label, datum.value, 20);
  }
},

onMouseOver: function(svg) {
  return function(datum, index) {
    var dimOrHighlight = function(element) {
      return element.transition().duration(50).style({
        "opacity": function(d, i) {return i === index ? "1" : "0.4";}
      });
    }
    
    dimOrHighlight(svg.selectAll(".legend-item"));
    dimOrHighlight(svg.selectAll(".arc"));
  };
},

onMouseOut: function(svg) {
  return function(datum, index) {
    var show = function(element) {
      return element.transition().duration(50).style({
        "opacity": 1
      });
    }
    
    show(svg.selectAll(".legend-item"));
    show(svg.selectAll(".arc"));
  };
},

getLegendLabel: function(label, value, totalLength) {
  var dots = [];
  for (var i = 0; i < totalLength; i++) {
    dots.push(".");
  }
  
  label = label + " ";
  value = " " + value;
  
  label.split("").forEach(function(c, i) {dots[i] = c;});
  value.split("").forEach(function(c, i) {dots[totalLength - value.length + i] = c;});
  
  return dots.join("");
}
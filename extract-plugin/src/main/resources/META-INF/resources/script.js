$(document)
        .ready(
                function() {
                  var url = window.location.href;
                  var qsplit = url.indexOf("?");
                  var hash = "";
                  if (qsplit) {
                    hash = url.substring(qsplit + 1);
                  }
                  var params = {};
                  var parts = hash.split("&");
                  for (var i = 0; i < parts.length; i++) {
                    var part = parts[i];
                    if (part) {
                      var pair = part.split("=");
                      params[pair[0]] = pair[1]
                              && decodeURIComponent(pair[1].replace(/\+/g, " "));
                    }
                  }

                  if (params.q) {
                    $("#q").val(params.q);
                  }

                  function filter(qpart) {
                    return function(e) {
                      return e.g.indexOf(qpart) != -1
                              || e.a.indexOf(qpart) != -1;
                    }
                  }

                  function filters(all) {
                    return function(e) {
                      for (var j = 0; j < all.length; j++) {
                        if (!all[j](e)) { return false; }
                      }
                      return true;
                    }
                  }

                  function buildGroup(e) {
                    var group = $("<a href='javascript:void(0)'>" + (e.g || e.groupId)
                            + "</a>");
                    group.click(function() {
                      params = {
                        groupId: e.g || e.groupId
                      };
                      update();
                    });
                    return group;
                  }

                  function buildArtifact(e) {
                    var artifact = $("<a href='javascript:void(0)'>" + (e.a || e.artifactId)
                            + "</a>");
                    artifact.click(function() {
                      params = {
                        groupId: e.g || e.groupId,
                        artifactId: e.a || e.artifactId
                      };
                      update();
                    });
                    return artifact;
                  }

                  function buildPage(i, page) {
                    var p = $("<a href='javascript:void(0)'>" + page + "</a>");
                    p.click(function() {
                      params.f = i;
                      update();
                    });
                    return p;
                  }

                  function buildVersion(e) {
                    return $("<a href='" + e.g + "-" + e.a + "-" + e.v
                            + ".html'>" + e.v + "</a>");
                  }

                  var pageSize = 20;

                  function displaytable(filters, upperVersion) {
                    var d = window.data;
                    var filtered = [];
                    var table;
                    for (var i = 0; i < d.length; i++) {
                      if (filters(d[i])) {
                        filtered.push(d[i]);
                      }
                    }

                    if (upperVersion) {
                      var map = {};
                      for (var i = 0; i < filtered.length; i++) {
                        var key = filtered[i].g + "@" + filtered[i].a;
                        var val = map[key];
                        if (!val || val.v < filtered[i].v) {
                          map[key] = filtered[i];
                        }
                      }
                      filtered = [];
                      for (k in map) {
                        filtered.push(map[k]);
                      }
                    }

                    filtered.sort(function(a, b) {
                      if (a.g !== b.g) { return a.g < b.g ? -1 : 1; }
                      if (a.a !== b.a) { return a.a < b.a ? -1 : 1; }
                      if (a.v !== b.v) { return a.v < b.v ? -1 : 1; }
                      return 0;
                    });

                    params.f = params.f || 0;
                    var count = filtered.length;
                    filtered = filtered.slice(params.f, Math.min(params.f
                            + pageSize, filtered.length));

                    for (var i = 0; i < filtered.length; i++) {
                      if (!table) {
                        var tableel = $("<table class='table'></table>");
                        table = $("<tbody>").appendTo(tableel);
                        table
                                .append($("<tr><th>groupId</th><th>artifactId</th><th>version</th></tr>"));
                        $("#content").append(tableel);
                      }
                      var tr = $("<tr>");
                      table.append(tr);
                      $("<td>").append(buildGroup(filtered[i])).appendTo(tr);
                      $("<td>").append(buildArtifact(filtered[i])).appendTo(tr);
                      $("<td>").append(buildVersion(filtered[i])).appendTo(tr);
                    }

                    if (count > pageSize) {
                      var nav = $(
                              "<nav aria-label=\"Page navigation\"><ul class=\"pagination\"></ul></nav>")
                              .appendTo("#content").find("ul");
                      if (params.f == 0) {
                        nav
                                .append($("<li class=\"disabled\"><a href='javascript:void(0)'><span aria-hidden=\"true\">&laquo;</span></a></li>"));
                      } else {
                        var a = $("<a href='javascript:void(0)'><span aria-hidden=\"true\">&laquo;</span></a>");
                        a.click(function() {
                          params.f = Math.max(params.f - pageSize, 0);
                          update();
                        });
                        $("<li>").append(a).appendTo(nav);
                      }
                      for (var i = 0; i < count; i += pageSize) {
                        var page = Math.floor((i / pageSize) + 1);
                        if (i == params.f) {
                          nav
                                  .append($("<li class=\"disabled\"><a href='javascript:void(0)'>"
                                          + page + "</a></li>"));
                        } else {
                          $("<li>").append(buildPage(i, page)).appendTo(nav);
                        }
                      }
                      if (count - params.f > pageSize) {
                        var a = $("<a href='javascript:void(0)'><span aria-hidden=\"true\">&raquo;</span></a>");
                        a.click(function() {
                          params.f = params.f + pageSize;
                          update();
                        });
                        $("<li>").append(a).appendTo(nav);
                      } else {
                        nav
                                .append($("<li class=\"disabled\"><a href='javascript:void(0)'><span aria-hidden=\"true\">&raquo;</span></a></li>"));
                      }
                    }
                  }

                  function update() {
                    $("#content").empty();
                    if (params.q) {

                      var qparts = params.q.split(/\s/g);
                      var allfilters = [];
                      for (var i = 0; i < qparts.length; i++) {
                        var qpart = qparts[i];
                        if (!qpart) {
                          continue;
                        }
                        allfilters.push(filter(qpart));
                      }
                      displaytable(filters(allfilters), true);
                    } else if (params.groupId && params.artifactId) {
                      var bc = $('<ol class="breadcrumb"></ol>');
                      $("#content").append(bc);
                      $("<li>").append(buildGroup(params)).appendTo(bc);
                      bc.append($('<li class="active">' + params.artifactId
                              + '</li>'));
                      displaytable(function(e) {
                        return e.g === params.groupId
                                && e.a === params.artifactId;
                      });
                    } else if (params.groupId) {
                      var bc = $('<ol class="breadcrumb"></ol>');
                      $("#content").append(bc);
                      bc.append($('<li class="active">' + params.groupId + '</li>'));
                      displaytable(function(e) {
                        return e.g === params.groupId;
                      }, true);
                    } else {
                      displaytable(function() {
                        return true;
                      }, true);
                    }
                  }

                  update();

                  $("#search-form").submit(function(e) {
                    params = {
                      q: $("#q").val()
                    };
                    update();
                    e.preventDefault();
                    return false;
                  });
                })

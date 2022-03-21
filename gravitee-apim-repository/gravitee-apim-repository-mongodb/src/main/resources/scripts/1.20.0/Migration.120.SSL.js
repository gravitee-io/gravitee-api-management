function guid() {
    function s4() {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + s4() + s4();
  }

function fixSSLConfig(endpoint, src) {
    // endpoint https sans config ssl
    if (endpoint.target && endpoint.target.startsWith("https") &&
            (      !endpoint.ssl
                || !endpoint.ssl.enabled
                || (endpoint.ssl.trustAll && endpoint.ssl.hasOwnProperty("pem"))
                || ( endpoint.ssl.hasOwnProperty("pem")
                  && endpoint.ssl.pem != null
                  && ( 0 === endpoint.ssl.pem.trim().length
                    || "null" === endpoint.ssl.pem))
        )) {
        print("    mise à jour du endpoint d'", src, ": ", endpoint.name);

        endpoint.ssl = {
            "enabled": true,
            "trustAll": true,
            "hostnameVerifier" : false
        };
        return true;
    }
    return false;
}

updatedApiIds = [];
now = new ISODate();

db.events.find({"type": {$in: ["PUBLISH_API", "UNPUBLISH_API", "START_API", "STOP_API"]}}).sort({"updatedAt":-1}).forEach(
    function(event) {
        var curApiId = event.properties.api_id;
        if (updatedApiIds.indexOf(curApiId) < 0) {
            print("API ID : ", curApiId);
            updatedApiIds.push(curApiId);

            //deserialisation de la definition
            var payload = JSON.parse(event.payload);
            var definition = JSON.parse(payload.definition);
            var toUpdate = false;

            // gestion des endpoints avant les groupes
            if (definition.proxy.endpoints) {
                definition.proxy.endpoints.forEach(
                    function (endpoint) {
                        toUpdate = fixSSLConfig(endpoint, "event") || toUpdate
                    }
                );
            }
            // gestion des endpoints apres les groupes
            else if (definition.proxy.groups) {
                definition.proxy.groups.forEach(
                    function (group) {
                        group.endpoints.forEach(
                            function (endpoint) {
                                toUpdate = fixSSLConfig(endpoint, "event") || toUpdate
                            }
                        );
                    }
                );
            }

            if (toUpdate) {
                // mise à jour de la définition d'API
                db.apis.find({ _id: curApiId }).forEach(
                    function (api) {
                        var def = JSON.parse(api.definition)
                        // gestion des endpoints avant les groupes
                        if (def.proxy.endpoints) {
                            def.proxy.endpoints.forEach(
                                function (endpoint) {
                                    fixSSLConfig(endpoint, "api")
                                }
                            );
                        }
                        // gestion des endpoints apres les groupes
                        else if (def.proxy.groups) {
                            def.proxy.groups.forEach(
                                function (group) {
                                    group.endpoints.forEach(
                                        function (endpoint) {
                                            fixSSLConfig(endpoint, "api")
                                        }
                                    );
                                }
                            );
                        }

                        print("    update the api")
                        db.apis.updateOne(
                            { _id: api._id },
                            {
                                $set: {
                                    definition: JSON.stringify(def),
                                    updatedAt: now,
                                    deployedAt: now
                                }
                            });
                    }
                );

                // ajout d'un event
                print("    copie depuis l'event:", event._id)
                event._id = guid();
                print("    insert de l'event ", event._id);
                payload.deployedAt = now
                payload.definition = JSON.stringify(definition);
                event.payload = JSON.stringify(payload);
                event.updatedAt = now;
                event.createdAt = now;
                db.events.insert(event);
            }
        }
    }
);

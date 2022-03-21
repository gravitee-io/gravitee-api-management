db.apis.find().forEach(
    function (api) {
        var endpointNames = [];
        var definition = JSON.parse(api.definition);

        if (definition.proxy.endpoints) {
            definition.proxy.endpoints.forEach(
                function (endpoint) {
                    endpointNames.push(endpoint.name);
                }
            );
        }
        else if (definition.proxy.groups) {
            definition.proxy.groups.forEach(
                function (group) {
                    endpointNames.push(group.name);
                    if (group.endpoints) {
                        group.endpoints.forEach(
                            function (endpoint) {
                                endpointNames.push(endpoint.name);
                            }
                        );
                    }
                }
            );
        }

        // test uniqueness of each name ans if contains :
        let inError = false;
        for (name of endpointNames) {
            if (!inError && (endpointNames.indexOf(name) != endpointNames.lastIndexOf(name) || (name.indexOf(":") > -1))) {
                inError = true;
            }
        }

        if (inError) {
            print("api.id: ", api._id);
            print("api.name: ", api.name);
            print("api.endpoints: [", endpointNames, "]");
            print();
        }
    }
);

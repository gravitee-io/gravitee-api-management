db.applications.update(
    {},
    {$unset: {members: ""}},
    {multi: true}
);
db.apis.update(
    {},
    {$unset: {members: ""}},
    {multi: true}
);
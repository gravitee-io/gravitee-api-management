# APIM — JDBC schema migrations

This module packages every Liquibase changeset APIM and its bundled Gamma modules apply to a JDBC
management database, so that installations running with `management.jdbc.liquibase=false` can apply
them themselves instead of opening each installed plugin. It ships as this file's own README inside
the published archive, which is what a customer reads.

Applies to the JDBC backend only.

You need a Liquibase CLI. Nothing else: the changesets in the archive are the ones the application
runs, and the CLI ships the JDBC driver for every database APIM supports except MySQL, whose licence
keeps it out. The archive filename carries the APIM version it was built from.

## Read this first

**Always pass the prefix parameters, even when your installation uses no prefix.** Almost every
changeset names its tables through a parameter — `${gravitee_prefix}api_products` rather than
`api_products`. Omitting the parameter leaves the placeholder unresolved, which changes the checksum
of every changeset, and Liquibase then reports well over a hundred failures of this shape:

```
Validation Failed:
     141 changesets check sum
          liquibase/changelogs/v1_15_0/schema.yml::1.15.0::GraviteeSource Team
              was: 9:89bae981dae8df68c63aa03c4aa12792 but is now: 9:8259b78599409dec370ffa0c10c4395f
```

Nothing is corrupted and no version is incompatible: a parameter is missing. Pass
`-Dgravitee_prefix=` with an empty value if you have no prefix.

## What is in the archive, and what to run it against

One entry point per component. List them:

```
find . -name master.yml
```

```
./liquibase/master.yml       APIM itself
./liquibase/aim/master.yml   the "aim" Gamma module
```

`liquibase/master.yml` is always APIM. Every other entry point is a Gamma module, and the directory
name is the module id. Only modules that own a schema appear — a module absent from this list has
nothing to migrate.

**Each entry point is a separate run.** Modules keep their migration history in their own tracking
tables, independently of APIM and of each other, so running APIM's changelog does not migrate the
modules. Derive the arguments from the id:

| | Changelog | Tracking tables | Parameters |
|---|---|---|---|
| APIM | `liquibase/master.yml` | `<prefix>databasechangelog` and `<prefix>databasechangeloglock` | `gravitee_prefix`, `gravitee_rate_limit_prefix` |
| module `<id>` | `liquibase/<id>/master.yml` | `<prefix><id>_databasechangelog` and `<prefix><id>_databasechangeloglock` | `<id>_prefix` |

`<prefix>` is `management.jdbc.prefix` from your `gravitee.yml`, empty unless you configured one.
`gravitee_rate_limit_prefix` is `ratelimit.jdbc.prefix`.

**The tracking table names carry the prefix too.** The application derives them from
`management.jdbc.prefix`, so a run that leaves them at Liquibase's default opens a different table
from the one the application writes to, finds it empty, and replays every changeset against an
already-migrated database.

Set these once and every command below can be pasted as it is:

```
URL=<jdbc url>     # e.g. jdbc:postgresql://localhost:5432/gravitee
USER=<user>
PASSWORD=<password>
PREFIX=            # management.jdbc.prefix, leave empty if you configured none
RL_PREFIX=         # ratelimit.jdbc.prefix
```

## Reviewing what an upgrade would do

`update-sql` writes the pending statements to a file without applying them. Run it from the
directory you unzipped the archive into.

APIM:

```
liquibase --search-path=. --changelog-file=liquibase/master.yml --url="$URL" --username="$USER" --password="$PASSWORD" --database-changelog-table-name="${PREFIX}databasechangelog" --database-changelog-lock-table-name="${PREFIX}databasechangeloglock" --output-file=apim.sql update-sql "-Dgravitee_prefix=$PREFIX" "-Dgravitee_rate_limit_prefix=$RL_PREFIX"
```

Then one run per module — here `aim`:

```
liquibase --search-path=. --changelog-file=liquibase/aim/master.yml --url="$URL" --username="$USER" --password="$PASSWORD" --database-changelog-table-name="${PREFIX}aim_databasechangelog" --database-changelog-lock-table-name="${PREFIX}aim_databasechangeloglock" --output-file=aim.sql update-sql "-Daim_prefix=$PREFIX"
```

Or generate every file in one pass:

```
liquibase --search-path=. --changelog-file=liquibase/master.yml --url="$URL" --username="$USER" --password="$PASSWORD" --database-changelog-table-name="${PREFIX}databasechangelog" --database-changelog-lock-table-name="${PREFIX}databasechangeloglock" --output-file=apim.sql update-sql "-Dgravitee_prefix=$PREFIX" "-Dgravitee_rate_limit_prefix=$RL_PREFIX"
for module in $(find liquibase -mindepth 2 -name master.yml | cut -d/ -f2); do
  liquibase --search-path=. --changelog-file="liquibase/$module/master.yml" --url="$URL" --username="$USER" --password="$PASSWORD" --database-changelog-table-name="${PREFIX}${module}_databasechangelog" --database-changelog-lock-table-name="${PREFIX}${module}_databasechangeloglock" --output-file="$module.sql" update-sql "-D${module}_prefix=$PREFIX"
done
```

The generated files carry their own `INSERT INTO ... databasechangelog` statements. Applying one
therefore leaves the database in the state the application expects, and it will start without
replaying anything.

`update-sql` does not change your schema, but it does take the Liquibase lock, so it writes to the
lock table and creates it if absent. It is not a read-only operation.

## Applying

Replace `update-sql` with `update` to apply.

## Three things that will bite you

**Do not move the files inside `liquibase/`.** Liquibase identifies a changeset by *(id, author,
filename)*, and `filename` is the changelog path as resolved — it is stored in the tracking table.
The paths in the archive are the ones the application records when it migrates itself. Re-rooting
them makes your run record different filenames, and the application then considers nothing applied
and replays everything against an already-migrated database. No error is reported at the time of
your run.

**The archive describes one combination.** It carries one APIM version and the module versions
bundled with it. If you upgraded a module on its own, use the changelogs of the version you actually
run — they are inside that plugin's own archive, under `liquibase/<id>/`.

**MySQL and MariaDB need `--classpath`.** On MySQL, because the CLI carries no driver for it. And on
both, when the server runs with `sql_require_primary_key=ON`: Liquibase creates its tracking table
without a primary key, and the server rejects it with error 3750 before any changeset runs. The
application never hits this, because it registers a generator that adds the key. Take that same jar
out of the plugin you already have installed:

```
unzip -j plugins/gravitee-apim-repository-jdbc-*.zip 'gravitee-apim-repository-jdbc-*.jar' -d .
```

then add `--classpath=<that jar>` to the commands above, prefixed by your MySQL driver and a colon if
you need one. It only matters for the run that creates the tracking table: once the table exists with
its key, later runs need nothing.

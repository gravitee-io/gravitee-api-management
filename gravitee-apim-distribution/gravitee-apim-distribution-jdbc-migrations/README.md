# APIM — JDBC schema migrations

This module packages every Liquibase changeset APIM and its bundled Gamma modules apply to a JDBC
management database, so that installations running with `management.jdbc.liquibase=false` can apply
them themselves instead of opening each installed plugin. It ships as this file's own README inside
the published archive, which is what a customer reads.

Applies to the JDBC backend only. The `mongodb` backends have no file-based migrations.

You need a Liquibase CLI and a JDBC driver for your database. Nothing else: the changesets in the
archive are the ones the application runs. The archive filename carries the APIM version it was
built from.

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

## Reviewing what an upgrade would do

`update-sql` writes the pending statements to a file without applying them. Run it from the
directory you unzipped the archive into.

APIM:

```
liquibase --search-path=. --changelog-file=liquibase/master.yml --url=<jdbc url> --username=<user> --password=<password> --output-file=apim.sql update-sql -Dgravitee_prefix= -Dgravitee_rate_limit_prefix=
```

Then one run per module — here `aim`:

```
liquibase --search-path=. --changelog-file=liquibase/aim/master.yml --url=<jdbc url> --username=<user> --password=<password> --database-changelog-table-name=aim_databasechangelog --database-changelog-lock-table-name=aim_databasechangeloglock --output-file=aim.sql update-sql -Daim_prefix=
```

Or generate every file in one pass:

```
liquibase --search-path=. --changelog-file=liquibase/master.yml --url="$URL" --username="$USER" --password="$PASSWORD" --output-file=apim.sql update-sql -Dgravitee_prefix= -Dgravitee_rate_limit_prefix=
for module in $(find liquibase -mindepth 2 -name master.yml | cut -d/ -f2); do
  liquibase --search-path=. --changelog-file="liquibase/$module/master.yml" --url="$URL" --username="$USER" --password="$PASSWORD" --database-changelog-table-name="${module}_databasechangelog" --database-changelog-lock-table-name="${module}_databasechangeloglock" --output-file="$module.sql" update-sql "-D${module}_prefix="
done
```

The generated files carry their own `INSERT INTO ... databasechangelog` statements. Applying one
therefore leaves the database in the state the application expects, and it will start without
replaying anything.

`update-sql` does not change your schema, but it does take the Liquibase lock, so it writes to the
lock table and creates it if absent. It is not a read-only operation.

## Applying

Replace `update-sql` with `update` to apply, or with `changelog-sync` to record the changesets as
already applied on a database you brought up to date by other means.

## Two things that will bite you

**Do not move the files inside `liquibase/`.** Liquibase identifies a changeset by *(id, author,
filename)*, and `filename` is the changelog path as resolved — it is stored in the tracking table.
The paths in the archive are the ones the application records when it migrates itself. Re-rooting
them makes your run record different filenames, and the application then considers nothing applied
and replays everything against an already-migrated database. No error is reported at the time of
your run.

**The archive describes one combination.** It carries one APIM version and the module versions
bundled with it. If you upgraded a module on its own, use the changelogs of the version you actually
run — they are inside that plugin's own archive, under `liquibase/<id>/`.

# Local PostgreSQL backups and restore verification

These helpers target the development database in `compose.yaml`. They do not target the separate port-8088 container stack, a public server or an external backup service. Start local services first with `start-local.ps1`.

## Create a backup

From PowerShell 7:

```powershell
Set-Location D:\GuestHouse
.\backup-local.ps1
```

The script reads the local database name/user from `.env`, runs PostgreSQL 17 `pg_dump` in the existing container and copies its custom-format archive into `.backups`. PostgreSQL supplies a transactionally consistent database snapshot without stopping the application. A JSON manifest records its database name, UTC creation time, size and SHA-256 checksum. Binary archive data is copied as a file, not passed through a PowerShell text pipeline. Temporary container files are removed.

Each backup has a unique timestamp/GUID name and is not overwritten automatically. The working database is not changed. This script does not delete old backups, install schedules or upload files. Decide a retention policy and maintain a separate private copy on another storage device if recovery from disk loss is needed.

Backups contain guest/account records, password hashes and session records. They are **not encrypted**. The `.backups` folder is ignored by Git; do not share or commit it. Keep archives, manifests and verification reports private. A checksum detects accidental changes; it does not prove authenticity if someone can replace both the archive and manifest.

## Verify a restore

Pass the exact archive path printed by the backup command:

```powershell
.\verify-local-backup.ps1 -BackupPath 'D:\GuestHouse\.backups\<printed-backup-name>.dump'
```

The adjacent `.dump.json` manifest is required. A size/hash mismatch fails before creating a database. The script creates a unique `guesthouse_restore_<guid>_test` database, restores with `pg_restore --single-transaction --exit-on-error`, and checks migrations, inventory and key data tables. PostgreSQL recreates the dump's database constraints as part of restore. A successful archive restoration is stronger evidence than merely listing archive contents.

The disposable database and temporary container file are removed, including after an unsuccessful restore. Cleanup is restricted to the exact uniquely named database created by that invocation. A cleanup failure is reported as an error with its temporary name for investigation. Successful verification writes an adjacent `.dump.verified.json` report containing counts and the archive hash, without guest details.

This is a restore rehearsal, **not a command to overwrite the working database**. It also does not start a second application against the restored copy or replay the entire booking lifecycle there. The helper currently expects this guest-house schema with at least five migrations and ten rooms; review its checks if the inventory/schema changes.

## Recovery scope

The archive covers the selected PostgreSQL database. It does not back up `.env`, application source, images, Docker volumes outside that database, PostgreSQL cluster roles or server configuration. Keep the source/version and required secrets separately in appropriate private storage. Restore uses the configured local database user rather than recreating original ownership and privileges.

For actual recovery, stop writes, preserve the damaged database for investigation, select a verified archive and restore to a separately named empty recovery database before switching the application connection. Validate the application and migrations before resuming use. Review session invalidation/JWT-secret rotation so restored old sessions cannot unexpectedly become valid. Recovery of a public installation requires a rehearsal on that actual hosting setup; this local check does not establish off-machine disaster recovery.

## Verification on 27 October 2026

- Created a custom-format archive of the working local database and its checksum manifest.
- Successfully restored it into a separate disposable database, checked the schema/inventory and removed the temporary database.
- Rejected an intentionally invalid archive/checksum pair before database creation.
- Confirmed no matching temporary restore databases remained and that the original backend health and website still responded successfully.
- Confirmed `.backups` is ignored by Git. No working database was replaced, no backup was published, and no recurring job was configured.

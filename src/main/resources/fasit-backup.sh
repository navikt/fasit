#!/bin/bash

BACKUP_HOME=/app/fasit_backup/files
NEW_BACKUP_FILE=$BACKUP_HOME/fasit-backup-$(date +%s).tar.gz

mkdir -p $BACKUP_HOME

logger -p cron.info -t fasit-backup "Creating backup-file $NEW_BACKUP_FILE of /app/fasit"
tar -czpvf $NEW_BACKUP_FILE --exclude=fasit-backup*.tar.gz  /app/fasit/ || { logger -p cron.error -t fasit-backup "backup for $NEW_BACKUP_FILE failed"; exit 1; }

# cleanup
ls -t $BACKUP_HOME/*.tar.gz | tail -n +6 | xargs rm -f || { logger -p cron.error -t fasit-backup "Unable to cleanup backup files"; exit 1; }


FASIT BACKUP

In order to be able to restore Fasit to a previous state, we backup the entire /app/fasit directory before performing the deployment by running the backup.sh script.
The script will create a compressed archive of the entire /app/fasit directory and put it in /app/fasit_backup/files  

If the number of backups exceed the configured size (5 by default) it will delete the oldest one to prevent unnecessary disk usage

RESTORE

In order to restore the node to the state of a backup, log into the server and run the command

tar xzvf [your backup of choice] -C /

which will unzip the backup into the /app/fasit directory.

Finally, you start JBoss and you should be back in business.

service jboss-fasit start
#!/bin/bash
set -e
set -u

# Funzione per creare utente e database
function create_user_and_database() {
  local database=$1
  local user=$2
  local password=$3
  echo "  Creating user '$user' and database '$database'"

  # Assegniamo direttamente SUPERUSER.
  # Questo include automaticamente REPLICATION e i permessi per creare Publication.
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
     CREATE USER $user WITH PASSWORD '$password' SUPERUSER;
     CREATE DATABASE $database WITH OWNER $user;
EOSQL
}

# Legge la variabile POSTGRES_MULTIPLE_DATABASES
if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
  echo "Multiple database creation requested:"
  # Itera sulla stringa separata da virgole
  for db_config in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
    # Legge i singoli pezzi separati da ":"
    IFS=':' read -r db user password <<< "$db_config"
    create_user_and_database $db $user $password
  done
  echo "Multiple databases created"
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "filesdb" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS vector;
EOSQL
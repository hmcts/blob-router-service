#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER blob_router;
    CREATE DATABASE blob_router
        WITH OWNER = blob_router
        ENCODING ='UTF-8'
        CONNECTION LIMIT = -1;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=blob_router --username "$POSTGRES_USER" <<-EOSQL
    CREATE SCHEMA blob_router AUTHORIZATION blob_router;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=blob_router --username "$POSTGRES_USER" <<-EOSQL
    CREATE EXTENSION lo;
EOSQL

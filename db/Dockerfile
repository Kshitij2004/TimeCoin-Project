FROM mysql:8.0
COPY init.sql /docker-entrypoint-initdb.d/01-init.sql
COPY seed.sql /docker-entrypoint-initdb.d/02-seed.sql
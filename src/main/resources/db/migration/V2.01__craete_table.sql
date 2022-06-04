DROP TABLE IF EXISTS record;

SET TIME ZONE 'UTC';

CREATE TABLE record (
    id serial,
    datetime TIMESTAMP UNIQUE,
    amount FLOAT,
    PRIMARY KEY (id)
);

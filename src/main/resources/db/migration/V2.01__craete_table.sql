DROP TABLE IF EXISTS record;

CREATE TABLE record (
    id serial,
    datetime TIMESTAMP UNIQUE,
    amount FLOAT,
    PRIMARY KEY (id)
);

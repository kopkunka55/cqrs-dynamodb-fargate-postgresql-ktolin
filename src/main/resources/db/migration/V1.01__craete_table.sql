DROP TABLE IF EXISTS record;

CREATE TABLE record (
    id serial,
    datetime TIMESTAMP UNIQUE,
    amount FLOAT,
    datetime_with_tz VARCHAR(30),
    PRIMARY KEY (id)
);

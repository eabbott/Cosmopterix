-- Data store table
CREATE TABLE hll
(
  hash         bigint           NOT NULL
, registers    varbinary(99)    NOT NULL
, CONSTRAINT hll_hash_pk PRIMARY KEY
  (
    hash
  )
);

PARTITION TABLE hll ON COLUMN hash;

CREATE PROCEDURE FROM CLASS org.eabbott.volthll.procedures.HllSet;
PARTITION PROCEDURE HllSet ON TABLE hll COLUMN hash;

CREATE PROCEDURE FROM CLASS org.eabbott.volthll.procedures.HllMerge;
PARTITION PROCEDURE HllMerge ON TABLE hll COLUMN hash;

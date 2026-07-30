-- Drops everything schema.sql creates. Handy while iterating on the model during
-- the hackathon; never run it against data you want to keep.
DROP TABLE recommendations CASCADE CONSTRAINTS PURGE;
DROP TABLE transactions CASCADE CONSTRAINTS PURGE;
DROP TABLE users CASCADE CONSTRAINTS PURGE;
DROP TABLE currencies CASCADE CONSTRAINTS PURGE;

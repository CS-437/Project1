DROP DATABASE IF EXISTS TokenIndex;
CREATE DATABASE TokenIndex;
use TokenIndex;

-- ------------- --
-- Create Tables --
-- ------------- --

CREATE TABLE IF NOT EXISTS Tokens (
	TokenPK int not null,
	Token varchar(45) not null,
    HashValue int not null
);

CREATE TABLE IF NOT EXISTS Documents (
    DocumentID int not null,
    Title varchar(100) not null,
    Path varchar(255) not null
);

CREATE TABLE IF NOT EXISTS Intersection (
    TokenFK int not null,
    DocumentID int not null,
    Frequency int not null
);

-- ----------------- --
-- Add Primary Keys --
-- ----------------- --

ALTER TABLE Tokens
MODIFY TokenPK int not null AUTO_INCREMENT PRIMARY KEY;

ALTER TABLE Documents
ADD PRIMARY KEY (DocumentID);

ALTER TABLE Intersection
ADD PRIMARY KEY (TokenFK, DocumentID);

-- ----------------- --
-- Create FK Indexes --
-- ----------------- --

CREATE INDEX FK_IND_1 ON Intersection(TokenFK);
CREATE INDEX FK_IND_2 ON Intersection(DocumentID);

-- ---------------- --
-- Add Foreign Keys --
-- ---------------- --

ALTER TABLE Intersection
ADD CONSTRAINT Intersection_FK1 FOREIGN KEY
(TokenFK) REFERENCES Tokens (TokenPK);

ALTER TABLE Intersection
ADD CONSTRAINT Intersection_FK2 FOREIGN KEY
(DocumentID) REFERENCES Documents (DocumentID);

-- ----------- --
-- Add Indexes --
-- ----------- --

CREATE INDEX Token_Hash ON Tokens(HashValue);

-- -------------- --
-- Add Procedures --
-- -------------- --

DELIMITER $$
CREATE PROCEDURE `Update_Add_Token` (
		in docId int,
        in tkn varchar(45),
        in hash int,
        in frequency int)
BEGIN
Declare tpk int default -1;

if (select count(*) from tokens as t where t.HashValue=hash and t.Token like tkn) = 0 then
	insert into Tokens (Token, HashValue) VALUES (tkn, hash);
end if;

Select TokenPK into tpk from Tokens as t
where t.HashValue=hash and t.Token like tkn;

insert into Intersection (TokenFK, DocumentID, Frequency) VALUES (tpk, docId, frequency);
END$$

DELIMITER ;
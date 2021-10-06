DROP DATABASE IF EXISTS TokenIndex;
CREATE DATABASE TokenIndex;
use TokenIndex;

-- ------------- --
-- Create Tables --
-- ------------- --

CREATE TABLE IF NOT EXISTS Tokens (
	TokenPK int not null,
	Token varchar(45) not null,
    HashValue int not null,
    VocabSize int,
    CollectionSize int
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
        in token varchar(45), 
        in hashValue int, 
        in frequency int, 
        in vocabSize int, 
        in collectionSize int)
BEGIN
Declare tpk int default -1;

if (select count(*) from tokens as t where t.HashValue=hashValue and t.Token like token) = 0 then
	insert into Tokens (Token, HashValue, VocabSize, CollectionSize) VALUES (token, hashValue, vocabSize, collectionSize);
end if;

Select TokenPK into tpk from Tokens
where t.HashValue=hashValue and t.Token like token;

insert into Intersection (TokenFK, DocumentID, Frequency) VALUES (tpk, docId, frequency);
END$$

DELIMITER ;
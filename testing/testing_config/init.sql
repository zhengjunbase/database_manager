CREATE GLOBAL TEMPORARY TABLE TEST_CATEGORY
AS
  SELECT * FROM CATEGORY;
  
CREATE GLOBAL TEMPORARY TABLE TEST_BRAND
AS
  SELECT * FROM BRAND;

CREATE GLOBAL TEMPORARY TABLE TEST_WEEKDAY
AS
  SELECT * FROM WEEKDAY;

CREATE GLOBAL TEMPORARY TABLE TEST_MULTIPLIER
AS
  SELECT * FROM MULTIPLIER;
  
CREATE GLOBAL TEMPORARY TABLE TEST_UNIT_SYSTEM
AS
  SELECT * FROM UNIT_SYSTEM;

CREATE GLOBAL TEMPORARY TABLE TEST_GENDER
AS
  SELECT * FROM GENDER;

CREATE GLOBAL TEMPORARY TABLE TEST_TEMP_ACCOUNT
AS
  SELECT * FROM TEST_ACCOUNT;

CREATE GLOBAL TEMPORARY TABLE TEST_ACCOUNT
AS
  SELECT * FROM ACCOUNT;


DROP TABLE TEST_ACCOUNT_SETTING;
DROP TABLE TEST_TEMP_ACCOUNT;
DROP TABLE TEST_ACCOUNT;
DROP TABLE TEST_GENDER;
DROP TABLE TEST_MULTIPLIER;
DROP TABLE TEST_UNIT_SYSTEM;
DROP TABLE TEST_WEEKDAY;
DROP TABLE TEST_BRAND;
DROP TABLE TEST_CATEGORY;
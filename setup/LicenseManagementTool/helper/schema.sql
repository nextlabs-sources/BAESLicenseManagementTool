CREATE TABLESPACE tbs_lmt_per
  DATAFILE 'tbs_lmt_per.dat' 
    SIZE 40M AUTOEXTEND ON
  ONLINE;  

CREATE TEMPORARY TABLESPACE tbs_lmt_temp
  TEMPFILE 'tbs_lmt_temp.dbf'
    SIZE 20M
    AUTOEXTEND ON;                        

CREATE USER lmtadmin
  IDENTIFIED BY Nextlabs123
  DEFAULT TABLESPACE tbs_lmt_per
  TEMPORARY TABLESPACE tbs_lmt_temp
  QUOTA unlimited on tbs_lmt_per;
  

GRANT create session TO lmtadmin;
GRANT create table TO lmtadmin;
GRANT create view TO lmtadmin;
GRANT create any trigger TO lmtadmin;
GRANT create any procedure TO lmtadmin;
GRANT create sequence TO lmtadmin;
GRANT create synonym TO lmtadmin;
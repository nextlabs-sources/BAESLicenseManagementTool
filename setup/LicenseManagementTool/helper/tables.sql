CREATE TABLE license (
	name varchar2(255) not null,
	category varchar2(255) not null,
	label varchar2(255) not null,
	parties varchar2(1000),
	effectivedate timestamp,
	expiration timestamp,
	deactivated number DEFAULT 0,
	groupenabled number DEFAULT 0,
	groupname varchar2(255),
	CONSTRAINT license_pk PRIMARY KEY (name)	
);

CREATE TABLE project (
	name varchar2(255) not null,
	description varchar2(4000),
	deactivated number DEFAULT 0,
	CONSTRAINT project_pk PRIMARY KEY (name)	
);

CREATE TABLE license_project (
	license varchar2(255) not null,
	project varchar2(255) not null,
	CONSTRAINT license_project_pk PRIMARY KEY (license, project),
	CONSTRAINT fk_license
    		FOREIGN KEY (license)
    		REFERENCES license(name) ON DELETE CASCADE,
	CONSTRAINT fk_project
    		FOREIGN KEY (project)
    		REFERENCES project(name) ON DELETE CASCADE	
);

CREATE TABLE user_license (
	aduser varchar2(255) not null,
	displayname varchar2(255),
	email varchar2(255),
	license varchar2(255) not null,
	CONSTRAINT user_license_pk PRIMARY KEY (aduser, license),
	CONSTRAINT fk_user_license
			FOREIGN KEY (license)
			REFERENCES license(name) ON DELETE CASCADE
);

CREATE TABLE user_project (
	aduser varchar2(255) not null,
	displayname varchar2(255),
	email varchar2(255),
	project varchar2(255) not null,
	CONSTRAINT user_project_pk PRIMARY KEY (aduser, project),
	CONSTRAINT fk_user_project
			FOREIGN KEY (project)
			REFERENCES project(name) ON DELETE CASCADE
);

CREATE TABLE log (
	id varchar2(36) not null,
	time timestamp not null,
	admin varchar2(255) not null,
	targetuser varchar2(255) not null,
	action varchar2(50) not null,
	attribute varchar2(255) not null,
	oldvalue varchar2(255),
	value varchar2(255),
	trig varchar2(255),
	CONSTRAINT log_pk PRIMARY KEY (id)	
);

CREATE TABLE task (
	timestart timestamp not null,
	admin varchar2(255) not null,
	tasktype varchar(50) not null,
	error number,
	totalprogress int,
	tempprogress int,
	progress int,
	successcount int,
	failcount int,
	message clob,
	taskjson clob not null,
	CONSTRAINT task_pk PRIMARY KEY (timestart, admin, tasktype)
);


CREATE TABLE application_detail ( organization VARCHAR(100), space VARCHAR(100), app_id VARCHAR(50) PRIMARY KEY, app_name VARCHAR(100), buildpack VARCHAR(50), image VARCHAR(250), stack VARCHAR(25), running_instances INT, total_instances INT, urls VARCHAR(2000), last_pushed TIMESTAMP, last_event VARCHAR(50), last_event_actor VARCHAR(100), last_event_time TIMESTAMP, requested_state VARCHAR(25) );
CREATE TABLE service_instance_detail ( organization VARCHAR(100), space VARCHAR(100), service_id VARCHAR(50) PRIMARY KEY, service_name VARCHAR(100), service VARCHAR(100), description VARCHAR(1000), plan VARCHAR(50), type VARCHAR(30), bound_applications CLOB(30M), last_operation VARCHAR(50), last_updated TIMESTAMP, dashboard_url VARCHAR(250), requested_state VARCHAR(25) );
CREATE TABLE application_policy ( id VARCHAR(50) PRIMARY KEY, description VARCHAR(1000), state VARCHAR(25), from_datetime TIMESTAMP, from_duration VARCHAR(25), delete_services BOOLEAN, organization_whitelist CLOB(30M) );
CREATE TABLE service_instance_policy ( id VARCHAR(50) PRIMARY KEY, description VARCHAR(1000), from_datetime TIMESTAMP, from_duration VARCHAR(25), organization_whitelist CLOB(30M) );
CREATE TABLE application_relationship ( id INT IDENTITY PRIMARY KEY, organization VARCHAR(100), space VARCHAR(100), app_id VARCHAR(50), app_name VARCHAR(100), service_id VARCHAR(50), service_name VARCHAR(100), service_plan VARCHAR(50), service_type VARCHAR(30) );
CREATE TABLE historical_record ( id INT IDENTITY PRIMARY KEY, transaction_datetime TIMESTAMP, action_taken VARCHAR(20), organization VARCHAR(100), space VARCHAR(100), app_id VARCHAR(50), service_id VARCHAR(50), type VARCHAR(20), name VARCHAR(300) );
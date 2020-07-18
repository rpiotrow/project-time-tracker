-- create schemas

CREATE SCHEMA IF NOT EXISTS ptt;
CREATE SCHEMA IF NOT EXISTS ptt_read_model;

-- read-side model

CREATE TABLE IF NOT EXISTS ptt_read_model.projects(
  db_id SERIAL PRIMARY KEY,
  deleted_at TIMESTAMP CHECK (deleted_at >= updated_at),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL CHECK (updated_at >= created_at),
  id VARCHAR UNIQUE CHECK(id != ''),
  owner UUID NOT NULL,
  duration_sum BIGINT NOT NULL CHECK(duration_sum >= 0)
);

CREATE TABLE IF NOT EXISTS ptt_read_model.tasks(
  db_id SERIAL PRIMARY KEY,
  deleted_at TIMESTAMP,
  project_id BIGINT NOT NULL REFERENCES ptt_read_model.projects(db_id),
  owner UUID NOT NULL,
  started_at TIMESTAMP NOT NULL,
  duration BIGINT NOT NULL CHECK (duration >= 0),
  volume INT CHECK (volume >= 0),
  comment TEXT
);

CREATE TABLE IF NOT EXISTS ptt_read_model.statistics(
  db_id SERIAL PRIMARY KEY,
  owner UUID NOT NULL,
  year SMALLINT NOT NULL CHECK (year >= 2020),
  month SMALLINT NOT NULL CHECK (month >= 1 AND month <= 12),
  number_of_tasks INT NOT NULL CHECK (number_of_tasks >= 0),
  average_task_duration DECIMAL NOT NULL CHECK (average_task_duration >= 0),
  average_task_volume DECIMAL CHECK (average_task_volume >= 0),
  volume_weighted_average_task_duration DECIMAL CHECK (volume_weighted_average_task_duration >= 0),

  UNIQUE(owner, year, month)
);

-- write-side model

-- TBD

-- create users

CREATE USER reader WITH PASSWORD 'reader';

GRANT USAGE ON SCHEMA ptt_read_model TO reader;
GRANT SELECT ON ALL TABLES IN SCHEMA ptt_read_model to reader;

CREATE USER writer WITH PASSWORD 'writer';

GRANT USAGE ON SCHEMA ptt TO writer;
GRANT SELECT,INSERT ON ALL TABLES IN SCHEMA ptt to writer;

GRANT USAGE ON SCHEMA ptt_read_model TO writer;
GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA ptt_read_model to writer;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA ptt_read_model to writer;

-- create schemas

CREATE SCHEMA IF NOT EXISTS ptt;
CREATE SCHEMA IF NOT EXISTS ptt_read_model;

-- read-side model

CREATE TABLE IF NOT EXISTS ptt_read_model.projects(
  db_id SERIAL PRIMARY KEY,
  deleted_at TIMESTAMP CONSTRAINT deleted_not_before_updated CHECK (deleted_at >= updated_at),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL CONSTRAINT updated_not_before_created CHECK (updated_at >= created_at),
  id VARCHAR UNIQUE CONSTRAINT id_not_empty CHECK(id != ''),
  owner UUID NOT NULL,
  duration_sum INTERVAL NOT NULL
    CONSTRAINT duration_sum_positive CHECK(duration_sum >= interval '0 seconds')
);

CREATE TABLE IF NOT EXISTS ptt_read_model.tasks(
  db_id SERIAL PRIMARY KEY,
  deleted_at TIMESTAMP,
  project_id BIGINT NOT NULL REFERENCES ptt_read_model.projects(db_id),
  owner UUID NOT NULL,
  started_at TIMESTAMP NOT NULL,
  duration INTERVAL NOT NULL CONSTRAINT duration_positive CHECK (duration >= interval '0 seconds'),
  volume INT CONSTRAINT volume_positive CHECK (volume >= 0),
  comment TEXT
);

CREATE TABLE IF NOT EXISTS ptt_read_model.statistics(
  db_id SERIAL PRIMARY KEY,
  owner UUID NOT NULL,
  year SMALLINT NOT NULL CONSTRAINT year_after_2020 CHECK (year >= 2020),
  month SMALLINT NOT NULL CONSTRAINT month_in_range_1_to_12 CHECK (month >= 1 AND month <= 12),
  number_of_tasks INT NOT NULL CONSTRAINT number_of_tasks_positive CHECK (number_of_tasks >= 0),
  average_task_duration INTERVAL NOT NULL
    CONSTRAINT average_task_duration_positive CHECK (average_task_duration >= interval '0 seconds'),
  average_task_volume DECIMAL CONSTRAINT average_task_volume_positive CHECK (average_task_volume >= 0),
  volume_weighted_average_task_duration INTERVAL
    CONSTRAINT volume_weighted_average_task_duration_positive
    CHECK (volume_weighted_average_task_duration >= interval '0 seconds'),

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

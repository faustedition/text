CREATE TABLE IF NOT EXISTS text_qname (
  id BIGINT PRIMARY KEY,
  local_name VARCHAR(255) NOT NULL,
  namespace VARCHAR(255),
  UNIQUE (local_name, namespace)
);

CREATE SEQUENCE IF NOT EXISTS text_qname_sequence;

CREATE TABLE IF NOT EXISTS text_content (
  id BIGINT PRIMARY KEY,
  type SMALLINT NOT NULL,
  content CLOB NOT NULL,
  content_length BIGINT NOT NULL,
  content_digest BYTEA(64) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS text_content_sequence;

CREATE TABLE IF NOT EXISTS text_annotation (
  id BIGINT PRIMARY KEY,
  text BIGINT NOT NULL REFERENCES text_content (id) ON DELETE CASCADE,
  name BIGINT NOT NULL REFERENCES text_qname (id),
  range_start BIGINT NOT NULL,
  range_end BIGINT NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS text_annotation_sequence;
CREATE INDEX IF NOT EXISTS text_annotation_ranges ON text_annotation (range_start, range_end);

CREATE TABLE IF NOT EXISTS text_annotation_link (
  id BIGINT PRIMARY KEY,
  name BIGINT NOT NULL REFERENCES text_qname (id)
);

CREATE SEQUENCE IF NOT EXISTS text_annotation_link_sequence;

CREATE TABLE IF NOT EXISTS text_annotation_link_target (
  link BIGINT NOT NULL REFERENCES text_annotation_link (id) ON DELETE CASCADE,
  target BIGINT NOT NULL REFERENCES text_annotation (id) ON DELETE CASCADE,
  UNIQUE (link, target)
);

CREATE TABLE IF NOT EXISTS text_annotation_data (
  annotation BIGINT NOT NULL REFERENCES text_annotation (id) ON DELETE CASCADE,
  name BIGINT NOT NULL REFERENCES text_qname (id),
  value TEXT NOT NULL,
  UNIQUE (annotation, name)
);

CREATE TABLE IF NOT EXISTS text_annotation_link_data (
  link BIGINT NOT NULL REFERENCES text_annotation_link (id) ON DELETE CASCADE,
  name BIGINT NOT NULL REFERENCES text_qname (id),
  value TEXT NOT NULL,
  UNIQUE (link, name)
);

/**
  This file represents the sql schema of the v3 backend.
  Execute jooqCodegen to create java classes for these files.
 */

CREATE TABLE IF NOT EXISTS cloud_servers
(
    unique_id      varchar   NOT NULL PRIMARY KEY,
    group_name     varchar   NOT NULL,
    host_id        varchar   NOT NULL,
    numerical_id   int       NOT NULL,
    ip             varchar   NOT NULL,
    port           int       NOT NULL,
    minimum_memory int       NOT NULL,
    maximum_memory int       NOT NULL,
    max_players    int       NOT NULL,
    player_count   int       NOT NULL,
    state          varchar   NOT NULL,
    type           varchar   NOT NULL,
    created_at     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cloud_server_properties
(
    server_id varchar NOT NULL,
    key       varchar NOT NULL,
    value     varchar,
    CONSTRAINT compound_key PRIMARY KEY (server_id, key)
);

CREATE TABLE IF NOT EXISTS oauth2_client_details
(
    client_id     varchar PRIMARY KEY,
    client_secret varchar,
    redirect_uri  varchar,
    grant_types   varchar,
    scope         varchar
);

CREATE TABLE IF NOT EXISTS oauth2_users
(
    user_id         varchar PRIMARY KEY,
    scopes          varchar,
    username        varchar UNIQUE NOT NULL,
    hashed_password varchar NOT NULL
);


CREATE TABLE IF NOT EXISTS oauth2_tokens
(
    token_id     varchar PRIMARY KEY,
    client_id    varchar,
    access_token varchar,
    scope        varchar,
    expires_in   timestamp,
    user_id      varchar,
    CONSTRAINT fk_user_token FOREIGN KEY (user_id) REFERENCES oauth2_users (user_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS oauth2_groups
(
    group_name varchar PRIMARY KEY,
    scopes     varchar
);

CREATE TABLE IF NOT EXISTS oauth2_user_groups
(
    user_id    VARCHAR,
    group_name VARCHAR,
    PRIMARY KEY (user_id, group_name),
    CONSTRAINT fk_user_group_user FOREIGN KEY (user_id) REFERENCES oauth2_users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_group_group FOREIGN KEY (group_name) REFERENCES oauth2_groups (group_name) ON DELETE CASCADE
);

CREATE DATABASE IF NOT EXISTS message_in_bottle DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE message_in_bottle;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    balance DOUBLE NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    amount DOUBLE NOT NULL,
    deadline VARCHAR(64) NOT NULL,
    publisher_id BIGINT NOT NULL,
    publisher_name VARCHAR(64) NOT NULL,
    publish_time_text VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    accepter_id BIGINT NULL,
    review_status VARCHAR(32) NULL,
    completed BIT NOT NULL,
    created_at BIGINT NOT NULL
);

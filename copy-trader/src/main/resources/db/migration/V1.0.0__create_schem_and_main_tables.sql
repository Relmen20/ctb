CREATE SCHEMA IF NOT EXISTS trader;

CREATE TABLE IF NOT EXISTS trader.auth (
    auth_id serial PRIMARY KEY,
    person_name VARCHAR(255) NOT NULL,
    chat_id INT NOT NULL,
    wallet_address VARCHAR(255),
    private_key VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS trader.subscription (
    subscription_id serial PRIMARY KEY,
    sub_name VARCHAR(255) NOT NULL,
    follow_key_available INT NOT NULL,
    count_autotrade_available INT NOT NULL,
    sub_price FLOAT NOT NULL,
    sub_date_period INTERVAL NOT NULL
);

CREATE TABLE IF NOT EXISTS trader.follow (
    follow_id serial PRIMARY KEY,
    user_id INT NOT NULL REFERENCES trader.auth(auth_id),
    follow_key_wallet VARCHAR(255) NOT NULL,
    count_coll_done INT,
    count_autotrade_done INT,
    date_start_follow DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS trader.subscription_history(
    trade_id serial PRIMARY KEY,
    user_id INT NOT NULL REFERENCES trader.auth(auth_id),
    subscription_id INT NOT NULL REFERENCES trader.subscription(subscription_id),
    status BOOLEAN NOT NULL
);
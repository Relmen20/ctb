CREATE SCHEMA IF NOT EXISTS trader;

CREATE TABLE IF NOT EXISTS trader.subscription (
    subscription_id serial PRIMARY KEY,
    sub_name VARCHAR(255) NOT NULL,
    sub_description VARCHAR(255) NOT NULL,
    follow_key_available INT NOT NULL,
    count_coll_available INT NOT NULL,
    count_autotrade_available INT NOT NULL,
    sub_price FLOAT NOT NULL,
    sub_date_period VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS trader.auth (
    auth_id serial PRIMARY KEY,
    person_name VARCHAR(255) NOT NULL,
    chat_id INT NOT NULL,
    active_sub_id INT NOT NULL REFERENCES trader.subscription(subscription_id),
    sub_start_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS trader.client_wallets (
    wallet_id serial PRIMARY KEY,
    auth_id INT NOT NULL REFERENCES trader.auth(auth_id),
    wallet_address VARCHAR(255),
    private_key VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS trader.follow (
    follow_id serial PRIMARY KEY,
    auth_id INT NOT NULL REFERENCES trader.auth(auth_id),
    follow_key_wallet VARCHAR(255) NOT NULL,
    name_of_wallet VARCHAR(255) NOT NULL,
    count_coll_done INT DEFAULT 0,
    count_autotrade_done INT DEFAULT 0,
    date_start_follow DATE NOT NULL,
    tracking_status BOOLEAN NOT NULL
);

INSERT INTO trader.subscription (sub_name, sub_description, follow_key_available, count_coll_available, count_autotrade_available, sub_price, sub_date_period)
VALUES
    ('Starter', 'Идеально для начинающих трейдеров! 😀🚀?nПолучите доступ к базовым функциям и начните свой путь в мире криптотрейдинга.🔰⚡?nЭта подписка даст вам возможность получить первый опыт и понять, подходит ли вам этот вид деятельности. 🔑🌐🏁🎯', 10, 50, 10, 9.99, '1 month'),
    ('Pro', 'Усильте свою торговую стратегию с Pro-подпиской! 💪🚀?nПолучите расширенные возможности слежения и автоматизации сделок.🎯💥?nВы сможете отслеживать больше активов, использовать дополнительные индикаторы и настраивать автоматические правила торговли. 📈📊🤖⚙️', 15, 200, 30, 29.99, '2 month'),
    ('Premium', 'Станьте профессионалом с Premium-подпиской! 🏆👑?nРасширенный доступ ко всем функциям и эксклюзивная поддержка экспертов.🥇🌟 Вы получите доступ ко всем возможностям платформы, а также личную консультацию от опытных трейдеров. 🔐📚💎✨', 20, 500, 100, 59.99, '3 month'),
    ('Enterprise', 'Масштабируйте свою торговлю с Enterprise-подпиской! 🚀🌍?nИдеально для крупных трейдеров и институциональных инвесторов.🏢💼?nЭта подписка включает специальные функции для управления большими портфелями и интеграции с корпоративными системами. 📂⚙️🔓⚡', 30, 1500, 500, 99.99, '6 month'),
    ('Lifetime', 'Единовременная выгодная инвестиция! 💎💰🤑?nПолучите самый длительный доступ ко всем функциям и забудте про подписку на год.🔥🔥?nЭто лучший выбор для тех, кто планирует торговать криптовалютами на долгосрочной основе. ♾️🔒🔑🏆', 45, 5000, 2000, 199.99, '1 year'),
    ('Default', 'Стандартная бесплатная подписка. 🆓🔍✅?nОграниченный доступ к базовым функциям для ознакомления с платформой.🕰️⌛?nЭта подписка позволит вам протестировать платформу и определить, нужны ли вам дополнительные возможности. ⏳🔑', 5, 15, 0, 0.0, '1 year');
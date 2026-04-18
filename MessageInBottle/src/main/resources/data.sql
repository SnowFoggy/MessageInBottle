INSERT INTO users (username, nickname, password, created_at)
SELECT 'demo', '演示用户', '123456', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'demo');

INSERT INTO wallets (user_id, balance, updated_at)
SELECT id, 88.8, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000
FROM users
WHERE username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM wallets WHERE user_id = users.id);

INSERT INTO tasks (title, category, description, amount, deadline, task_image_url, publisher_id, publisher_name, publish_time_text, status, accepter_id, review_status, completed, completion_proof_url, created_at)
SELECT '帮取快递', '校园', '去学校北门驿站取一个中号快递，送到 3 号宿舍楼楼下即可。', 8.0, '今天 18:30', NULL, id, '演示用户', '10分钟前', 'OPEN', NULL, '进行中', b'0', NULL, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000
FROM users WHERE username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM tasks WHERE title = '帮取快递');

INSERT INTO tasks (title, category, description, amount, deadline, task_image_url, publisher_id, publisher_name, publish_time_text, status, accepter_id, review_status, completed, completion_proof_url, created_at)
SELECT '食堂拼单代拿', '跑腿', '已经下好单，需要在一食堂窗口帮忙取餐并送到教学楼 A 区。', 12.5, '今天 19:10', NULL, id, '演示用户', '25分钟前', 'OPEN', NULL, '进行中', b'0', NULL, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000
FROM users WHERE username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM tasks WHERE title = '食堂拼单代拿');


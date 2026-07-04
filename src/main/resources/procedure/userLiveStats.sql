DELIMITER //

CREATE PROCEDURE IF NOT EXISTS get_user_live_stats(
    IN p_user_id INT
)
BEGIN
WITH RECURSIVE
    active_ranges AS (
        SELECT
    DATE(ul.login_time) AS start_day,
    LEAST(DATE(ul.logout_time), CURDATE()) AS end_day
FROM user_login ul
WHERE ul.user_id = p_user_id
  AND ul.logout_time >= ul.login_time
  AND ul.login_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY)

UNION ALL

SELECT
    DATE(us.login_time) AS start_day,
    CURDATE() AS end_day
FROM user_session us
WHERE us.user_id = p_user_id
  AND us.login_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
    ),

    active_days_recursive AS (
SELECT
    start_day AS streak_day,
    end_day
FROM active_ranges
WHERE start_day <= end_day

UNION ALL

SELECT
    DATE_ADD(streak_day, INTERVAL 1 DAY),
    end_day
FROM active_days_recursive
WHERE streak_day < end_day
    ),

    active_days AS (
SELECT DISTINCT streak_day
FROM active_days_recursive
    ),

    excuse_ranges AS (
SELECT
    ue.start_date AS start_day,
    LEAST(
    DATE_ADD(ue.start_date, INTERVAL ue.extra_days DAY),
    CURDATE()
    ) AS end_day
FROM user_excuses ue
WHERE ue.user_id = p_user_id
  AND ue.start_date <= CURDATE()
  AND ue.extra_days >= 0
    ),

    excuse_days_recursive AS (
SELECT
    start_day AS streak_day,
    end_day
FROM excuse_ranges
WHERE start_day <= end_day

UNION ALL

SELECT
    DATE_ADD(streak_day, INTERVAL 1 DAY),
    end_day
FROM excuse_days_recursive
WHERE streak_day < end_day
    ),

    excuse_days AS (
SELECT DISTINCT streak_day
FROM excuse_days_recursive
    ),

    effective_excuse_days AS (
SELECT ed.streak_day
FROM excuse_days ed
    LEFT JOIN active_days ad
ON ad.streak_day = ed.streak_day
WHERE ad.streak_day IS NULL
    ),

    non_break_days AS (
SELECT
    streak_day,
    1 AS is_active
FROM active_days

UNION ALL

SELECT
    streak_day,
    0 AS is_active
FROM effective_excuse_days
    ),

    numbered_days AS (
SELECT
    streak_day,
    is_active,
    DATE_SUB(
    streak_day,
    INTERVAL ROW_NUMBER() OVER (ORDER BY streak_day) DAY
    ) AS streak_group
FROM non_break_days
WHERE streak_day <= CURDATE()
    ),

    anchor AS (
SELECT MAX(streak_day) AS anchor_day
FROM numbered_days
WHERE streak_day >= DATE_SUB(CURDATE(), INTERVAL 1 DAY)
    ),

    anchor_group AS (
SELECT nd.streak_group
FROM numbered_days nd
    JOIN anchor a
ON a.anchor_day = nd.streak_day
    ),

    streak_result AS (
SELECT COALESCE(SUM(nd.is_active), 0) AS login_streak
FROM numbered_days nd
    JOIN anchor_group ag
ON ag.streak_group = nd.streak_group
    ),

    play_time_result AS (
SELECT
    COALESCE((
    SELECT SUM(TIMESTAMPDIFF(SECOND, ul.login_time, ul.logout_time))
    FROM user_login ul
    WHERE ul.user_id = p_user_id
    AND ul.logout_time >= ul.login_time
    ), 0)
    +
    COALESCE((
    SELECT SUM(TIMESTAMPDIFF(SECOND, us.login_time, NOW()))
    FROM user_session us
    WHERE us.user_id = p_user_id
    AND us.login_time <= NOW()
    ), 0) AS play_time
    ),

    last_seen_result AS (
SELECT MAX(last_seen_at) AS last_seen_at
FROM (
    SELECT MAX(ul.logout_time) AS last_seen_at
    FROM user_login ul
    WHERE ul.user_id = p_user_id

    UNION ALL

    SELECT NOW() AS last_seen_at
    FROM user_session us
    WHERE us.user_id = p_user_id
    ) x
    )

SELECT
    p_user_id AS user_id,
    ptr.play_time,
    COALESCE(sr.login_streak, 0) AS login_streak,
    lsr.last_seen_at,
    EXISTS (
        SELECT 1
        FROM user_session us
        WHERE us.user_id = p_user_id
    ) AS online
FROM play_time_result ptr
         LEFT JOIN streak_result sr ON TRUE
         LEFT JOIN last_seen_result lsr ON TRUE;
END //

DELIMITER ;
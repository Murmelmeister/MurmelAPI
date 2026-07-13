DELIMITER //

CREATE PROCEDURE IF NOT EXISTS end_user_session(
    IN p_user_id INT
)
BEGIN
    SET @login_id = UUID_v4();

    INSERT INTO user_login (id, user_id, login_time, ip_address, client_brand, protocol_version)
    SELECT @login_id, user_id, login_time, ip_address, client_brand, protocol_version
    FROM user_session
    WHERE user_id = p_user_id
    RETURNING id, user_id, login_time, logout_time, ip_address, client_brand, protocol_version;

    DELETE FROM user_session
    WHERE user_id = p_user_id;
END; //

DELIMITER ;

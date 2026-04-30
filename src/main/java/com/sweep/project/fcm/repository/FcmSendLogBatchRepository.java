package com.sweep.project.fcm.repository;

import com.sweep.project.fcm.domain.FcmSendLog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FcmSendLogBatchRepository {        //jdbcTemplate,  FCM 로그 batch로 insert

    private static final int BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(List<FcmSendLog> logs) {
        String sql = """
                INSERT INTO fcm_send_log
                (member_id, alarm_id, alarm_type, token, title, body, firebase_message_id, sent_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        for (int i = 0; i < logs.size(); i += BATCH_SIZE) {
            List<FcmSendLog> batch = logs.subList(i, Math.min(i + BATCH_SIZE, logs.size()));

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int index) throws SQLException {
                    FcmSendLog log = batch.get(index);

                    ps.setLong(1, log.getMemberId());

                    if (log.getAlarmId() == null) {
                        ps.setNull(2, Types.BIGINT);
                    } else {
                        ps.setLong(2, log.getAlarmId());
                    }

                    ps.setString(3, log.getAlarmType().name());
                    ps.setString(4, log.getToken());
                    ps.setString(5, log.getTitle());
                    ps.setString(6, log.getBody());
                    ps.setString(7, log.getFirebaseMessageId());
                    ps.setTimestamp(8, Timestamp.valueOf(log.getSentAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
        }
    }
}

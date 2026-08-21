CREATE SEQUENCE IF NOT EXISTS chat_messages_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT DEFAULT NEXT VALUE FOR chat_messages_seq PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    sequence_no INTEGER NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content VARCHAR(16000) NOT NULL
);

ALTER TABLE chat_messages ALTER COLUMN id SET DEFAULT NEXT VALUE FOR chat_messages_seq;
CREATE INDEX IF NOT EXISTS chat_messages_conversation_sequence_idx
    ON chat_messages (conversation_id, sequence_no);

MERGE INTO orders (id, owner_id, item, status, eta) KEY(id) VALUES
    ('12345', 'user1', '무선 이어폰', 'SHIPPING', '2026-08-20'),
    ('12346', 'user1', '키보드', 'SHIPPING', '2026-08-22'),
    ('12347', 'user1', '맥북', 'SHIPPING', '2026-08-24'),
    ('99999', 'user2', '스마트워치', 'SHIPPING', '2026-08-25');

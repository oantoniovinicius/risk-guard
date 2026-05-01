CREATE TABLE admin_decision_history (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    target_user_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    from_role VARCHAR(20),
    to_role VARCHAR(20),
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_decision_history_target_user_created_at
    ON admin_decision_history (target_user_id, created_at DESC);

CREATE INDEX idx_admin_decision_history_created_at
    ON admin_decision_history (created_at DESC);

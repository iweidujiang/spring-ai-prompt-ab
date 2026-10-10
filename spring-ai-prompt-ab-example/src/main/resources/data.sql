-- 预置实验：演示聊天 A/B 测试
INSERT INTO ab_experiment (experiment_key, description, status)
VALUES ('demo-chat', '演示聊天 A/B 测试', 'ACTIVE');

-- 变体 A：正式风格
INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active)
VALUES (1, 'formal', '你是一位专业、严谨的AI助手，请使用正式的语言回答用户问题。', 50, TRUE);

-- 变体 B：活泼风格
INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active)
VALUES (1, 'casual', '你是一个活泼、友好的AI助手，请用轻松幽默的语气回答用户问题～', 50, TRUE);

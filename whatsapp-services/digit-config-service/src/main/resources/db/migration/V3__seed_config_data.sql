-- ============================================================
-- V3: Seed OOTB config data for Bidirectional WhatsApp
-- ============================================================

-- Config Set: Default WhatsApp Config Set
INSERT INTO eg_config_set (id, tenant_id, name, code, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cs-default-whatsapp', 'default', 'Default WhatsApp Config Set', 'DEFAULT_WHATSAPP', 'Out-of-the-box config set for bidirectional WhatsApp', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

-- ============================================================
-- 1. OOTB_TEMPLATES
-- ============================================================
INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-tpl-welcome', 'cs-default-whatsapp', 'default', 'OOTB_TEMPLATES', 'Welcome Message', 'WELCOME_MSG', 'prod', 'Welcome template sent on first WhatsApp interaction', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-tpl-welcome-v1', 'cfg-tpl-welcome', 'v1', '{"template":"Hello {{name}}! Welcome to {{cityName}} municipal services. How can we help you today?","type":"TEXT","locale":"en_IN"}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-tpl-otp', 'cs-default-whatsapp', 'default', 'OOTB_TEMPLATES', 'OTP Message', 'OTP_MSG', 'prod', 'OTP verification template', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-tpl-otp-v1', 'cfg-tpl-otp', 'v1', '{"template":"Your OTP is {{otp}}. Valid for {{validMinutes}} minutes. Do not share this with anyone.","type":"TEXT","locale":"en_IN"}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-tpl-payment', 'cs-default-whatsapp', 'default', 'OOTB_TEMPLATES', 'Payment Receipt', 'PAYMENT_RECEIPT', 'prod', 'Payment confirmation template', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-tpl-payment-v1', 'cfg-tpl-payment', 'v1', '{"template":"Payment of Rs. {{amount}} received for {{serviceName}}. Receipt No: {{receiptNumber}}. Thank you, {{name}}!","type":"TEXT","locale":"en_IN"}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-tpl-complaint', 'cs-default-whatsapp', 'default', 'OOTB_TEMPLATES', 'Complaint Status', 'COMPLAINT_STATUS', 'prod', 'Complaint status update template', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-tpl-complaint-v1', 'cfg-tpl-complaint', 'v1', '{"template":"Complaint {{complaintId}} status: {{status}}. Updated on {{updateDate}}. For queries, reply HELP.","type":"TEXT","locale":"en_IN"}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

-- ============================================================
-- 2. OOTB_TEMPLATE_BINDINGS
-- ============================================================
INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-bind-main', 'cs-default-whatsapp', 'default', 'OOTB_TEMPLATE_BINDINGS', 'Template Bindings', 'TEMPLATE_BINDINGS', 'prod', 'Maps eventType+channel to template configCode', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-bind-main-v1', 'cfg-bind-main', 'v1', '{"bindings":[{"eventType":"USER_ONBOARD","channel":"WHATSAPP","templateCode":"WELCOME_MSG"},{"eventType":"OTP_VERIFY","channel":"WHATSAPP","templateCode":"OTP_MSG"},{"eventType":"OTP_VERIFY","channel":"SMS","templateCode":"OTP_MSG"},{"eventType":"PAYMENT_SUCCESS","channel":"WHATSAPP","templateCode":"PAYMENT_RECEIPT"},{"eventType":"COMPLAINT_UPDATE","channel":"WHATSAPP","templateCode":"COMPLAINT_STATUS"}]}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

-- ============================================================
-- 3. EVENT_CHANNELS
-- ============================================================
INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-channels', 'cs-default-whatsapp', 'default', 'EVENT_CHANNELS', 'Channel Definitions', 'CHANNEL_DEFS', 'prod', 'Supported notification channels with provider config', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-channels-v1', 'cfg-channels', 'v1', '{"channels":[{"code":"WHATSAPP","name":"WhatsApp","enabled":true,"provider":"META_CLOUD_API","maxRetries":3,"retryDelayMs":5000,"rateLimitPerSec":50},{"code":"SMS","name":"SMS","enabled":true,"provider":"DEFAULT_SMS_GW","maxRetries":2,"retryDelayMs":3000,"rateLimitPerSec":100},{"code":"EMAIL","name":"Email","enabled":true,"provider":"SMTP","maxRetries":2,"retryDelayMs":10000,"rateLimitPerSec":200},{"code":"PUSH","name":"Push Notification","enabled":false,"provider":"FCM","maxRetries":1,"retryDelayMs":1000,"rateLimitPerSec":500}]}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

-- ============================================================
-- 4. EVENT_CATEGORY_MAP
-- ============================================================
INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-catmap', 'cs-default-whatsapp', 'default', 'EVENT_CATEGORY_MAP', 'Event Category Mapping', 'EVENT_CATEGORIES', 'prod', 'Maps event types to categories for routing and compliance', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-catmap-v1', 'cfg-catmap', 'v1', '{"categories":[{"eventType":"PAYMENT_SUCCESS","category":"TRANSACTIONAL","priority":"HIGH"},{"eventType":"PAYMENT_FAILURE","category":"TRANSACTIONAL","priority":"HIGH"},{"eventType":"OTP_VERIFY","category":"TRANSACTIONAL","priority":"CRITICAL"},{"eventType":"COMPLAINT_UPDATE","category":"SERVICE","priority":"MEDIUM"},{"eventType":"USER_ONBOARD","category":"SERVICE","priority":"LOW"},{"eventType":"CAMPAIGN_MSG","category":"PROMOTIONAL","priority":"LOW"},{"eventType":"SURVEY_INVITE","category":"PROMOTIONAL","priority":"LOW"}]}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

-- ============================================================
-- 5. LANGUAGE_STRATEGY
-- ============================================================
INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-lang', 'cs-default-whatsapp', 'default', 'LANGUAGE_STRATEGY', 'Language Strategy', 'LANG_STRATEGY', 'prod', 'Locale resolution rules and supported locales', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-lang-v1', 'cfg-lang', 'v1', '{"defaultLocale":"en_IN","fallbackChain":["user_preference","tenant_default","system_default"],"supportedLocales":["en_IN","hi_IN","kn_IN","ta_IN","te_IN","mr_IN","bn_IN","gu_IN","pa_IN","ml_IN","od_IN"],"tenantOverrides":{}}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

-- ============================================================
-- 6. FEATURE_FLAGS
-- ============================================================
INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, environment, description, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cfg-flags', 'cs-default-whatsapp', 'default', 'FEATURE_FLAGS', 'Feature Flags', 'FEATURE_FLAGS', 'prod', 'Runtime feature toggles with tenant-level overrides', 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, created_by, created_time, last_modified_by, last_modified_time)
VALUES ('cv-flags-v1', 'cfg-flags', 'v1', '{"flags":{"whatsapp_bidirectional_enabled":true,"template_preview_enabled":true,"multilingual_enabled":true,"schema_validation_enabled":false,"config_set_activation_enabled":true,"event_category_routing_enabled":true,"rate_limiting_enabled":false,"audit_logging_enabled":true},"tenantOverrides":{}}', NULL, 'ACTIVE', 'SYSTEM', 1700000000000, 'SYSTEM', 1700000000000);

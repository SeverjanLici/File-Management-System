-- Create databases for each service
CREATE DATABASE user_db;
CREATE DATABASE document_db;
CREATE DATABASE file_db;
CREATE DATABASE ai_db;
CREATE DATABASE keycloak;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE user_db TO docplatform;
GRANT ALL PRIVILEGES ON DATABASE document_db TO docplatform;
GRANT ALL PRIVILEGES ON DATABASE file_db TO docplatform;
GRANT ALL PRIVILEGES ON DATABASE ai_db TO docplatform;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO docplatform;

-- Keycloak needs full schema access
\c keycloak
GRANT ALL ON SCHEMA public TO docplatform;

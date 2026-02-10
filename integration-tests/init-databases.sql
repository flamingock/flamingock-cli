-- Integration test databases for Flamingock CLI
-- Each test JAR type gets its own database for isolation

CREATE DATABASE IF NOT EXISTS flamingock_test_springboot;
CREATE DATABASE IF NOT EXISTS flamingock_test_standalone;

-- Grant privileges to the test user on both databases
GRANT ALL PRIVILEGES ON flamingock_test_springboot.* TO 'flamingock'@'%';
GRANT ALL PRIVILEGES ON flamingock_test_standalone.* TO 'flamingock'@'%';

FLUSH PRIVILEGES;

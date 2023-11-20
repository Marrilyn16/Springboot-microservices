DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'order-service') THEN
            CREATE DATABASE "order-service";
        END IF;
    END $$;
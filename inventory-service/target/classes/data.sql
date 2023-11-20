DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'inventory-service') THEN
            CREATE DATABASE "inventory-service";
        END IF;
    END $$;

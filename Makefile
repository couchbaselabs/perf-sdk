database:
	docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password --name timedb timescale/timescaledb:latest-pg14
	sleep 10;
	docker exec timedb psql -U postgres -c "CREATE DATABASE perf;"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS runs (id uuid PRIMARY KEY, datetime timestamp, params jsonb);"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS buckets (time TIMESTAMPTZ NOT NULL, run_id uuid, operations_total int, operations_success int, operations_failed int, operations_incomplete int, duration_min_us int, duration_max_us int, duration_average_us int, duration_p50_us int, duration_p95_us int, duration_p99_us int);"


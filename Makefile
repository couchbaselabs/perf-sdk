dbPerf:
	docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password --network perf --name timedb timescale/timescaledb:latest-pg14
	sleep 10;
	docker exec timedb psql -U postgres -c "CREATE DATABASE perf;"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS runs (id uuid PRIMARY KEY, datetime timestamp, params jsonb);"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS buckets (time TIMESTAMPTZ NOT NULL, run_id uuid, operations_total int, operations_success int, operations_failed int, operations_incomplete int, duration_min_us int, duration_max_us int, duration_average_us int, duration_p50_us int, duration_p95_us int, duration_p99_us int);"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS metrics (initiated TIMESTAMPTZ NOT NULL, run_id uuid, metric text);"

db:
	docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password --name timedb timescale/timescaledb:latest-pg14
	sleep 10;
	docker exec timedb psql -U postgres -c "CREATE DATABASE perf;"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS runs (id uuid PRIMARY KEY, datetime timestamp, params jsonb);"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS buckets (time TIMESTAMPTZ NOT NULL, run_id uuid, operations_total int, operations_success int, operations_failed int, operations_incomplete int, duration_min_us int, duration_max_us int, duration_average_us int, duration_p50_us int, duration_p95_us int, duration_p99_us int);"
	docker exec timedb psql -U postgres -d perf -c "CREATE TABLE IF NOT EXISTS metrics (initiated TIMESTAMPTZ NOT NULL, run_id uuid, metric text);"

truncate:
	docker exec timedb psql -U postgres -d perf -c "TRUNCATE TABLE runs;"
	docker exec timedb psql -U postgres -d perf -c "TRUNCATE TABLE buckets;"
	docker exec timedb psql -U postgres -d perf -c "TRUNCATE TABLE metrics;"

p3full:
	docker run --rm --network perf -d -p 8060:8060 --name pythonPerformer performer/python3
	docker run --rm --network perf -d -v /Users/charliehayes/Documents/GitHub/perf-sdk/test-suites/pythonInsert.yaml:/testSuite.yaml driver /testSuite.yaml

p4full:
	docker run --rm --network perf -d -p 8060:8060 --name pythonPerformer performer/python4
    docker run --rm --network perf -d -v /Users/charliehayes/Documents/GitHub/perf-sdk/test-suites/pythonInsert.yaml:/testSuite.yaml driver /testSuite.yaml

pp3:
	docker run --rm --network host -p 8060:8060 --name pythonPerformer performer/python3

pd3:
	docker run --rm --network perf -v /Users/charliehayes/Documents/GitHub/perf-sdk/test-suites/pythonInsert.yaml:/testSuite.yaml driver /testSuite.yaml

jp:
	docker run --rm --network perf -p 8060:8060 --name performer performer-java3.2.6


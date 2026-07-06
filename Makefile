.DEFAULT_GOAL := help

DB_NAME ?= baltnami
DB_USER ?= postgres

.PHONY: help db-create db-schema db-seed db-setup rabbitmq \
	backend-run backend-build backend-test backend-check \
	frontend-install frontend-dev frontend-build

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-18s\033[0m %s\n", $$1, $$2}'

db-create: ## Create the local PostgreSQL database and enable pgcrypto
	psql -U $(DB_USER) -c "CREATE DATABASE $(DB_NAME);"
	psql -U $(DB_USER) -d $(DB_NAME) -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"

db-schema: ## Apply the database schema
	psql -U $(DB_USER) -d $(DB_NAME) -f backend/db/schema.sql

db-seed: ## Load sample seed data
	psql -U $(DB_USER) -d $(DB_NAME) -f backend/db/seed.sql

db-setup: db-create db-schema ## Create the database and apply the schema

rabbitmq: ## Start a local RabbitMQ broker (with management UI) via Docker
	docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management

backend-run: ## Run the backend (Spring Boot)
	cd backend && ./gradlew bootRun

backend-build: ## Compile and package the backend
	cd backend && ./gradlew build

backend-test: ## Run backend tests (requires Docker for Testcontainers)
	cd backend && ./gradlew test

backend-check: ## Run backend tests + JaCoCo coverage check (minimum 70%)
	cd backend && ./gradlew check

frontend-install: ## Install frontend dependencies
	cd frontend && npm install

frontend-dev: ## Run the frontend dev server
	cd frontend && npm run dev

frontend-build: ## Build the frontend for production
	cd frontend && npm run build

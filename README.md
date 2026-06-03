# AI Meeting Summarizer

A microservices platform that accepts meeting transcript uploads and automatically generates structured AI summaries — including action items, decisions, and blockers — using OpenAI.

## Architecture

Two Spring Boot services communicate asynchronously via Kafka and share a PostgreSQL database.

```
Client
  │
  ▼
meeting-api-service  (port 8080)
  │  REST API, JWT auth
  │  Uploads video to S3 → saves Meeting → publishes meeting.uploaded event (with S3 URL)
  │
  ▼ Kafka
meeting-processor-service (Kafka)
     → starts AWS Transcribe job (reads from S3, returns immediately)

AWS Transcribe
     → publishes COMPLETED/FAILED to SNS topic

meeting-processor-service (SQS poller)
     → receives SNS notification via SQS
     → downloads transcript from S3
     → OpenAI chat/completions (summarization)
     → writes MeetingSummary to DB
```

### Services

| Service | Role |
|---|---|
| `meeting-api-service` | HTTP API: auth, transcript upload, meeting/summary retrieval |
| `meeting-processor-service` | Background worker: consumes Kafka events, calls OpenAI, stores summaries |

### Infrastructure

- **PostgreSQL 16** — shared database
- **Apache Kafka** (Confluent 7.6) — async event bus
- **OpenAI API** — `gpt-4o-mini` by default (configurable)

## API Reference

All endpoints except `/auth/**` require a `Bearer <token>` header.

### Auth

```
POST /auth/register    { "email": "...", "password": "..." }
POST /auth/login       { "email": "...", "password": "..." }  →  { "token": "..." }
```

### Meetings

```
POST   /meetings/upload          multipart: title (string), file (video: mp4, mov, m4a, webm, wav…)
GET    /meetings                 list all meetings for authenticated user
GET    /meetings/{id}            get a single meeting
GET    /meetings/{id}/summary    get the AI-generated summary
```

> **Transcription** is handled by AWS Transcribe, which reads directly from S3 and has no practical file size limit. Transcription of long recordings is asynchronous and may take several minutes.

### Summary response shape

```json
{
  "shortSummary": "1-2 sentence overview",
  "detailedSummary": "comprehensive paragraph",
  "actionItems": ["..."],
  "decisions": ["..."],
  "blockers": ["..."]
}
```

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Run everything with Docker Compose

```bash
# Copy and configure environment
cp .env.example .env   # edit JWT_SECRET and OPENAI_API_KEY

# Build and start all services
docker compose up --build
```

The API will be available at `http://localhost:8080`.

### Local development (services only, infra in Docker)

```bash
# Start only PostgreSQL and Kafka
docker compose up postgres zookeeper kafka

# In separate terminals:
./gradlew :meeting-api-service:bootRun
./gradlew :meeting-processor-service:bootRun
```

### Environment variables

| Variable | Default | Required |
|---|---|---|
| `JWT_SECRET` | insecure default | **Yes** — must be ≥ 32 chars in production |
| `OPENAI_API_KEY` | _(empty)_ | No — returns placeholder summary if unset |
| `OPENAI_MODEL` | `gpt-4o-mini` | No |
| `AWS_ACCESS_KEY_ID` | _(empty)_ | **Yes** — or use IAM role/instance profile |
| `AWS_SECRET_ACCESS_KEY` | _(empty)_ | **Yes** — or use IAM role/instance profile |
| `AWS_REGION` | `us-east-1` | No |
| `S3_BUCKET_NAME` | `meeting-videos` | No |
| `TRANSCRIBE_SNS_TOPIC_ARN` | _(empty)_ | **Yes** — SNS topic ARN for Transcribe completion events |
| `TRANSCRIBE_ROLE_ARN` | _(empty)_ | **Yes** — IAM role ARN Transcribe assumes to publish to SNS |
| `SQS_QUEUE_URL` | _(empty)_ | **Yes** — SQS queue URL subscribed to the SNS topic |
| `DB_HOST` | `localhost` | No |
| `DB_NAME` | `meetingdb` | No |
| `DB_USER` | `meetinguser` | No |
| `DB_PASSWORD` | `meetingpass` | No |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | No |

> If `OPENAI_API_KEY` is not set, Whisper transcription and summarization both return placeholders — useful for local dev.

## Build & Test

```bash
# Build all modules
./gradlew build

# Build skipping tests
./gradlew build -x test

# Run all tests
./gradlew test

# Run tests for one module
./gradlew :meeting-api-service:test
```

## Quick Start Example

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret123"}'

# 2. Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret123"}' | jq -r .token)

# 3. Upload a video
curl -X POST http://localhost:8080/meetings/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "title=Weekly Sync" \
  -F "file=@meeting.mp4"

# 4. Retrieve the summary (may take a few seconds to process)
curl http://localhost:8080/meetings/{id}/summary \
  -H "Authorization: Bearer $TOKEN"
```

## CI / CD

GitLab CI pipeline (`.gitlab-ci.yml`) runs on every push:

| Stage | What it does |
|---|---|
| `build` | Compiles both services via Gradle |
| `test` | Runs all tests, publishes JUnit report |
| `docker` | Builds and pushes images to GitLab Container Registry _(main branch only)_ |
| `deploy` | Rolls out to staging via `kubectl` _(manual trigger, main branch only)_ |

## Database Schema

Hibernate auto-creates tables on startup.

| Table | Key columns |
|---|---|
| `users` | `id` (UUID), `email`, `password_hash`, `role` |
| `meetings` | `id`, `user_id`, `title`, `transcript_content`, `status`, `created_at` |
| `meeting_summaries` | `id`, `meeting_id`, `short_summary`, `detailed_summary`, `action_items`, `decisions`, `blockers` |

Meeting status lifecycle: `UPLOADED` → `PROCESSING` → `COMPLETED` (or `FAILED`).

## AWS infrastructure setup

Three resources are needed before running the processor service.

**1. SNS topic**
```bash
aws sns create-topic --name meeting-transcribe-notifications
# note the TopicArn
```

**2. SQS queue subscribed to the topic**
```bash
aws sqs create-queue --queue-name meeting-transcribe-queue
# note the QueueUrl and QueueArn

aws sns subscribe \
  --topic-arn <TopicArn> \
  --protocol sqs \
  --notification-endpoint <QueueArn>

# Allow SNS to send to SQS (attach policy to the queue)
aws sqs set-queue-attributes \
  --queue-url <QueueUrl> \
  --attributes '{"Policy":"{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"sns.amazonaws.com\"},\"Action\":\"sqs:SendMessage\",\"Resource\":\"<QueueArn>\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"<TopicArn>\"}}}]}"}'
```

**3. IAM role for Transcribe → SNS**

Create a role trusted by `transcribe.amazonaws.com` with an inline policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [{ "Effect": "Allow", "Action": "sns:Publish", "Resource": "<TopicArn>" }]
}
```
```bash
# note the role ARN
```

Set `TRANSCRIBE_SNS_TOPIC_ARN`, `TRANSCRIBE_ROLE_ARN`, and `SQS_QUEUE_URL` from the values above.

## Roadmap

- Support speaker diarization (AWS Transcribe has built-in speaker labels)

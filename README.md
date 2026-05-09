[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/Tk34cou5)

# 📂 Company Document Portal
### A Distributed Microservices Platform for Intelligent Document Management & Observability

# 👥 Team Members
#### Matteo Flores – s337579
#### Giosuè Pinto – s342711
#### Francesco Palmisani – s343429
#### Tommaso Mancuso - s342958

## 📝 Project Overview
This project consists of the development of a Company Document Portal. Designed as a **distributed microservices-based application**, the system ensures high **scalability, availability, and maintainability**. It enables secure uploading, organizing, processing, and sharing of internal documents among authenticated users.  The platform is built on a **Zero-Trust security model**, integrating **Keycloak** for Identity and Access Management (**IAM**). It enforces granular **Access Control Lists** (ACL) based on user roles—Admin, Manager, and Staff—and departmental membership.

### Core Pillars of the System
#### *AI Integration & RAG:* The system automatically enriches uploaded documents with LLM-generated metadata, including summaries, tags, and sensitivity classification. It also features a secure "Chat with your Data" capability via Retrieval-Augmented Generation (RAG), ensuring users only retrieve information they are authorized to access.

#### *Event-Driven Architecture:* Utilizing Apache Kafka and the Transactional Outbox pattern, the system guarantees data consistency and decouples document uploads from heavy AI processing tasks.


#### *Full-Stack Observability:* A comprehensive monitoring suite is implemented to track system health in real-time. This includes Prometheus for metrics, Loki for log aggregation, and Grafana for centralized dashboards.  

#### *Distributed Tracing:* By integrating OpenTelemetry, the system provides end-to-end visibility of request flows across service boundaries, helping to identify latency bottlenecks and failure points in complex AI pipelines.

#### *AI-Specific Monitoring:* The integration of Langtrace allows for deep inspection of AI interactions, tracking prompt performance, token consumption, and execution latencies.


##  🏗️ System Architecture

### The system is built as a set of decoupled microservices that communicate via REST APIs and asynchronous messaging.

### Microservices
* *API Gateway:* The single entry point for all client requests, responsible for routing and load balancing.  
* *IAM (Keycloak):* Handles Identity and Access Management, supporting both User Authentication and Machine-to-Machine (M2M) communication.
* *File Service (Kotlin/Spring Boot):* Manages the core document lifecycle, including resumable uploads (TUS protocol), versioning, and Access Control Lists (ACL). It acts as a producer for the Transactional Outbox.
* *AI Processing Service (Kotlin/Spring Boot):* An autonomous consumer that processes events from Kafka. It performs: 
    * Content extraction and LLM-based metadata generation.
    * Vector embedding generation and storage.
    * Retrieval-Augmented Generation (RAG) for secure user queries.
* *User Detail Service:* Manages extended user profiles, including roles (Admin, Manager, Staff) and departmental affiliations required for permission enforcement.  
* *Infrastructure & Data Tier - Apache Kafka:* The message broker used for event-driven communication between the File Service and AI Service.PostgreSQL + pgvector: A relational database that also serves as a high-performance **Vector Store** for semantic search.
* *MinIO:* S3-compatible object storage for the physical persistence of documents.
* *Debezium (Kafka Connect):* Implements the Transactional Outbox pattern to ensure reliable message delivery by monitoring database changes.


## 📈 Observability & Monitoring

### To ensure the reliability and performance of our distributed system, we implemented a comprehensive observability stack based on industry standards.

### System Monitoring & Logging
* *Prometheus:* Collects system-wide metrics such as API response times, error rates, and resource usage across all microservices.
* *Loki:* Aggregates and stores logs from multiple services to provide a unified view of system behavior and facilitate troubleshooting.  
* *Grafana:* Provides centralized dashboards that allow administrators to monitor system health, inspect anomalies, and visualize document processing activity in real-time.  

### Distributed Tracing

* *OpenTelemetry:* All services are instrumented to propagate contextual information across service boundaries, enabling end-to-end visibility of request flows.  
* *Performance Optimization:* This allows for the precise identification of latency bottlenecks and the analysis of failure points, specifically within the asynchronous AI processing pipelines.  

### AI Tracing & Evaluation
* *Langtrace:* Integrated to provide deep insights into AI-driven features. It tracks interactions with LLMs, including prompt/response performance, token consumption, and quota usage.  
* *Output Analysis:* Supports the analysis and comparison of AI-generated metadata over time to ensure consistency and quality. 

## 🔑 Key Features
### 📄 Intelligent Document Management
* *Resumable Uploads:* Support for large file uploads using the TUS protocol to ensure reliability even on unstable connections.
* *Multi-Format Support:* Native handling of PDF, DOCX, video, and image files.  
* *Metadata & Versioning:* Automatic and manual metadata association (tags, categories, summaries) with support for multiple immutable document versions.  
* *Search & Discovery:* Advanced filtering based on metadata, categories, and user permissions.  

### 🔐 Enterprise-Grade Security
* *Role-Based Access Control (RBAC):* Specific workflows and permissions for Admins, Managers, and Staff members.  
* *Granular ACL:* Access enforcement based on document ownership, departmental boundaries, and explicit sharing rules.
* *Zero-Trust Communication:* Secure Machine-to-Machine (M2M) interactions between microservices using OAuth2.

### 🤖 AI-Powered Workflow
* *Automated Enrichment:* Background processing for automatic generation of summaries, tags, and sensitivity classification (Public, Internal, Confidential).
* *Secure RAG (Retrieval-Augmented Generation):* A "Chat with your Data" feature that uses a Vector Store (pgvector) to provide context-aware answers.
* *Permission-Aware Retrieval:* The RAG pipeline strictly filters document context based on the user's specific access rights.

### 📊 Administrative Insights
* *Real-time Monitoring:* Detailed dashboards for tracking AI pipeline health, processing failures, and system metrics. 
* *Manual Intervention:* Capabilities for administrators to re-execute or override automated processing when necessary. 

## 🛠️ Core Technologies

| Category | Stack |
| :--- | :--- |
| **Language & Framework** | Kotlin, Spring Boot |
| **Messaging** | Apache Kafka |
| **Databases** | PostgreSQL + **pgvector** (Relational & Vector Store) |
| **Storage** | MinIO (S3-compatible) |
| **Security/IAM** | Keycloak (OAuth2, OIDC) |
| **Observability** | Grafana, Prometheus, Loki, OpenTelemetry |
| **AI Monitoring** | Langtrace |



## 🛠️ How to Run Tests

The project includes a comprehensive suite of automated tests. Since we use **Testcontainers**, you do not need to manually install databases or Kafka on your machine, but **Docker must be running**.

## 🧪 Testing Strategy
We implemented a robust testing strategy using JUnit 5, Mockito, and Testcontainers to validate the distributed nature of the system. The tests cover integration, security, and resilience scenarios.


### 1. Kafka Integration & Consumer Logic
File: AiKafkaConsumerTest.kt

Verifies that the AIProcessingService correctly connects to the Kafka broker, consumes messages from the file-upload.completed topic, and triggers the internal business logic. We use a Spy bean to verify the listener invocation without mocking the entire infrastructure.


### 2. Transactional Outbox Pattern
File: FileServiceOutboxTest.kt

Validates the producer side of the architecture. It ensures that when a textual file is uploaded, an event is strictly written to the outbox database table within the same transaction. It also verifies that non-textual files (e.g., images) do not trigger events.


### 3. Machine-to-Machine (M2M) Security
File: AiSafetyAndSecurityTest.kt

Verifies the Zero-Trust security model. The test intercepts outgoing HTTP calls from the AI Service to the File Service (for downloading files or updating metadata) and asserts that the Authorization: Bearer header is present and contains a valid OAuth2 token.


### 4. Resilience & Idempotency
File: AiIdempotencyTest.kt

Retry Mechanism: Simulates transient failures (e.g., network glitches) and verifies that the system retries the operation according to the configured BackOff policy before giving up.

Idempotency: Simulates the arrival of duplicate Kafka messages. The test ensures the system processes the request multiple times without crashing or corrupting the state.


### 5. RAG & Vector Store Integration
File: AIProcessorIntegrationTest.kt

Performs a full integration test of the RAG pipeline. It verifies that documents are correctly chunked, embeddings are generated (mocked LLM), stored in the PostgreSQL vector database, and correctly retrieved via similarity search.


## 🚀 How to Run

### 📋 Prerequisites & Environment
* ***Docker Desktop:*** Must be running to host the infrastructure and support Testcontainers.

* ***IntelliJ IDEA:*** The recommended IDE for managing and running the Kotlin/Spring Boot modules.

### 🛠️ Step-by-Step Setup

1. ***Launch Docker & IntelliJ IDEA***
2. ***Open the terminal in the project root and run:***
    ```bash 
    docker compose up -d
3.  ***Use IntelliJ to start the following services in order:***

    1. ApiGateway

    2. UserDetailService

    3. FileService

    4. AIProcessingService

4. ***Once the services are healthy, the main entry point is available at*** http://localhost:8080/.


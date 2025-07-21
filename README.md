# Textify API Service

Textify is a Spring Boot-based REST API for asynchronous file processing. It allows users to upload images or PDFs, tracks processing jobs, and provides download links for processed results. The service uses Google Cloud Storage for file management, Google Pub/Sub for job orchestration, and MySQL (Cloud SQL) for metadata persistence.

## Features

- **File Upload**: Accepts images (JPEG, PNG, GIF, BMP, WebP) and PDFs.
- **Job Tracking**: Each upload creates a job with a unique ID and status.
- **Google Cloud Integration**: Stores files in GCS buckets and publishes processing requests to Pub/Sub.
- **Status & Download**: Query job status and get download links for processed files.
- **Recent Jobs**: Paginated endpoint to view recent processing jobs.

## API Endpoints

| Method | Endpoint                       | Description                                 |
|--------|------------------------------- |---------------------------------------------|
| POST   | `/api/v1/jobs/upload`          | Upload a file for processing                |
| GET    | `/api/v1/jobs/{jobId}/status`  | Get status of a specific job                |
| GET    | `/api/v1/jobs/{jobId}/download`| Redirect to download processed file         |
| GET    | `/api/v1/jobs/recent`          | List recent jobs (pagination supported)     |

## Quick Start

1. **Clone the repository**  
   `git clone <repo-url>`

2. **Configure MySQL and Google Cloud**  
   - Set up a MySQL database and update credentials in [`application.properties`](api-service/src/main/resources/application.properties).
   - Provide Google Cloud credentials and bucket names in the same file.

3. **Build and Run**  
   ```sh
   cd api-service
   ./mvnw spring-boot:run
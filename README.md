# Textify

Textify is a scalable file processing application that uses a microservices architecture. It consists of a React front-end for user interaction, a Spring Boot API service to manage file uploads and job creation, and a Spring Boot worker service that processes tasks asynchronously using Google Cloud Pub/Sub.

## Tech stack 
<img src="https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" /><img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" /><img src="https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white" /><img src="https://img.shields.io/badge/Google_Cloud-4285F4?style=for-the-badge&logo=google-cloud&logoColor=white" />

## Project Structure

The project is divided into three main components:

* **`api-service`**: A Spring Boot application that exposes a REST API for clients to upload files and create processing jobs.

* **`worker-service`**: A Spring Boot application that subscribes to a Pub/Sub topic, processes jobs, and updates their status in MySQL.

* **`frontend`**: A React application that provides the user interface for uploading files and viewing job statuses in real-time.

## API Service

The `api-service` acts as the entry point for all client requests. Its primary responsibilities are:

* Receiving file upload requests from the front-end.

* Saving files to a storage service (e.g., Google Cloud Storage).

* Creating a new job entry in the MySQL database.

* Publishing a message to a Pub/Sub topic to signal the `worker-service` that a new job is ready for processing.

**Running the API Service**

1. Navigate to the `api-service` directory.

2. Ensure you have your environment variables for Firebase and Pub/Sub configured.

3. Run the application using Maven: mvn spring-boot:run

## Worker Service

The `worker-service` is an asynchronous microservice that handles the heavy lifting of your application.

* It is configured as a Pub/Sub subscriber, constantly listening for new messages on a specific topic.

* When a new message arrives (indicating a new job), it retrieves the job details.

* It performs the file processing task (e.g., text extraction, data analysis).

* It updates the job's status in the MySQL database to `IN_PROGRESS`, then to `COMPLETED` or `FAILED`.

**Running the Worker Service**

1. Navigate to the `worker-service` directory.

2. Ensure you have your environment variables for Pub/Sub configured.

3. Run the application using Maven: mvn spring-boot:run
## Frontend React App

The `frontend` provides a clean, responsive user interface for your application.

* Users can upload files through a simple form.

* Job statuses (PENDING, IN_PROGRESS, COMPLETED, FAILED) are automatically updated on the UI as the `worker-service` changes them in the database.

* Authentication is handled anonymously or via a custom token, providing a seamless user experience.

**Running the Frontend App**

1. Navigate to the `frontend` directory.

2. Ensure you have Node.js and npm installed.

3. Install the dependencies: npm install


4. Start the development server: npm start


## Getting Started

1. Clone this repository.

2. Set up your Google Cloud project with Pub/Sub, and Google Cloud Storage.

3. Configure your environment variables with the necessary credentials and project IDs for each service.

4. Start each of the three services as described above.

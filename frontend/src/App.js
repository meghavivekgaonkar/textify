import React, { useState, useEffect, useRef } from 'react';

// The main application component.
// This component handles file selection, upload, and polling for job status.
const App = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [userId, setUserId] = useState(null);
  const [jobId, setJobId] = useState(null);
  const [jobStatus, setJobStatus] = useState('IDLE');
  const [message, setMessage] = useState('Please select a file to upload.');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [downloadUrl, setDownloadUrl] = useState(null);
  
  // Use a ref to store the interval ID so we can clear it later.
  const intervalRef = useRef(null);

  // --- MOCK AUTHENTICATION & USER ID GENERATION ---
  useEffect(() => {
    // For local development, generate a unique ID for the user.
    // In a real application, this would be provided by a proper authentication service.
    const storedUserId = localStorage.getItem('textify-user-id');
    if (storedUserId) {
      setUserId(storedUserId);
    } else {
      const newUserId = `user-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      localStorage.setItem('textify-user-id', newUserId);
      setUserId(newUserId);
    }
  }, []);

  // This effect starts polling for job status once a jobId is set.
  useEffect(() => {
    // Only start polling if a jobId and userId are available
    if (jobId && userId) {
      // Start polling every 3 seconds.
      intervalRef.current = setInterval(() => {
        checkJobStatus();
      }, 3000);
      
      // Cleanup function to clear the interval when the component unmounts
      // or when the jobId changes.
      return () => {
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
        }
      };
    }
  }, [jobId, userId]);

  // Handle file selection from the input.
  const handleFileChange = (event) => {
    setSelectedFile(event.target.files[0]);
    setMessage(event.target.files[0] ? `File selected: ${event.target.files[0].name}` : 'Please select a file to upload.');
    setError(null);
    setDownloadUrl(null); // Clear any old download URL
  };

  // Handle the file upload process.
  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a file first.');
      return;
    }

    if (!userId) {
      setError('User ID not ready. Please try again.');
      return;
    }

    setIsLoading(true);
    setError(null);
    setMessage('Uploading...');

    const formData = new FormData();
    formData.append('file', selectedFile);
  
    // Construct the API URL with userId as a request parameter, matching the backend controller.
    const uploadApiUrl = `http://localhost:8080/api/v1/jobs/upload?userId=${userId}`;

    try {
      const response = await fetch(uploadApiUrl, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`API call failed: ${errorText}`);
      }
    
      const uploadData = await response.json();
      setJobId(uploadData.jobId);
      setJobStatus(uploadData.status);
      setMessage(`File uploaded successfully. Job ID: ${uploadData.jobId}. Polling for status...`);

    } catch (e) {
      console.error("Error during file upload:", e);
      setError(`Failed to create job: ${e.message}`);
      setMessage('Upload failed.');
      setIsLoading(false);
    } finally {
      setSelectedFile(null); // Clear the selected file
    }
  };


// Poll the API for the job status using the userId.
  const checkJobStatus = async () => {
    try {
      const statusResponse = await fetch(`http://localhost:8080/api/v1/jobs?userId=${userId}`);
      
      // Check if the response is not OK
      if (!statusResponse.ok) {
        throw new Error(`Status check failed with status: ${statusResponse.status}`);
      }

      // We expect a JSON response from a proper status check API.
      // The current backend API seems to be returning a redirect,
      // which is why the .json() call is failing.
      const statusData = await statusResponse.json();
      setJobStatus(statusData.status);
      
      // Update the message and stop polling if the job is complete or failed.
      if (statusData.status === 'COMPLETED') {
        setMessage(`Job ${jobId} is complete!`);
        setIsLoading(false);
        clearInterval(intervalRef.current);
        // Corrected download URL to match the new API path variable format
        setDownloadUrl(`http://localhost:8080/api/v1/jobs/${jobId}/download`);
      } else if (statusData.status === 'FAILED') {
        setMessage(`Job ${jobId} failed.`);
        setIsLoading(false);
        clearInterval(intervalRef.current);
      } else if (statusData.status === 'PROCESSING') {
        setMessage('Job is still processing...');
      } else if (statusData.status === 'NO_JOB_FOUND') {
        // Handle the case where the backend returns 'NO_JOB_FOUND'
        setMessage('No active job found for this user.');
        setIsLoading(false);
        clearInterval(intervalRef.current);
      }

    } catch (err) {
      setError(`Status check error: ${err.message}`);
      setIsLoading(false);
      clearInterval(intervalRef.current);
    }
  };

  // Determine the color of the status indicator based on the job status.
  const getStatusColor = (status) => {
    switch (status) {
      case 'UPLOADED':
      case 'PROCESSING':
        return 'text-yellow-500';
      case 'COMPLETED':
        return 'text-green-500';
      case 'FAILED':
        return 'text-red-500';
      case 'NO_JOB_FOUND':
        return 'text-gray-500';
      default:
        return 'text-gray-500';
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4 font-sans">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-2xl p-8 space-y-6">
        <h1 className="text-3xl font-bold text-center text-gray-800">File Uploader</h1>
        
        {/* File Input */}
        <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
          <input
            type="file"
            onChange={handleFileChange}
            className="block w-full text-sm text-gray-500
                       file:mr-4 file:py-2 file:px-4
                       file:rounded-full file:border-0
                       file:text-sm file:font-semibold
                       file:bg-indigo-50 file:text-indigo-600
                       hover:file:bg-indigo-100 cursor-pointer"
          />
        </div>

        {/* Upload Button */}
        <button
          onClick={handleUpload}
          disabled={!selectedFile || isLoading || !userId}
          className={`w-full py-3 rounded-lg text-lg font-semibold text-white
                      transition-colors duration-200 ease-in-out
                      ${!selectedFile || isLoading || !userId ? 'bg-gray-400 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700 shadow-md'}`}
        >
          {isLoading ? (
            <svg className="animate-spin h-5 w-5 mr-3 inline-block" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          ) : (
            'Upload File'
          )}
        </button>

        {/* Status Display */}
        <div className="text-center">
          {message && (
            <p className="text-gray-600">{message}</p>
          )}
          {error && (
            <p className="text-red-500 font-medium mt-2">{error}</p>
          )}
          {jobId && (
            <div className="mt-4 p-4 border rounded-lg bg-gray-50">
              <p className="text-gray-700 font-bold">Job Status</p>
              <p className={`text-2xl font-bold mt-1 ${getStatusColor(jobStatus)}`}>{jobStatus}</p>
              {downloadUrl && (
                <a href={downloadUrl} className="mt-2 inline-block text-white bg-green-500 hover:bg-green-600 font-bold py-2 px-4 rounded-lg transition-colors duration-200">
                  Download Processed File
                </a>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default App;

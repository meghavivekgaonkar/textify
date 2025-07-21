package com.textify.me.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
// Return 404 if this exception is thrown
@ResponseStatus(HttpStatus.NOT_FOUND) 
public class JobNotFoundException extends RuntimeException {

   public JobNotFoundException(String message) {
       super(message);
   }
   public JobNotFoundException() {
       super("Job not found.");
   }
}
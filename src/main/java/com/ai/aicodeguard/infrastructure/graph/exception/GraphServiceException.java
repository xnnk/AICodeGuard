package com.ai.aicodeguard.infrastructure.graph.exception;

public class GraphServiceException extends RuntimeException {
    public GraphServiceException(String message) {
        super(message);
    }

  public GraphServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}

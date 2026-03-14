package com.ragul.OrderService.config;

import com.ragul.OrderService.exception.BusinessException;
import com.ragul.OrderService.exception.ResourceNotFoundException;
import com.ragul.OrderService.exception.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.http.HttpStatus;

public class CustomFeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch(response.status()){
            case HttpStatus.SC_BAD_REQUEST -> new BusinessException(
                    "Bad request — method: " + methodKey
            );
            case HttpStatus.SC_NOT_FOUND -> new ResourceNotFoundException(
                    "Resource not found — method: " + methodKey
            );
            case HttpStatus.SC_UNPROCESSABLE_ENTITY -> new BusinessException(
                    "Business rule violation — method: " + methodKey
            );
            case HttpStatus.SC_SERVICE_UNAVAILABLE, HttpStatus.SC_INTERNAL_SERVER_ERROR-> new ServiceUnavailableException(
                    "Downstream service unavailable — method: " + methodKey
            );
            default -> new Exception("Generic error");
        };
    }

}
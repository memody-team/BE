package com.guru2.memody.config;

import com.guru2.memody.Exception.RegionWrongException;
import com.guru2.memody.Exception.UserAlreadyExistsException;
import com.guru2.memody.dto.ErrorResponse;
import com.guru2.memody.Exception.UserNameAlreadyExistsException;
import com.guru2.memody.entity.Region;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(new ErrorResponse(
                        "USER_ALREADY_EXISTS",
                        e.getMessage()
                ));
    }

    @ExceptionHandler(UserNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserNameAlreadyExists(
            UserNameAlreadyExistsException e
    ){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "USERNAME_ALREADY_EXIST",
                        e.getMessage()
                ));
    }

    @ExceptionHandler(RegionWrongException.class)
    public ResponseEntity<ErrorResponse> handleRegionWrongException(
            RegionWrongException e
    ){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "REGION_WRONG",
                        e.getMessage()
                ));
    }
}

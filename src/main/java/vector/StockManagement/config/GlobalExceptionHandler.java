package vector.StockManagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vector.StockManagement.exceptions.TenantException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("ACCESS DENIED - URI: {} {} | Tenant ID: {} | User: {} | Error: {}", 
                    method, requestUri, tenantId, getCurrentUser(), ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Access Denied");
        errorResponse.put("message", "You do not have permission to access this resource");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", requestUri);
        errorResponse.put("method", method);
        errorResponse.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("AUTHENTICATION FAILED - URI: {} {} | Tenant ID: {} | Error: {}", 
                    method, requestUri, tenantId, ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Authentication Failed");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", requestUri);
        errorResponse.put("method", method);
        errorResponse.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("BAD CREDENTIALS - URI: {} | Tenant ID: {} | Error: {}", 
                    requestUri, tenantId, ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Bad Credentials");
        errorResponse.put("message", "Invalid username or password");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", requestUri);
        errorResponse.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("ILLEGAL STATE - URI: {} | Tenant ID: {} | Error: {}", 
                    requestUri, tenantId, ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Illegal State");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", requestUri);
        errorResponse.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("VALIDATION ERROR - URI: {} | Tenant ID: {} | Errors: {}", 
                    requestUri, tenantId, ex.getBindingResult().getFieldErrors());
        
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        errors.put("timestamp", LocalDateTime.now());
        errors.put("path", requestUri);
        errors.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("BIND ERROR - URI: {} | Tenant ID: {} | Errors: {}", 
                    requestUri, tenantId, ex.getBindingResult().getFieldErrors());
        
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        errors.put("timestamp", LocalDateTime.now());
        errors.put("path", requestUri);
        errors.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("CONSTRAINT VIOLATION - URI: {} | Tenant ID: {} | Violations: {}", 
                    requestUri, tenantId, ex.getConstraintViolations());
        
        Map<String, Object> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        errors.put("timestamp", LocalDateTime.now());
        errors.put("path", requestUri);
        errors.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TenantException.class)
    public ResponseEntity<Map<String, Object>> handleTenantException(TenantException ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("TENANT EXCEPTION - URI: {} {} | Current Tenant ID: {} | Exception Tenant ID: {} | Operation: {} | User: {} | Error: {}", 
                    method, requestUri, tenantId, ex.getTenantId(), ex.getOperation(), getCurrentUser(), ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Tenant Access Error");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", requestUri);
        errorResponse.put("method", method);
        errorResponse.put("currentTenantId", tenantId);
        errorResponse.put("exceptionTenantId", ex.getTenantId());
        errorResponse.put("operation", ex.getOperation());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        HttpServletRequest request = getCurrentRequest();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        Long tenantId = TenantContext.getTenantId();
        
        logger.error("UNEXPECTED ERROR - URI: {} {} | Tenant ID: {} | User: {} | Error: {}", 
                    method, requestUri, tenantId, getCurrentUser(), ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", requestUri);
        errorResponse.put("method", method);
        errorResponse.put("tenantId", tenantId);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getCurrentUser() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
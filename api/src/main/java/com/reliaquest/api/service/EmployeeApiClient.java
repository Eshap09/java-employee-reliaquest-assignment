package com.reliaquest.api.service;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.ExternalApiException;
import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeApiClient {

    private final RestTemplate restTemplate;

    @Value("${mockserver.base-url: http://localhost:8112/api/v1/employee}")
    private String baseUrl;

//    @Retry(name = "externalApiRetry")
//    public List<Employee> getAllEmployees() {
//        try{
//            log.info("Fetching all employees from External Api");
//            ResponseEntity<ApiResponse<List<Employee>>> response = restTemplate.exchange(
//                    baseUrl,
//                    HttpMethod.GET,
//                    null,
//                    new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {}
//            );
//
//            if(response.getBody() !=null && response.getBody().getData()!=null){
//                log.info("Successfully fetched employees");
//                return response.getBody().getData();
//            }
//            return Collections.emptyList();
//        } catch (HttpClientErrorException.TooManyRequests ex){
//            log.error("Too many requests, will retry: {}", ex.getMessage());
//            throw new RuntimeException("Too many requests", ex);
//        }
//        catch (Exception ex){
//            log.error("Error fetching all employees: {}", ex.getMessage());
//            throw new RuntimeException("Failed to fetch employees", ex);
//        }
//    }

    @Retry(name = "externalApiRetry")
    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees from External Api");
        try {
            ResponseEntity<ApiResponse<List<Employee>>> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {}
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                log.info("Successfully fetched employees");
                return response.getBody().getData();
            }

            log.warn("API returned empty response");
            return Collections.emptyList();

        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("Too many requests (429), will retry. Message: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error fetching employees: {}", ex.getMessage());
            throw new RuntimeException("Error while fetching employees");
        }
    }


    @Retry(name = "externalApiRetry")
    public Employee getEmployeeById(String id) {
        try {
            log.info("Fetching employee with id: {}", id);
            ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                    baseUrl + "/" + id,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<Employee>>() {
                    }
            );

            ApiResponse<Employee> apiResponse = response.getBody();

            if (apiResponse == null || apiResponse.getData() == null) {
                log.warn("No employee found in API response for id: {}", id);
                throw new EmployeeNotFoundException("Employee not found with id: " + id);
            }

            log.info("Successfully fetched employee: {}", apiResponse.getData());
            return apiResponse.getData();
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("Too many requests (429), will retry. Message: {}", ex.getMessage());
            throw ex;
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Employee not found (404) for id: {}", id);
            throw new EmployeeNotFoundException("Employee not found with id: " + id);
        } catch (Exception ex) {
            log.error("Error calling employee API for id {}: {}", id, ex.getMessage());
            throw new ExternalApiException("Failed to fetch employee from external API");
        }
    }

    @Retry(name = "externalApiRetry")
    public Employee createEmployee(CreateEmployeeRequest request) {
        try{
            log.info("Creating employee: {}", request.getName());
            Map<String, Object> requestBody = Map.of(
                    "name", request.getName(),
                    "salary", request.getSalary(),
                    "age", request.getAge(),
                    "title", request.getTitle()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    httpEntity,
                    new ParameterizedTypeReference<ApiResponse<Employee>>() {}
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                log.info("Successfully created employee: {}", response.getBody().getData().getEmployeeName());
                return response.getBody().getData();
            }

            throw new ExternalApiException("Failed to create employee");

        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("Too many requests (429), will retry. Message: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex){
            log.error("Error creating employee: {}", ex.getMessage());
            throw new ExternalApiException("Failed to create employee");
        }
    }

    @Retry(name = "externalApiRetry")
    public String deleteEmployee(String employeeName) {
        try {
            log.info("Deleting employee: {}", employeeName);
            Map<String, Object> requestBody = Map.of("name", employeeName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<ApiResponse<Boolean>> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.DELETE,
                    httpEntity,
                    new ParameterizedTypeReference<ApiResponse<Boolean>>() {}
            );

            if(response.getBody().getData().equals(false)){
                log.info("Employee with name {} doesn't exist", employeeName);
                throw new EmployeeNotFoundException("Employee not found for deletion");
            }
            log.info("Successfully deleted employee: {}", employeeName);
            return employeeName;
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("Too many requests (429), will retry. Message: {}", ex.getMessage());
            throw ex;
        } catch (EmployeeNotFoundException ex) {
            log.warn("Employee not found for deletion: {}", employeeName);
            throw new EmployeeNotFoundException("Employee not found: " + employeeName);
        } catch (Exception ex) {
            log.error("Error deleting employee {}: {}", employeeName, ex.getMessage());
            throw new RuntimeException("Failed to fetch employee", ex);
        }
    }
}

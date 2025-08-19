package com.reliaquest.api.service;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.ExternalApiException;
import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EmployeeApiClient employeeApiClient;

    private final String baseUrl = "http://localhost:8112/api/v1/employee";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(employeeApiClient, "baseUrl", baseUrl);
    }

    @Test
    void testGetAllEmployees_Success() {
        ApiResponse<List<Employee>> apiResponse = new ApiResponse<>(
                List.of(
                        new Employee("1", "John Doe", 50000, 30, "dev", "john.doe@test.com"),
                        new Employee("2", "Jane Smith", 60000, 32, "qa", "jane.smith@test.com")
                ),
                "success"
        );
        ResponseEntity<ApiResponse<List<Employee>>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);


        List<Employee> employees = employeeApiClient.getAllEmployees();


        assertNotNull(employees);
        assertEquals(2, employees.size());
        assertEquals("John Doe", employees.get(0).getEmployeeName());
        verify(restTemplate).exchange(eq(baseUrl), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class));
    }

    @Test
    void testGetAllEmployees_ApiReturnsEmptyData() {

        ApiResponse<List<Employee>> apiResponse = new ApiResponse<>(null, "success");
        ResponseEntity<ApiResponse<List<Employee>>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);


        List<Employee> employees = employeeApiClient.getAllEmployees();


        assertNotNull(employees);
        assertTrue(employees.isEmpty());
    }

    @Test
    void testGetAllEmployees_ApiThrowsUnexpectedException() {
        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Connection failed"));

        assertThrows(RuntimeException.class, () -> employeeApiClient.getAllEmployees());
    }

    @Test
    void testGetEmployeeById_Success() {
        String employeeId = "1";
        ApiResponse<Employee> apiResponse = new ApiResponse<>(new Employee(employeeId, "John Doe", 50000, 30, "dev", "john.doe@test.com"), "success");
        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                eq(baseUrl + "/" + employeeId),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        Employee employee = employeeApiClient.getEmployeeById(employeeId);

        assertNotNull(employee);
        assertEquals("John Doe", employee.getEmployeeName());
    }


    @Test
    void testGetEmployeeById_UnexpectedException() {
        String employeeId = "1";
        when(restTemplate.exchange(
                eq(baseUrl + "/" + employeeId),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Network error"));

        assertThrows(ExternalApiException.class, () -> employeeApiClient.getEmployeeById(employeeId));
    }

    @Test
    void testCreateEmployee_Success() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("Test User", 50000, 30,"dev");
        ApiResponse<Employee> apiResponse = new ApiResponse<>(new Employee("1", "Test User", 50000, 30, "dev", "test@test.com"), "success");
        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.CREATED);

        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        Employee createdEmployee = employeeApiClient.createEmployee(request);

        assertNotNull(createdEmployee);
        assertEquals("Test User", createdEmployee.getEmployeeName());
    }

    @Test
    void testCreateEmployee_Failure_ExternalApiException() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("Test User", 50000, 30,"dev");
        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("API is down"));

        assertThrows(ExternalApiException.class, () -> employeeApiClient.createEmployee(request));
    }

    @Test
    void testDeleteEmployee_Success() {
        String employeeName = "Test User";
        ApiResponse<Boolean> apiResponse = new ApiResponse<>(true, "success");
        ResponseEntity<ApiResponse<Boolean>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        String result = employeeApiClient.deleteEmployee(employeeName);

        assertNotNull(result);
        assertEquals(employeeName, result);
    }

    @Test
    void testDeleteEmployee_EmployeeNotFound() {
        String employeeName = "Non Existent User";
        ApiResponse<Boolean> apiResponse = new ApiResponse<>(false, "success");
        ResponseEntity<ApiResponse<Boolean>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        assertThrows(EmployeeNotFoundException.class, () -> employeeApiClient.deleteEmployee(employeeName));
    }

    @Test
    void testDeleteEmployee_UnexpectedException() {
        String employeeName = "Test User";
        when(restTemplate.exchange(
                eq(baseUrl),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Connection issue"));

        assertThrows(RuntimeException.class, () -> employeeApiClient.deleteEmployee(employeeName));
    }
}
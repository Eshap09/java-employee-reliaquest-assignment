package com.reliaquest.api.service;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeApiClient employeeApiClient;

    @Mock
    private EmployeeService self;

    @InjectMocks
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService.self = self;
    }

    private List<Employee> getSampleEmployees() {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("1", "John Doe", 50000, 32, "Software Engineer", "john.doe@example.com"));
        employees.add(new Employee("2", "Jane Smith", 75000, 28, "Product Manager", "jane.smith@example.com"));
        employees.add(new Employee("3", "Peter Jones", 90000, 45, "Senior Developer", "peter.jones@example.com"));
        employees.add(new Employee("4", "Alex White", 45000, 25, "Software Engineer", "alex.white@example.com"));
        employees.add(new Employee("5", "Mary Johnson", 120000, 40, "Senior Manager", "mary.johnson@example.com"));
        employees.add(new Employee("6", "Mike Davis", 60000, 35, "Data Analyst", "mike.davis@example.com"));
        employees.add(new Employee("7", "Emily Chen", 150000, 50, "Director", "emily.chen@example.com"));
        employees.add(new Employee("8", "Chris Brown", 85000, 38, "Team Lead", "chris.brown@example.com"));
        employees.add(new Employee("9", "Jessica Garcia", 110000, 42, "Architect", "jessica.garcia@example.com"));
        employees.add(new Employee("10", "Tom Miller", 135000, 48, "Principal Engineer", "tom.miller@example.com"));
        employees.add(new Employee("11", "Tim White", 140000, 55, "CTO", "tim.white@example.com"));
        return employees;
    }

    @Test
    void testGetAllEmployees() {
        List<Employee> mockEmployees = getSampleEmployees();
        when(employeeApiClient.getAllEmployees()).thenReturn(mockEmployees);
        List<Employee> employees = employeeService.getAllEmployees();
        assertNotNull(employees);
        assertEquals(mockEmployees.size(), employees.size());
        assertEquals("John Doe", employees.get(0).getEmployeeName());
        verify(employeeApiClient).getAllEmployees();
    }

    @Test
    void testGetEmployeesByNameSearch_withValidSearchTerm() {
        List<Employee> mockEmployees = getSampleEmployees();
        when(self.getAllEmployees()).thenReturn(mockEmployees);
        String searchTerm = "john";
        List<Employee> result = employeeService.getEmployeesByNameSearch(searchTerm);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> "John Doe".equals(e.getEmployeeName())));
        verify(self).getAllEmployees();
    }

    @Test
    void testGetEmployeesByNameSearch_withNoMatchingEmployees() {
        List<Employee> mockEmployees = getSampleEmployees();
        when(self.getAllEmployees()).thenReturn(mockEmployees);
        String searchTerm = "notfound";
        List<Employee> result = employeeService.getEmployeesByNameSearch(searchTerm);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(self).getAllEmployees();
    }

    @Test
    void testGetEmployeesByNameSearch_withEmptyAndNullSearchTerm() {
        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeesByNameSearch(null));
        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeesByNameSearch(""));
        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeesByNameSearch("   "));
    }

    @Test
    void testGetEmployeeById() {
        Employee mockEmployee = new Employee("1", "John Doe", 50000, 32, "Software Engineer", "john.doe@example.com");
        when(employeeApiClient.getEmployeeById("1")).thenReturn(mockEmployee);
        Employee employee = employeeService.getEmployeeById("1");
        assertNotNull(employee);
        assertEquals("John Doe", employee.getEmployeeName());
        verify(employeeApiClient).getEmployeeById("1");
    }

    @Test
    void testGetHighestSalaryOfEmployees() {
        List<Employee> mockEmployees = getSampleEmployees();
        when(self.getAllEmployees()).thenReturn(mockEmployees);
        Integer highestSalary = employeeService.getHighestSalaryOfEmployees();
        assertNotNull(highestSalary);
        assertEquals(150000, highestSalary);
        verify(self).getAllEmployees();
    }

    @Test
    void testGetHighestSalaryOfEmployees_withEmptyList() {
        when(self.getAllEmployees()).thenReturn(Collections.emptyList());
        Integer highestSalary = employeeService.getHighestSalaryOfEmployees();
        assertNotNull(highestSalary);
        assertEquals(0, highestSalary);
        verify(self).getAllEmployees();
    }

    @Test
    void testGetTopTenHighestSalariedEmployee() {
        List<Employee> mockEmployees = getSampleEmployees();
        when(self.getAllEmployees()).thenReturn(mockEmployees);
        List<String> topTenEmployees = employeeService.getTopTenHighestSalariedEmployee();
        assertNotNull(topTenEmployees);
        assertEquals(10, topTenEmployees.size());
        assertEquals("Emily Chen", topTenEmployees.get(0));
        assertEquals("Tim White", topTenEmployees.get(1));
        assertEquals("Tom Miller", topTenEmployees.get(2));
        assertEquals("Mary Johnson", topTenEmployees.get(3));
        verify(self).getAllEmployees();
    }

    @Test
    void testGetTopTenHighestSalariedEmployee_withFewerThanTenEmployees() {
        List<Employee> smallList = new ArrayList<>();
        smallList.add(new Employee("1", "Test 1", 10000, 30, "dev", "a@b.com"));
        smallList.add(new Employee("2", "Test 2", 20000, 30, "dev", "a@b.com"));
        when(self.getAllEmployees()).thenReturn(smallList);
        List<String> topTenEmployees = employeeService.getTopTenHighestSalariedEmployee();
        assertNotNull(topTenEmployees);
        assertEquals(2, topTenEmployees.size());
        assertEquals("Test 2", topTenEmployees.get(0));
        assertEquals("Test 1", topTenEmployees.get(1));
        verify(self).getAllEmployees();
    }

    @Test
    void testCreateEmployee() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("Test User", 50000, 30,"Tester");
        Employee mockEmployee = new Employee("12", "Test User", 50000, 30, "Tester", "test.user@example.com");
        when(employeeApiClient.createEmployee(request)).thenReturn(mockEmployee);
        Employee createdEmployee = employeeService.createEmployee(request);
        assertNotNull(createdEmployee);
        assertEquals("Test User", createdEmployee.getEmployeeName());
        verify(employeeApiClient).createEmployee(request);
    }

    @Test
    void testDeleteEmployeeById() {
        String employeeId = "1";
        String employeeName = "John Doe";
        Employee mockEmployee = new Employee(employeeId, employeeName, 50000, 32, "Software Engineer", "john.doe@example.com");
        when(self.getEmployeeById(employeeId)).thenReturn(mockEmployee);
        when(employeeApiClient.deleteEmployee(employeeName)).thenReturn("successfully! deleted records");
        String result = employeeService.deleteEmployeeById(employeeId);
        assertNotNull(result);
        assertEquals("successfully! deleted records", result);
        verify(self).getEmployeeById(employeeId);
        verify(employeeApiClient).deleteEmployee(employeeName);
    }
}
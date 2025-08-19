package com.reliaquest.api.service;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeService implements IEmployeeService {

    @Autowired
    private EmployeeApiClient employeeApiClient;

    @Autowired
    @Lazy
    EmployeeService self;


    @Cacheable("employees")
    public List<Employee> getAllEmployees() {
        log.info("Service: Getting all employees");
        return employeeApiClient.getAllEmployees();
    }

    @Override
    public List<Employee> getEmployeesByNameSearch(String searchString) {
        log.info("Service: Searching employees by name fragment: {}", searchString);

        if(searchString == null || searchString.trim().isEmpty()){
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }

        List<Employee> allEmployees = self.getAllEmployees();

        return allEmployees.stream().
                filter(employee -> {
                    return employee.getEmployeeName() != null && employee.getEmployeeName().toLowerCase().contains(searchString.toLowerCase());
                })
            .collect(Collectors.toList());
    }

    @Cacheable(value = "employee", key = "#id")
    @Override
    public Employee getEmployeeById(String id) {
        log.info("Service: Getting employee by id: {}", id);
        return employeeApiClient.getEmployeeById(id);
    }

    @Override
    public Integer getHighestSalaryOfEmployees() {
        log.info("Service: Getting highest salary");
        List<Employee> employees = self.getAllEmployees();
        return employees.stream().mapToInt(Employee::getEmployeeSalary)
                .max()
                .orElse(0);
    }

    @Override
    public List<String> getTopTenHighestSalariedEmployee() {
        log.info("Service: Getting top 10 highest earning employees");
        List<Employee> employees = self.getAllEmployees();
        return employees.stream()
                .sorted(Comparator.comparing(Employee::getEmployeeSalary).reversed())
                .limit(10)
                .map(Employee::getEmployeeName)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "employees", allEntries = true)
    @Override
    public Employee createEmployee(CreateEmployeeRequest request) {
        log.info("Service: Creating employee: {}", request.getName());
        return employeeApiClient.createEmployee(request);
    }

    @Caching(evict = {
            @CacheEvict(value = "employee", key = "#id"),
            @CacheEvict(value = "employees", allEntries = true)
    })
    @Override
    public String deleteEmployeeById(String id) {
        log.info("Service: Deleting employee by id: {}", id);
        Employee employee = self.getEmployeeById(id);
        return employeeApiClient.deleteEmployee(employee.getEmployeeName());
    }
}

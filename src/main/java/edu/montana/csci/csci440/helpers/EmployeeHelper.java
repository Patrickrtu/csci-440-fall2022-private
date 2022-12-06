package edu.montana.csci.csci440.helpers;

import edu.montana.csci.csci440.model.Employee;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EmployeeHelper {

    public static String makeEmployeeTree() {
        Employee employee = Employee.find(1); // root employee
        List<Employee> all = Employee.all();
        // use this data structure to maintain reference information needed to build the tree structure
        Map<Long, List<Employee>> employeeMap = new HashMap<>();
        // for each employee, create a map of that employee's ID -> all employees that report to that ID
        for (Employee currentEmployee : all) {
            List<Employee> peons = Employee.emptyList();
            for (Employee peon : all) {
                if (peon.getReportsTo() == currentEmployee.getEmployeeId()) {
                    peons.add(peon);
                }
                employeeMap.put(currentEmployee.getEmployeeId(), peons);
            }
        }
        return "<ul>" + makeTree(employee, employeeMap) + "</ul>";
    }

    public static String makeTree(Employee employee, Map<Long, List<Employee>> employeeMap) {
        String list = "<li><a href='/employees" + employee.getEmployeeId() + "'>"
                + employee.getEmail() + "</a><ul>";
        List<Employee> reports = employeeMap.get(employee.getEmployeeId());
        for (Employee report : reports) {
            list += makeTree(report, employeeMap);
        }
        return list + "</ul></li>";
    }
}

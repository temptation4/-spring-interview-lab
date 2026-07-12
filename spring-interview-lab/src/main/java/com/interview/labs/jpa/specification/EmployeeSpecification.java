package com.interview.labs.jpa.specification;

import com.interview.labs.jpa.entity.Employee;
import org.springframework.data.jpa.domain.Specification;

public class EmployeeSpecification {

    public static Specification<Employee>
    hasSalaryGreaterThan(Double salary){

        return (root,query,cb)->

                cb.greaterThan(
                        root.get("salary"),
                        salary);

    }

}
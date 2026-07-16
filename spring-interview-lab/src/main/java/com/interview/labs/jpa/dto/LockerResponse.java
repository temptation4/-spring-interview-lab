package com.interview.labs.jpa.dto;

public record LockerResponse(
        Long lockerId,
        String lockerNumber,
        Long employeeId,
        String employeeName
) {}

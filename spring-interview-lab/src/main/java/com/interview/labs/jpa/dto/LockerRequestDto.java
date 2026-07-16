package com.interview.labs.jpa.dto;

import lombok.Data;

@Data
public class LockerRequestDto {

    private Long lockerId;

    private String lockerNumber;

    private Long employeeId;
}
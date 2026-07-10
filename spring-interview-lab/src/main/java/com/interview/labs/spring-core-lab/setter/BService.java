package com.interview.labs.circularDependency.setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BService {

    private AService aService;

    public AService getaService() {
        return aService;
    }

    public void setaService(@Autowired AService aService) {
        this.aService = aService;
    }
}

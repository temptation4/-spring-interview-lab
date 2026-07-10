package com.interview.labs.circularDependency.setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AService {

    private BService bService;

    public BService getbService() {
        return bService;
    }

    public void setbService(@Autowired BService bService) {
        this.bService = bService;
    }
}

package io.pivotal.cfapp.service;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.pivotal.cfapp.ButlerTest;


@ButlerTest
@ExtendWith(SpringExtension.class)
public class ServiceInstanceReporterTest {

    private final ServiceInstanceReporter reporter;
    private final ObjectMapper mapper;

    @Autowired
    public ServiceInstanceReporterTest(
        ServiceInstanceReporter reporter,
        ObjectMapper mapper
    ) {
        this.reporter = reporter;
        this.mapper = mapper;
    }

    @Test
    public void testReportGeneration() throws JsonParseException, JsonMappingException, IOException {
        ReportRequestSpec spec = mapper.readValue(new File(System.getProperty("user.home") + "/service-instance-reporting-config.json"), ReportRequestSpec.class);
        reporter.createReport(spec.getOutput(), spec.getInput());
    }

}
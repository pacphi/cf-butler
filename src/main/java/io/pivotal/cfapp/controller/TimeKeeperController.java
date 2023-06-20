package io.pivotal.cfapp.controller;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.pivotal.cfapp.service.TimeKeeperService;
import io.pivotal.cfapp.service.TkServiceUtil;
import reactor.core.publisher.Mono;

@RestController
public class TimeKeeperController {

    private final TkServiceUtil util;

    @Autowired
    public TimeKeeperController(
            TimeKeeperService tkService
            ) {
        this.util = new TkServiceUtil(tkService);
    }

    @GetMapping(value = { "/collect" }, produces = MediaType.TEXT_PLAIN_VALUE )
    public Mono<ResponseEntity<String>> getCollectionTime() {
        return util.getTimeCollected()
                .map(r -> ResponseEntity.ok(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
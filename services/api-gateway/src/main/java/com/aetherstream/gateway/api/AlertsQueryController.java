package com.aetherstream.gateway.api;

import com.aetherstream.application.cqrs.QueryBus;
import com.aetherstream.application.query.GetAlertsQuery;
import com.aetherstream.domain.model.Alert;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertsQueryController {

    private final QueryBus queryBus;

    public AlertsQueryController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping
    public List<Alert> alerts(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return queryBus.dispatch(new GetAlertsQuery(limit));
    }
}

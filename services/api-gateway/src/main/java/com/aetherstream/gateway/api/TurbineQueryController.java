package com.aetherstream.gateway.api;

import com.aetherstream.application.cqrs.QueryBus;
import com.aetherstream.application.query.GetTurbineQuery;
import com.aetherstream.domain.model.Turbine;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/turbines")
public class TurbineQueryController {

    private final QueryBus queryBus;

    public TurbineQueryController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/{id}")
    public Turbine getById(@PathVariable("id") String turbineId) {
        return queryBus
                .dispatch(new GetTurbineQuery(turbineId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turbine not found"));
    }
}

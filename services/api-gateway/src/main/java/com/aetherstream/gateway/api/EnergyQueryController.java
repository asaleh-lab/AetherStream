package com.aetherstream.gateway.api;

import com.aetherstream.application.cqrs.QueryBus;
import com.aetherstream.application.query.GetLatestEnergyStatesQuery;
import com.aetherstream.domain.model.EnergyState;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/energy")
public class EnergyQueryController {

    private final QueryBus queryBus;

    public EnergyQueryController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/latest")
    public List<EnergyState> latest() {
        return queryBus.dispatch(new GetLatestEnergyStatesQuery());
    }
}

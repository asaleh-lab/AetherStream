package com.aetherstream.application.handler;

import com.aetherstream.application.cqrs.QueryHandler;
import com.aetherstream.application.port.out.TurbineQueryPort;
import com.aetherstream.application.query.GetTurbineQuery;
import com.aetherstream.domain.model.Turbine;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GetTurbineHandler implements QueryHandler<GetTurbineQuery, Optional<Turbine>> {

    private final TurbineQueryPort turbineQueryPort;

    public GetTurbineHandler(TurbineQueryPort turbineQueryPort) {
        this.turbineQueryPort = turbineQueryPort;
    }

    @Override
    public Optional<Turbine> handle(GetTurbineQuery query) {
        return turbineQueryPort.findById(query.turbineId());
    }

    @Override
    public Class<GetTurbineQuery> queryType() {
        return GetTurbineQuery.class;
    }
}

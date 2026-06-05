package com.aetherstream.application.handler;

import com.aetherstream.application.cqrs.QueryHandler;
import com.aetherstream.application.port.out.EnergyStateReadModel;
import com.aetherstream.application.query.GetLatestEnergyStatesQuery;
import com.aetherstream.domain.model.EnergyState;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetLatestEnergyStatesHandler
        implements QueryHandler<GetLatestEnergyStatesQuery, List<EnergyState>> {

    private final EnergyStateReadModel readModel;

    public GetLatestEnergyStatesHandler(EnergyStateReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<EnergyState> handle(GetLatestEnergyStatesQuery query) {
        return readModel.findLatestPerRegion();
    }

    @Override
    public Class<GetLatestEnergyStatesQuery> queryType() {
        return GetLatestEnergyStatesQuery.class;
    }
}

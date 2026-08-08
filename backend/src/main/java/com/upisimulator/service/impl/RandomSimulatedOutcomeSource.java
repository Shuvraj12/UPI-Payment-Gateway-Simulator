package com.upisimulator.service.impl;

import com.upisimulator.service.SimulatedOutcomeSource;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomSimulatedOutcomeSource implements SimulatedOutcomeSource {

    @Override
    public boolean succeeds(double successRate) {
        return ThreadLocalRandom.current().nextDouble() < successRate;
    }

}

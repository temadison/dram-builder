package com.temadison.drambuilder.service;

public interface AttributionCalculator {

    AttributionResult calculate(AttributionSnapshotInput current, AttributionSnapshotInput prior);
}

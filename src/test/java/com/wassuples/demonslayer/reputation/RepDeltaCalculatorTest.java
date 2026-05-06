package com.wassuples.demonslayer.reputation;

import com.wassuples.demonslayer.reputation.witness.WitnessType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepDeltaCalculatorTest {
    @Test
    void killRegularDemonDeltaMatchesSpec() {
        RepDelta delta = RepDeltaCalculator.forEvent(RepEventType.KILL_REGULAR_DEMON_WITNESSED);
        assertEquals(12, delta.corps());
        assertEquals(0, delta.tanjiro());
        assertEquals(-4, delta.muzan());
        assertEquals(6, delta.demon());
    }

    @Test
    void witnessFilterKeepsOnlyDshqChannel() {
        RepDelta delta = RepDeltaCalculator.forWitnessedEvent(
                RepEventType.KILL_VILLAGER_WITNESSED,
                EnumSet.of(WitnessType.CROW)
        );
        assertEquals(-60, delta.corps());
        assertEquals(0, delta.muzan());
        assertEquals(0, delta.demon());
    }

    @Test
    void playerRecordClampRespectsBounds() {
        PlayerRepRecord record = new PlayerRepRecord();
        record.setDimension(ReputationDimension.CORPS, 99999);
        record.setDimension(ReputationDimension.SUSPICION, -100);

        assertEquals(PlayerRepRecord.MAX_REP, record.getCorpsRep());
        assertEquals(PlayerRepRecord.MIN_SUSPICION, record.getSuspicion());
    }
}


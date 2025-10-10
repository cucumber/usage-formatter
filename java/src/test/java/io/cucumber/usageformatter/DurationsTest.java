package io.cucumber.usageformatter;

import io.cucumber.usageformatter.UsageReport.Statistics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static io.cucumber.usageformatter.Durations.toBigDecimalSeconds;
import static org.assertj.core.api.Assertions.assertThat;

public class DurationsTest {
    
    @Test
    void testToBigDecimalSeconds(){
        assertThat(toBigDecimalSeconds(Duration.ofMillis(0))).isEqualTo(BigDecimal.valueOf(0, 9));
        assertThat(toBigDecimalSeconds(Duration.ofMillis(100))).isEqualTo(BigDecimal.valueOf(100_000_000, 9));
        assertThat(toBigDecimalSeconds(Duration.ofMillis(1000))).isEqualTo(BigDecimal.valueOf(1_000_000_000, 9));
    }
    
    @Test
    void createStatistics_without_values(){
        Statistics statistics = Durations.createStatistics(Collections.emptyList());
        assertThat(statistics).isNull();
    }
    
    @Test
    void createStatistics_with_even_number_of_values(){
        Statistics statistics = Durations.createStatistics(Arrays.asList(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(4)
        ));
        
        assertThat(statistics).isNotNull();
        assertThat(statistics.getSum()).isEqualTo(Duration.ofSeconds(8));
        assertThat(statistics.getMean()).isEqualTo(Duration.ofSeconds(2));
        assertThat(statistics.getMoe95()).isEqualTo(Duration.parse("PT1.224744871S"));
    }
    
    @Test
    void createStatistics_with_odd_number_of_values(){
        Statistics statistics = Durations.createStatistics(Arrays.asList(
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(4)
        ));
        
        assertThat(statistics).isNotNull();
        assertThat(statistics.getSum()).isEqualTo(Duration.ofSeconds(7));
        assertThat(statistics.getMean()).isEqualTo(Duration.parse("PT2.333333333S"));
        assertThat(statistics.getMoe95()).isEqualTo(Duration.parse("PT1.440164599S"));
    }
}

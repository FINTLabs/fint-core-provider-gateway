package no.fintlabs.provider.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
open class ClockConfig {

    @Bean
    open fun systemClock(): Clock = Clock.systemUTC()

}
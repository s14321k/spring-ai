package com.smartshop.customer.springbootai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class TimeTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeTools.class);

    @Tool(name = "getCurrentLocalTime", description = "Get the current local time")
    String getCurrentLocalTime()
    {
        LocalTime currentTime = LocalTime.now();
        LOGGER.info("Returning the current time in the user's timezone {}", currentTime);
        return currentTime.toString();
    }
}

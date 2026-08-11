package com.smartshop.customer.springbootai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

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

    @Tool(name = "getCurrentTimeByZone", description = "Get the current time for a specific timezone")
    public String getCurrentTimeByZone(@ToolParam(required = true, description = "Value representing the time zone") String timeZone) {
        LOGGER.info("Returning the current time in the timezone {}", timeZone);
        return LocalTime.now(ZoneId.of(timeZone)).toString();
    }
}

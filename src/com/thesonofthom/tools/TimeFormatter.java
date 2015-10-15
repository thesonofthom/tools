package com.thesonofthom.tools;

import java.util.concurrent.TimeUnit;

/**
 * While Java has useful libraries for parsing calendar dates, it does not have good tools for parsing arbitrary timestamps.
 * Java's built in libraries always treat a time as number of ms since the epoch, not as an absolute value.
 * 
 * {@code TimeFormatter} allows for formatting durations of time, independent of the date.
 *
 * @author Kevin Thomson
 */
public class TimeFormatter
{

    private final TimeUnit lowestPossibleTimeUnit;

    private final TimeUnit highestPossibleTimeUnit;

    private final boolean forceMinTimeUnit;

    private final boolean forceMaxTimeUnit;
    
    
    /**
     * Class to print a specified time duration in human readable form.
     * This constructor gives a "best fit" string, only printing the fields
     * that are necessary to completely describe the time without any loss of precision
     * 
     */
    public TimeFormatter()
    {
        this(TimeUnit.NANOSECONDS, TimeUnit.DAYS, false, false);
    }

    /**
     * Class to print a specified time duration in human readable form.
     * Possible units range from NANOSECONDS to DAYS.
     * User can artificially limit the granularity of the string is by specifying min and max time limits in the constructor
     * 
     * @param lowestPossibleTimeUnit  minimum time unit that will be printed. If the value being printed has a lower 
     *                                granularity than this value, the value will truncated. Min possible: NANOSECONDS
     * @param highestPossibleTimeUnit maximum time unit that will be printed. Max possible: DAYS
     * @param forceMinTimeUnit        if TRUE, will always print down to the minimum time unit, even if it is 0.
     *                                if FALSE, will print only down to the last non-0 time unit
     * @param forceMaxTimeUnit        if TRUE, will print up to the maximum time unit, even it if is 0.
     *                                if FALSE, will only print up to the last non-0 time unit
     * @see TimeUnit
     */
    public TimeFormatter(TimeUnit lowestPossibleTimeUnit, TimeUnit highestPossibleTimeUnit, boolean forceMinTimeUnit, boolean forceMaxTimeUnit)
    {
        this.lowestPossibleTimeUnit = lowestPossibleTimeUnit;
        this.highestPossibleTimeUnit = highestPossibleTimeUnit;
        this.forceMinTimeUnit = forceMinTimeUnit;
        this.forceMaxTimeUnit = forceMaxTimeUnit;
        if(lowestPossibleTimeUnit.ordinal() > highestPossibleTimeUnit.ordinal())
        {
            throw new IllegalArgumentException(String.format("\n"
                    + "lowestPossibleTimeUnit : %s\n"
                    + "highestPossibleTimeUnit: %s\n"
                    + "lowestPossibleTimeUnit must be a smaller time unit than highestPossibleTimeUnit!", lowestPossibleTimeUnit, highestPossibleTimeUnit));
        }
    }
    
    /**
     * 
     * @param timeUnit
     * @return the maximum range of values that can fit within the specified time limit without 
     * spilling over into the next time unit. Also, value to scale by to convert from 
     * one time unit to the next (E.G. to convert from SECONDS to MINUTES, multiply by the SECONDS value. 
     * To convert from SECONDS to MILLISECONDS, divide by the MILLISECONDS value).
     */
    public static int timeUnitLimit(TimeUnit timeUnit)
    {
        switch(timeUnit)
        {
            case NANOSECONDS:
            case MICROSECONDS:
            case MILLISECONDS:
                return 1000;
            case SECONDS:
            case MINUTES:
                return 60;
            case HOURS:
                return 24;
            default:
                return 1;
        }
    }
    
    /**
     * 
     * @param timeUnit
     * @return a short-hand string representation of the specified time unit.  
     * (EX. NANOSECONDS returns "ns" and MINUTES returns "m")
     */
    public static String timeUnitSuffix(TimeUnit timeUnit)
    {
        switch (timeUnit)
        {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "us";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "m";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                return "";
        }
    }
    

    private static String timeFormat(TimeUnit timeUnit)
    {
        switch (timeUnit)
        {
            case NANOSECONDS:
            case MICROSECONDS:
            case MILLISECONDS:
                return "%03d";
            case SECONDS:
            case MINUTES:
            case HOURS:
                return "%02d";
            default:
                return "%d";
        }
    }
    
    /**
     * @param durationNS a time duration, in nanoseconds
     * @return a formatted string representing the specified duration (specified in nanoseconds)
     */
    public String nsToString(long durationNS)
    {
        return timeToString(durationNS, TimeUnit.NANOSECONDS);
    }
    
    /**
     * @param durationUS a time duration, in microseconds
     * @return a formatted string representing the specified duration
     */
    public String usToString(long durationUS)
    {
        return timeToString(durationUS, TimeUnit.MICROSECONDS);
    }
    
    /**
     * @param durationMS a time duration, in milliseconds
     * @return a formatted string representing the specified duration
     */
    public String msToString(long durationMS)
    {
        return timeToString(durationMS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * @param durationS a time duration, in seconds
     * @return a formatted string representing the specified duration
     */
    public String sToString(long durationS)
    {
        return timeToString(durationS, TimeUnit.SECONDS);
    }
    
    /**
     * @param duration a time duration, in the units specified in the durationUnit parameter
     * @param durationUnit
     * @return a formatted string representing the specified duration
     */
    public String timeToString(long duration, TimeUnit durationUnit)
    {
        if(duration < 0)
        {
            throw new IllegalArgumentException("Specified duration must be non-negative! Given: " + duration);
        }
        String timeString = "";
        boolean firstPrint = true;
        //start at highest timeUnit
        for(int timeUnitIndex = highestPossibleTimeUnit.ordinal(); timeUnitIndex >= lowestPossibleTimeUnit.ordinal(); timeUnitIndex--)
        {
            if(duration == 0 && !forceMinTimeUnit)
            {
                break; //done
            }
            TimeUnit timeUnit = TimeUnit.values()[timeUnitIndex];
            long currentTimeUnitDuration = convert(duration, durationUnit, timeUnit);
            if(forceMaxTimeUnit || currentTimeUnitDuration != 0 || !firstPrint)
            {
                String timeSegment = format(currentTimeUnitDuration, timeUnit, firstPrint);
                if(firstPrint)
                {
                    timeString = timeSegment;
                }
                else
                {
                    timeString = timeString + ":" + timeSegment;
                }
                firstPrint = false;
            }
            long timeAccountedFor = convert(currentTimeUnitDuration, timeUnit, durationUnit);
            duration -= timeAccountedFor; //remainder
        }
        if(firstPrint) //still on the firstPrint (meaning the actual data passed in was 0 or our granularity range is not good enough to represent this number)
        {
            timeString = format(0, lowestPossibleTimeUnit, firstPrint);
        }
        
        return timeString;
    }
    
    private String format(long duration, TimeUnit durationUnit, boolean firstPrint)
    {
        if (firstPrint)
        {
            return String.format("%d%s", duration, timeUnitSuffix(durationUnit));
        }
        else
        {
            return String.format(timeFormat(durationUnit) + "%s", duration, timeUnitSuffix(durationUnit));
        }
    }
    
    private long convert(long duration, TimeUnit durationUnit, TimeUnit targetTimeUnit)
    {
        long newDuration = duration;
        if(durationUnit.ordinal() < targetTimeUnit.ordinal())
        {
            
            for(int i = durationUnit.ordinal(); i < targetTimeUnit.ordinal(); i++)
            {
                TimeUnit t = TimeUnit.values()[i];
                newDuration = newDuration / (long)timeUnitLimit(t);
            }
        }
        else
        {
            for (int i = durationUnit.ordinal() - 1; i >= targetTimeUnit.ordinal(); i--)
            {
                TimeUnit t = TimeUnit.values()[i];
                long limit = timeUnitLimit(t);
                
                if (newDuration != 0 && limit > Long.MAX_VALUE / newDuration)
                {
                    // Overflow
                    throw new IllegalArgumentException(String.format("%d %s is too large to represent in %s", duration, durationUnit, targetTimeUnit));
                }
                newDuration *= limit;
            }
        }
        return newDuration;
    }
}

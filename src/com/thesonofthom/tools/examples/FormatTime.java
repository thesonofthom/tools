package com.thesonofthom.tools.examples;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import com.thesonofthom.tools.MethodCallGenerator;
import com.thesonofthom.tools.TimeFormatter;
import java.util.HashMap;
import java.util.Random;

/**
 * Class to run a command line utility to execute the various behaviors of the 
 * TimeFormatter class
 *
 * @author Kevin Thomson
 *
 */
public class FormatTime
{
    private PrintStream stream = null;
    private File outFile = null;
    private boolean debug;
    private boolean alignOutput;
    
    private static final int maxTimeLength;
    private static int maxTimeUnitLength;
    static
    {
        maxTimeLength = String.valueOf(Long.MAX_VALUE).length();
        maxTimeUnitLength = 0;
        for(TimeUnit t : TimeUnit.values())
        {
            if(t.name().length() > maxTimeUnitLength)
            {
                maxTimeUnitLength  = t.name().length();
            }
        }
    }

	
    public FormatTime(String fileName) throws IOException
    {
        if (fileName != null)
        {
            outFile = new File(fileName);
            stream = new PrintStream(outFile);
        }
        debug = false;
        alignOutput = false;
    }
    
    public File getOutFile()
    {
        return outFile;
    }
    
    public void enableDebug(boolean debug)
    {
        this.debug = debug;
    }
    
    public void enableOutputAlignment(boolean enable)
    {
        alignOutput = enable;
    }
    
    /**
     * Create a new TimeFormatter with the specified parameters and also log the
     * constructor called by using MethodCallGenerator
     *
     * @return new TimeFormatter
     */
    private TimeFormatter createTimeFormatter(TimeUnit lowestPossibleTimeUnit, TimeUnit highestPossibleTimeUnit, boolean forceMinTimeUnit, boolean forceMaxTimeUnit)
    {
        if(debug)
        {
            println("Using: " + MethodCallGenerator.constructorToString(TimeFormatter.class, "tf", lowestPossibleTimeUnit, highestPossibleTimeUnit, forceMinTimeUnit, forceMaxTimeUnit));
        }
        return new TimeFormatter(lowestPossibleTimeUnit, highestPossibleTimeUnit, forceMinTimeUnit, forceMaxTimeUnit);
    }

    /**
     * Print s to both System.out and the PrintStream
     *
     * @param s
     */
    private void println(Object s)
    {
        System.out.println(s);
        if (stream != null)
        {
            stream.println(s);
        }
    }

    /**
     * Print an empty newline to both System.out and the PrintStream
     */
    private void println()
    {
        System.out.println();
        if (stream != null)
        {
            stream.println();
        }
    }

    /**
     * print both the original input time and time unit as well as the output
     * from TimeFormatter
     */
    private void formattedTimeToString(TimeFormatter tf, long duration, TimeUnit timeUnit)
    {
        String time = tf.timeToString(duration, timeUnit);
        println(String.format("%s: %s", timeToString(duration, timeUnit), time));
    }
    
    private String timeToString(long duration, TimeUnit timeUnit)
    {
        String s = String.format("%d %s", duration, timeUnit);
        if(alignOutput)
        {
            return String.format("%-"+(maxTimeLength+maxTimeUnitLength+1)+"s",s);
        }
        else
        {
            return s;
        }
    }
    
    public void close()
    {
        if (stream != null)
        {
            stream.close();
        }
    }

    private static final String OPTION_INDICATOR = "-";
    //possible options. Should be all lower case
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String FORCE_MIN = "forcemin";
    private static final String FORCE_MAX = "forcemax";
    private static final String DEBUG = "debug";
    private static final String OUTPUT = "out";
   
    
    private static final String TEST_MODE = "test";
    private static final String TEST_MODE_SEED = "seed";



    private static TimeUnit parseTimeUnit(String option, String value, TimeUnit defaultValue)
    {
        if (value == null && defaultValue != null)
        {
            return defaultValue;
        }
        for(TimeUnit t : TimeUnit.values())
        {
            if(TimeFormatter.timeUnitSuffix(t).equalsIgnoreCase(value))
            {
                return t;
            }
        }
        throw new IllegalArgumentException(String.format("\"%s\" is not a valid value for %s!", value, option));
    }

    private static void printHelp()
    {
        String className = FormatTime.class.getSimpleName();
        System.out.println(className + " can be used to convert a time duration into a human readable string\n"
                + "contaning nanoseconds through days."
                +"\nTo use this tool, run the following:\n\n"
                + className + " <options> time timeUnit\n"
                + "  time: must be a non-negative number\n"
                + "  timeUnit: must be one of the following: \"ns\", \"us\", \"ms\", \"s\", \"m\", \"h\", \"d\"\n\n"
                + "Zero or more of the following options may be specified\n  "
                + OPTION_INDICATOR + MIN + ":val - specifies minimum granularity for output string\n  "
                + "    (valid values: \"ns\", \"us\", \"ms\", \"s\", \"m\", \"h\", \"d\") (default: \"ns\")\n  "
                + OPTION_INDICATOR + MAX + ":val - specifies maximum granularity for output string\n"
                + "    (valid values: \"ns\", \"us\", \"ms\", \"s\", \"m\", \"h\", \"d\") (default: \"d\")\n  "
                + OPTION_INDICATOR + FORCE_MIN +" - if specified, will always print down to the minimum time unit,\n"
                + "    even if it is 0.\n"
                + "    Otherwise, will print only down to the last non-0 time unit\n  "
                + OPTION_INDICATOR + FORCE_MAX+" - if specified, will always print up to the maxium time unit,\n"
                + "    even if it is 0.\n"
                + "    Otherwise, will print only up to the last non-0 time unit\n  "
                + OPTION_INDICATOR + DEBUG + " - will enable printing of extra debug information\n  "
                + OPTION_INDICATOR + OUTPUT + " - will write the output to the specified text file\n  "

                + "\nAdditional use:\n\n"
                + "FormatTime " + TEST_MODE + " <options>\n\n"
                + "In this mode, the script will iterate over every possible combination\n"
                + "of the fields that can be specified above, and will iterate over a number\n"
                + "of different random time values from 0 to Long.MAX_VALUE at each time unit\n"
                + "to demonstrate as many permutations as possible.\n"
                + "NOTE: If " + TEST_MODE + " is specified, the parameters listed above are ignored.\n"
                + "Zero or more of the following options may be specified when using this mode:\n  "
                + OPTION_INDICATOR + TEST_MODE_SEED + ":val - will set the seed used by the random number generator.\n"
                + "    If not specified, the generator will use a random seed.\n"
                + "    The seed will always be printed in the output\n  "
                + OPTION_INDICATOR + OUTPUT + " - will write the output to the specified text file\n"
        );
    }
    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            printHelp();
            return;
        }

        Long time = null;
        TimeUnit timeUnit = null;
        
        boolean testMode = false;
        FormatTime formatTime = null;
        HashMap<String, String> options = new HashMap<String, String>();
        try
        {
            for (String arg : args)
            {
                if (arg.startsWith(OPTION_INDICATOR))
                {
                    String option = arg.substring(OPTION_INDICATOR.length());
                    try
                    {
                        time = Long.parseLong(arg); //account for negative numbers
                    }
                    catch(NumberFormatException e)
                    {
                        //not a number, must be a regular option
                        int splitPoint = option.indexOf(":");
                        String optionName = option;
                        String optionValue = null;
                        if (splitPoint >= 0)
                        {
                            optionName = option.substring(0, splitPoint);
                            optionValue = option.substring(splitPoint + 1);
                        }
                        options.put(optionName.toLowerCase(), optionValue);
                    }
                }
                else if(arg.equalsIgnoreCase(TEST_MODE))
                {
                    testMode = true;
                }
                else if (time == null) //anything else has to be the time
                {
                    time = Long.decode(arg);
                }
                else if (timeUnit == null) //once the time is set, the next argument should be the time unit
                {
                    timeUnit = parseTimeUnit("timeUnit", arg, null);
                }
                else
                {
                    throw new IllegalArgumentException(String.format("\"%s\" is not a valid argument!", arg));
                }
            }
            formatTime = new FormatTime(options.get(OUTPUT));

            if (testMode)
            {
                formatTime.enableDebug(true);
                formatTime.enableOutputAlignment(true);
                Random r;
                long seed;
                if (options.containsKey(TEST_MODE_SEED))
                {
                    seed = Long.decode(options.get(TEST_MODE_SEED));
                    r = new Random(seed);
                }
                else
                {
                    r = new Random();
                    seed = r.nextLong();
                    r.setSeed(seed);
                }
                
                formatTime.println("-- Seed: " + seed + " --\n");
                
                //in test mode, loop through all possible combinations and then pick a random number in each each 1000 number range
                for (TimeUnit min : TimeUnit.values())
                {
                    for(int maxIndex = TimeUnit.values().length - 1; maxIndex >= min.ordinal(); maxIndex--)
                    {
                        TimeUnit max = TimeUnit.values()[maxIndex];
                        for (boolean forceMin : new boolean[]{false, true})
                        {
                            for (boolean forceMax : new boolean[]{false, true})
                            {
                                TimeFormatter tf = formatTime.createTimeFormatter(min, max, forceMin, forceMax);
                                for (TimeUnit durationUnit : TimeUnit.values())
                                {
                                    long stepMultiplier = 1;
                                    for (long value = r.nextInt(9); value < Long.MAX_VALUE; value *= stepMultiplier)
                                    {
                                        //some test cases are expected to fail because the number is too large
                                        try
                                        {
                                            formatTime.formattedTimeToString(tf, value, durationUnit);
                                        }
                                        catch (IllegalArgumentException e)
                                        {
                                            formatTime.println(String.format("%s: ERROR: %s", formatTime.timeToString(value, durationUnit), e.getMessage()));
                                        }
                                        if(value == 0)
                                        {
                                            value = 1;
                                        }
                                        stepMultiplier = (long) r.nextInt(99)+1;
                                        if (stepMultiplier > (Long.MAX_VALUE / value))
                                        {
                                            // Overflow
                                            break;
                                        }
                                    }
                                }
                                formatTime.println();
                            }
                        }
                    }
                }
                formatTime.println("-- Seed: " + seed + " --");
            }
            else
            {
                if(time == null)
                {
                    throw new IllegalArgumentException("A time duration must be specified!");
                }
                if(timeUnit == null)
                {
                    throw new IllegalArgumentException("A time unit must be specified!");
                }
                
                TimeUnit min = parseTimeUnit(MIN, options.get(MIN), TimeUnit.NANOSECONDS);
                TimeUnit max = parseTimeUnit(MAX, options.get(MAX), TimeUnit.DAYS);
                boolean forceMin = options.containsKey(FORCE_MIN);
                boolean forceMax = options.containsKey(FORCE_MAX);
                boolean debug = options.containsKey(DEBUG);

                formatTime.enableDebug(debug);
                formatTime.enableOutputAlignment(false);
                
                TimeFormatter tf = formatTime.createTimeFormatter(min, max, forceMin, forceMax);
                formatTime.println();
                formatTime.formattedTimeToString(tf, time, timeUnit);
            }
            
            if(formatTime.getOutFile() != null)
            {
                System.out.println("\n\nOutput saved to " + formatTime.getOutFile().getAbsolutePath());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
            printHelp();
        }
        finally
        {
           if(formatTime != null)
           {
               formatTime.close();
           }
        }
    }
}

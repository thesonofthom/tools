
package com.thesonofthom.tools.examples;

/**
 * Main entry into the program.
 * Can be used to execute the main methods in either
 * ParseFile or FormatTime
 * 
 * @author Kevin Thomson
 */
public class Main
{
    private static void printHelp()
    {
        System.out.println("Usage:\n  "
                + ParseFile.class.getSimpleName() + " <args>\n  "
                + FormatTime.class.getSimpleName() + " <args>\n  ");
    }

    /**
     * Execute one of the main methods available by typing in the class name and then the list of arguments used by that class
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {
        if(args.length == 0)
        {
            System.out.println();
            printHelp();
        }
        else
        {
            String[] actualArgs = new String[args.length - 1];
            for(int i = 0; i < actualArgs.length; i++)
            {
                actualArgs[i] = args[i+1];
            }
            String function = args[0];
            if(function.equals(ParseFile.class.getSimpleName()))
            {
                ParseFile.main(actualArgs);
            }
            else if(function.equals(FormatTime.class.getSimpleName()))
            {
                FormatTime.main(actualArgs);
            }
            else
            {
                printHelp();
            }
        }
    }
            
}

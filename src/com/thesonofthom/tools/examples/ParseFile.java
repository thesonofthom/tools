package com.thesonofthom.tools.examples;

import com.thesonofthom.tools.BufferMap;
import java.io.File;
import java.io.FileNotFoundException;

import com.thesonofthom.maps.BmpHeaderMap;
import com.thesonofthom.maps.PngFileMap;
import com.thesonofthom.maps.WinExeFileHeaderMap;

import java.io.IOException;
import java.util.HashMap;


/**
 * Class to execute a command line utility to parse various file types into human readable text dumps
 *
 * @author Kevin Thomson
 *
 */
public class ParseFile
{
    private static boolean printMapToConsole = false;

    public enum FileType
    {
        EXE,
        DLL,
        BMP,
        PNG;

        /**
         * 
         * @param extension extension of the file 
         * @return true if the extension equals the enum name
         */
        public boolean extensionMatches(String extension)
        {
            if(extension != null)
            {
                return extension.toUpperCase().equals(name());
            }
            return false;
        }
    }
    
    /**
     * Parse the specified file as the specified file type
     * @param file file to be parsed
     * @param type FileType to attempt to parse the file as
     * @return BufferMap containing the parsed data
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if the file cannot be parsed as that file type
     */
    public static BufferMap parseFile(File file, FileType type) throws IOException
    {
        switch(type)
        {
            case EXE:
            case DLL: //both EXEs and DLLs use the same file format
                return WinExeFileHeaderMap.buildWinExeFileHeaderMap(file);
            case BMP:
                return BmpHeaderMap.buildBmpFileHeaderMap(file);
            case PNG:
                return PngFileMap.buildPngFileMap(file);
            default:
                return null;
        }
    }

    /**
     * Attempts to parse the specified file as one of the types supported
     * @param file file to be parsed
     * @return BufferMap containing the parsed data, if successful, or null if the file could not be parsed
     */
    public static BufferMap parseFile(File file)
    {
        FileType likelyFileType = getMostLikelyFileType(file);
        //try the most likely file type first, if possible
        if(likelyFileType != null)
        {
            try
            {
                return parseFile(file, likelyFileType);
            }
            catch(Exception e)
            {
                //not of that file type
            }     
        }
        //if we can't determine the most likely file type or it failed to parse, loop over the rest
        for(FileType type : FileType.values())
        {
            if(type == likelyFileType) //already checked this
            {
                continue;
            }
            try
            {
                return parseFile(file, type);
            }
            catch(Exception e)
            {
                String exception = e.getMessage();
                //System.out.println(exception);
                //not of that file type
            }
        }
        return null; //does not match any of the files
    }
    
    /**
     * 
     * @param file
     * @return the extension of the file, if any
     */
    public static String getExtension(File file)
    {
        int index = file.getName().lastIndexOf(".");
        if(index >= 0)
        {
            return file.getName().substring(index + 1);
        }
        return null;
    }
    /**
     * 
     * @param file
     * @return the best-guess file type of the file, 
     * based strictly on the file's extension
     */
    public static FileType getMostLikelyFileType(File file)
    {
        String extension = getExtension(file);
        for (FileType type : FileType.values())
        {
            if (type.extensionMatches(extension))
            {
                return type;
            }
        }
        return null;
    }

    private static FileType checkFile(File file, FileType fileType, boolean ignoreExtension) throws IllegalArgumentException
    {
        if (ignoreExtension) //if we ignore the extension, then we will always try to parse the file
        {
            return fileType;
        }
        else
        {
            if (fileType != null)
            {
                String extension = getExtension(file);
                if (!fileType.extensionMatches(extension))
                {
                    throw new IllegalArgumentException(String.format("ERROR: User selected to only parse %s files. %s has the extension %s.\n"
                            + "To force parsing of this file as a %s, use the %s%s option",
                            fileType, file.getName(), extension, fileType, OPTION_INDICATOR, IGNORE_EXTENSION));
                }
            }
            else //no fileType specified. Parse the file if the extension is valid for any file Type
            {
                fileType = getMostLikelyFileType(file);
                if(fileType == null)
                {
                    throw new IllegalArgumentException(String.format("ERROR: File %s does not contain an extension that can be parsed by this tool!\n"
                            + "To force parsing of this file, use the %s%s option.", file.getName(),OPTION_INDICATOR, IGNORE_EXTENSION));
                }
            }
        }
        return fileType;
    }
        
    /**
     * Parse the file and dump the output to a text file in the specified output directory
     * @param file the file to parse
     * @param fileType how to parse the file. if null, will try each supported type
     * @param dumpDebug whether to enable debug information in the dump
     * @param outputDirectory directory to save the output text to. If null, will use the same directory as the file
     * @throws IOException 
     */
    public static void dumpFileInfo(File file, FileType fileType, boolean dumpDebug, String outputDirectory)  throws IOException
    {
        if(file.isDirectory())
        {
            throw new IllegalArgumentException(file.getAbsolutePath() + " is a directory!");
        }
        try
        {
            BufferMap map;
            if (fileType == null) //didn't specify what type of file we are trying to parse. try all of them
            {
                map = parseFile(file);
            }
            else
            {
                map = parseFile(file, fileType);
            }

            if (map == null)
            {
                throw new IllegalArgumentException(String.format("ERROR: File %s does not contain data that can be parsed by this tool!", file.getName()));
            }

            String outputFileNamePrefix = file.getName().replace(".", "_");

            File outputFilePath;
            if (outputDirectory == null)
            {
                outputFilePath = new File(file.getAbsoluteFile().getParent());
            }
            else
            {
                outputFilePath = new File(outputDirectory);
            }

            String outputFileName;
            if (dumpDebug)
            {
                outputFileName = outputFileNamePrefix + "_" + "map_debug.txt";
            }
            else
            {
                outputFileName = outputFileNamePrefix + "_" + "map.txt";
            }
            
            BufferMap.enableDebug(dumpDebug);
            if (printMapToConsole)
            {
                System.out.println(map);
            }
            File outputFile = new File(outputFilePath, outputFileName);
            if (!outputFilePath.exists())
            {
                outputFilePath.mkdirs();
            }
            System.out.println("Writing map to file: " + outputFile.getAbsolutePath());
            map.writeMapToFile(outputFile.getAbsolutePath());
        }
        finally
        {
            BufferMap.enableDebug(false); //always turn off debug when done
        }
    }
    
    
    private static final String OPTION_INDICATOR = "-";
    private static final String OUTPUT_DIRECTORY = "out";
    private static final String DEBUG = "debug";
    private static final String FILE_TYPE = "filetype";
    private static final String IGNORE_EXTENSION = "ignoreext";
    
    private static void printHelp()
    {
        String className = ParseFile.class.getSimpleName();
        System.out.print("\n"+className + " can be used to parse binary files into human readable text dumps"
                + "\nCurrently, the tool supports the following file formats:\n  ");
        boolean firstPrinted = false;
        for(FileType type : FileType.values())
        {
            if(firstPrinted)
            {
                System.out.print(", ");
            }
            System.out.print(type);
            firstPrinted = true;
        }
        System.out.println("\nTo use this tool, run the following:\n\n"
                + className + " filePath <options>\n"
                + "  filePath: path of the file or directory to be parsed.\n"
                + "    If it denotes a single file, only that file will be parsed.\n"
                + "    If it denotes a directory, every file in that directory will be parsed.\n\n"
                + "Zero or more of the following options may be specified:\n  "
                + OPTION_INDICATOR + OUTPUT_DIRECTORY + ":val - indicates the directory the text dump will be written to.\n"
                + "    If not specified, it will default to the directory of the input\n  "
                + OPTION_INDICATOR + DEBUG + " - enables printing of extra information about each field in the file\n  "
                + OPTION_INDICATOR + FILE_TYPE + ":val - will only parse files of the specified type.\n"
                + "    If not specified, will attempt to parse all files in the input path.\n"
                + "    Valid values: any in the list of formats supported by this tool\n  "
                + OPTION_INDICATOR + IGNORE_EXTENSION + " - will ignore file extensions when determining if a file is valid.\n"
                + "    If not specified, only files with valid file extensions will be parsed.");
    }
    
    public static void main(String[] args) throws Exception
    {
        HashMap<String, String> options = new HashMap<String, String>();
        File inputPath = null;
        if (args.length == 0)
        {
            printHelp();
            return;
        }
        try
        {
            //build parameters
            for (String arg : args)
            {
                
                if (arg.startsWith(OPTION_INDICATOR))
                {
                    String option = arg.substring(OPTION_INDICATOR.length());
                    int splitPoint = option.indexOf(":");
                    String optionName = option;
                    String optionValue = null;
                    if(splitPoint >= 0)
                    {
                        optionName = option.substring(0, splitPoint);
                        optionValue = option.substring(splitPoint+1);
                    }
                    options.put(optionName.toLowerCase(), optionValue);
                }
                else if (inputPath == null)
                {
                    inputPath = new File(arg);
                }
                else
                {
                    throw new IllegalArgumentException(String.format("\"%s\" is not a valid argument!", arg));

                }
            }
            if (inputPath == null)
            {
                throw new IllegalArgumentException("No file or path was specified!");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
            printHelp();
            return;
        }
        
        if (!inputPath.exists())
        {
            throw new FileNotFoundException(String.format("File %s does not exist!", inputPath.getAbsolutePath()));
        }
        
        String outputDirectory = options.get(OUTPUT_DIRECTORY); //if null, output to same path as file
        boolean ignoreExtension = options.containsKey(IGNORE_EXTENSION);
        FileType fileType = null;
        boolean debug = options.containsKey(DEBUG);
        if (options.containsKey(FILE_TYPE))
        {
            fileType = FileType.valueOf(options.get(FILE_TYPE).toUpperCase());
        }

        if (inputPath.isDirectory())
        {
            printMapToConsole = false;
            int numberofFilesParsed = 0;
            int numberOfFilesSkipped = 0;
            int totalNumberOfFiles = 0;
            for (File file : inputPath.listFiles())
            {
                FileType fileTypeToUse = null;
                if (!file.isDirectory()) //don't recurse into directories
                {
                    boolean checkFile = false;
                    //only print files that we're actually going to be checking
                    try
                    {
                        fileTypeToUse = checkFile(file, fileType, ignoreExtension);
                        checkFile = true;
                    }
                    catch (IllegalArgumentException e)
                    {
                        //silently fail on this file since it is not valid to check
                        numberOfFilesSkipped++;
                    }

                    if (checkFile)
                    {
                        try
                        {
                            totalNumberOfFiles++;
                            //file is valid (or we're force checking it). print that we're checking it
                            System.out.println("\n--File " + totalNumberOfFiles + ": " + file.getAbsolutePath());
                            dumpFileInfo(file, fileTypeToUse, debug, outputDirectory);
                            numberofFilesParsed++;
                        }
                        catch (IllegalArgumentException e)
                        {
                            System.out.println(e.getMessage());
                            numberOfFilesSkipped++;
                        }
                    }
                }
            }
            System.out.format("\nNumber of files parsed: %d (%d skipped)\n", numberofFilesParsed, numberOfFilesSkipped);
        }
        else
        {
            printMapToConsole = true; //since we're only parsing 1 file, print the contents to the console
            //always print
            System.out.println("--File: " + inputPath.getAbsolutePath());
            fileType = checkFile(inputPath, fileType, ignoreExtension);
            dumpFileInfo(inputPath, fileType, debug, outputDirectory);
        }
    }
}


#thesonofthom.tools

A suite of tools written in Java used for formatting a variety of different types of input data.
The following tools are available:

# TimeFormatter
Java does not have any libraries for easily presenting an arbitrary time duration as an easily interpretable string. It has libraries for parsing calendar dates, but it does not have any tools to handle arbitrary durations of time. The **TimeFormatter** class adds this functionality while allowing a wide variety of customization for exactly how to display the time. See the provided Javadoc on how to fully use the tool.
A command line utility called **FormatTime** has been provided as an example of the different format possibilities of the class.
To use this tool, run the following:

    FormatTime <options> time timeUnit
where:

 - *time*: a non-negative number  
 - *timeUnit*: the duration of *time.* Must be one of the following: "ns", "us", "ms", "s", "m", "h", "d"

Zero or more of the following options may also be specified:

 - *-min:val* - specifies minimum granularity for output string
      (valid values: "ns", "us", "ms", "s", "m", "h", "d") (default: "ns")
 - *-max:val* - specifies maximum granularity for output string
    (valid values: "ns", "us", "ms", "s", "m", "h", "d") (default: "d")
 - *-forcemin* - if specified, will always print down to the minimum time unit, even if it is 0.
    Otherwise, will print only down to the last non-0 time unit
 - *-forcemax* - if specified, will always print up to the maxium time unit, even if it is 0.
    Otherwise, will print only up to the last non-0 time unit
 - *-debug* - will enable printing of extra information, such as the TimeFormatter constructor used to generate the output string.
 - *-out:outputFilePath* - will write the output to the specified text file
  
There is also an additional use case for the FormatTime tool:

    FormatTime test <options>

In this mode, the script will iterate over every possible combination of the fields that can be specified above, and will iterate over a number of different random time values from 0 to Long.MAX_VALUE at each time unit to demonstrate as many permutations as possible.
Zero or more of the following options may be specified when using this mode:

 - -seed:val - will set the seed used by the random number generator. If not specified, the generator will use a random seed. The seed will always be printed in the output.
 -  -out - will write the output to the specified text file

The batch file **FormatTimeTest.bat** has been provided that will run the test and output the data to **formatTimeTest.txt**.

##Examples

> **input**
> FormatTime 14324 s
> 
> **output** 
> 14324 SECONDS: 3h:58m:44s

<div></div>
> **input** 
> FormatTime -min:ms -forcemin 14324 s
> 
> **output** 
> 14324 SECONDS: 3h:58m:44s:000ms:000us
<div></div>
> **input** 
> FormatTime -max:m 14324 s
> 
> **output** 
> 14324 SECONDS: 238m:44s

# BufferMap
Have you ever wanted to know what the data in a binary file actually means? Do you even have the specification for what the file contains but don't have an easy way of automatically parsing that data into an easily human readable format? The **BufferMap** class provides a powerful API for defining the format and layout of binary data. The class can then automatically build a *toString()* method that outputs a formatted, human-readable dump of a file using the structure defined by the user.
See the provided Javadoc for the full definition of the API as well as the following classes for examples on how to implement the API:

 - **BmpHeaderMap.java** - for parsing Windows Bitmap (BMP) files
 - **PngFileMap.java** - for parsing Portable Network Graphics (PNG) files
 - **WinExeFileHeaderMap.java** for parsing Windows Portable Executable (EXE) files and Windows Dynamic-link library (DLL) files

As with TimeFormatter, a command line utility called **ParseFile** has been written to demonstrate usage of the BufferMap class and the format of the data that it can output.

To use this tool, run the following:

    ParseFile filePath <options>
where:

 - *filePath*: path of the file or directory to be parsed. 
If it denotes a single file, only that file will be parsed. 
If it denotes a directory, every file in that directory will be parsed.

Zero or more of the following options may be specified:

 - *-out:val* - indicates the directory the text dump will be written to.
 If not specified, it will default to the directory of the input
 - *-debug* - enables printing of extra information about each field in the file
 - *-filetype:val* - will only parse files of the specified type.
    If not specified, will attempt to parse all files in the input path.
    Valid values: bmp, png, exe, dll
 - *-ignoreext* - will ignore file extensions when determining if a file is valid.
If not specified, only files with valid file extensions will be parsed.

##Example
**input**
>ParseFile examples\png\example.png
<div></div>

**output**
<pre>

 ----------------------------
 PNG File Format: example.png
 ----------------------------
 PNG Header: 
     First Byte      : 0x89 (137)
     Signature       : "PNG" [0x50,0x4E,0x47]
     DOS Line Ending : 0x0D0A (3338)
     DOS End Of File : 0x1A (26)
     UNIX Line Ending: 0x0A (10)
 IHDR Chunk: 
     Length            : 0x0000000D (13)
     Chunk type        : "IHDR" [0x49,0x48,0x44,0x52]
     Width             : 0x00000068 (104)
     Height            : 0x00000078 (120)
     Bit depth         : 0x08
     Color Type        : 0x06 (TrueColorWithAlpha)
     Compression Method: 0x00
     Filter Method     : 0x00
     Interlace Method  : 0x00
     CRC               : 0x1ECAB73A (516601658)
 Chunk[0]: 
     Length       : 0x00000019 (25)
     Chunk type   : "tEXt" [0x74,0x45,0x58,0x74]
     Data: 
         Keyword        : "Software"
         Text String    : "Adobe ImageReady"
     CRC          : 0x71C9653C (1909024060)
 Chunk[1]: 
     Length       : 0x0000186C (6252)
    Chunk type   : "IDAT" [0x49,0x44,0x41,0x54]
     Data: 
         Chunk Data: {6252 bytes}
     CRC          : 0xA4E01544 (2766148932)
 Chunk[2]: 
     Length       : 0x00000000
     Chunk type   : "IEND" [0x49,0x45,0x4E,0x44]
     Data: 
         Chunk Data: {0 bytes}
     CRC          : 0xAE426082 (2923585666)
</pre>

    Writing map to file: G:\GitHub\tools\examples\png\example_png_map.txt


For further examples, see the **tools\examples** folder inside this repository.
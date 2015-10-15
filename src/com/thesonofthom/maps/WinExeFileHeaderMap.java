
package com.thesonofthom.maps;

import com.thesonofthom.tools.BufferMap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Map to easily parse the header of standard Windows Executable files (.exe, .dll, etc).
 * See the following for details of the structure of the files:
 * <ul>
 * <li><a href="https://en.wikibooks.org/wiki/X86_Disassembly/Windows_Executable_Files">https://en.wikibooks.org/wiki/X86_Disassembly/Windows_Executable_Files</a></li>
  * <li><a href="https://msdn.microsoft.com/en-us/library/ms809762.aspx">https://msdn.microsoft.com/en-us/library/ms809762.aspx</a></li>
 * <li>the file WinNT.h from the Windows SDK</li>
 * </ul>
 * 
 * <pre>
 * <p>The basic structure is as follows:
 * +------------------------+
 * |       DOS Header       |
 * |------------------------|
 * |        DOS Data        |
 * |------------------------|
 * |        PE Header       |
 * | +--------------------+ |
 * | |    PE Signature    | |
 * | |--------------------| |
 * | |     COFF Header    | |
 * | |--------------------| |
 * | | PE Optional Header | |
 * | +--------------------+ |
 * +------------------------+
 * </pre>
 * 
 * @author Kevin Thomson
 */
public class WinExeFileHeaderMap extends BufferMap
{
    /**
     * Returns a {@code WinExeFileHeaderMap} object if the specified file is a valid Windows executable.
     * Only reads the data necessary to read the header, regardless of how large the file is.
     * @param file - the file to parse
     * @return {@code WinExeFileHeaderMap} if the file is valid, null otherwise
     * @throws IOException if file cannot be read
     */
    public static WinExeFileHeaderMap buildWinExeFileHeaderMap(File file) throws IOException
    {
        byte[] data = new byte[512]; // the data in the header will always be less than 1 sector, so just read 1 sector to simplify the logic
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
        stream.read(data);
        stream.close();
        return new WinExeFileHeaderMap(file.getName(), data);
    }

    /**
     * Standard Constructor
     * @param b - byte array containing the data to parse
     */
    public WinExeFileHeaderMap(byte[] b)
    {
        super("Windows EXE File Header", b, Endian.LITTLE);
    }
    
    /**
     * Constructor
     * @param fileName - the name of the file being parsed
     * @param b - byte array of the contents of the header of the file
     */
    public WinExeFileHeaderMap(String fileName, byte[] b)
    {
        super("Windows EXE File Header: " + fileName, b, Endian.LITTLE);
    }

    //when declaring fields, simply list them in the order they appear in the buffer
    public ComplexField<DOSHeaderMap> DOSHeader = new ComplexField<DOSHeaderMap>("DOS Header", DOSHeaderMap.class);
    public BufferField DOSData = new BufferField("DOS Data", DOSHeader.getValue().e_lfanew.getValueAsInt() - getCurrentOffset());
    public ComplexField<PEHeaderMap> PEHeader = new ComplexField<PEHeaderMap>("PE Header", PEHeaderMap.class);
    

    /**
     * Map to parse the "DOS Header" section of the executable file header.
     * <p>Same as the IMAGE_DOS_HEADER struct in WinNT.h 
     */
    public static class DOSHeaderMap extends BufferMap
    {
        public static final String MAGIC_NUMBER = "MZ";

        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a ComplexField of another BufferMap.
         * This is not called directly, but will instead be called inside the constructor of ComplexField via reflection
         * @param parent
         */
        public DOSHeaderMap(BufferMap parent)
        {
            super(parent);
        }
        
        /**
         * Standard Constructor. Used when parsing a buffer directly
         * @param b - byte array containing the data to parse
         */
        public DOSHeaderMap(byte[] b)
        {
            super("DOS Header", b, Endian.LITTLE);
        }

        //first 2 bytes must be "NZ" for this to be a valid header. If they aren't no need to keep parsing
        public StaticStringField e_magic = new StaticStringField("Magic number", 2, true, MAGIC_NUMBER); 
        public NumberField e_cblp = new NumberField("Bytes on last page of file", 2);
        public NumberField e_cp = new NumberField("Pages in file", 2);
        public NumberField e_crlc = new NumberField("Relocations", 2);
        public NumberField e_cparhdr = new NumberField("Size of header in paragraphs", 2);
        public NumberField e_minalloc = new NumberField("Minimum extra paragraphs needed", 2);
        public NumberField e_maxalloc = new NumberField("Maximum extra paragraphs needed", 2);
        public NumberField e_ss = new NumberField("Initial (relative) SS value", 2);
        public NumberField e_sp = new NumberField("Initial SP value", 2);
        public NumberField e_csum = new NumberField("Checksum", 2);
        public NumberField e_ip = new NumberField("Initial IP value", 2);
        public NumberField e_cs = new NumberField("Initial (relative) CS value", 2);
        public NumberField e_lfarlc = new NumberField("File address of relocation table", 2);
        public NumberField e_ovno = new NumberField("Overlay number", 2);
        public ReservedField e_res = new ReservedField(4 * 2);
        public NumberField e_oemid = new NumberField("OEM identifier", 2);
        public NumberField e_oeminfo = new NumberField("OEM information", 2);
        public ReservedField e_res2new = new ReservedField(10 * 2);
        public NumberField e_lfanew = new NumberField("File address of new exe header", 4);
    }

    /**
     * Map to parse the "PE Header" section of the executable file header.
     * <p>Same as IMAGE_NT_HEADERS32 and IMAGE_NT_HEADERS64 structs in WinNT.h 
     * (which struct is used by the file depends on whether it is a 32 or 64 bit file)
     */
    public static class PEHeaderMap extends BufferMap
    {
        public static final String SIGNATURE = "PE";

        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a ComplexField of another BufferMap.
         * This is not called directly, but will instead be called inside the constructor of ComplexField via reflection
         * @param parent
         */
        public PEHeaderMap(BufferMap parent)
        {
            super(parent);
        }

        /**
         * Standard Constructor
         * @param b - Buffer containing the data to parse
         * NOTE: Will automatically begin reading from the correct offset in the buffer based on the data in the DOS Header
         */
        public PEHeaderMap(byte[] b)
        {
            this(b, new DOSHeaderMap(b).e_lfanew.getValueAsInt()); //hard-coded to start the index at the correct byte in the file
        }

        /**
         * Standard Constructor
         * @param b byte array containing the data to parse
         * @param offset offset into the buffer to begin parsing
         */
        public PEHeaderMap(byte[] b, int offset)
        {
            super("Portable Executable (PE) Header", b, Endian.LITTLE, offset);
        }

        //the Signature must equal "PE" for this to be a valid PE Header
        public StaticStringField Signature = new StaticStringField("Signature", 4, SIGNATURE);
        public ComplexField<COFFHeaderMap> COFFHeader = new ComplexField<COFFHeaderMap>("COFF Header", COFFHeaderMap.class);
        public ComplexField<PEOptionalHeader> PEOptionalHeader = new ComplexField<PEOptionalHeader>("PE Optional Header", PEOptionalHeader.class);

        /**
         * Map to Parse the "COFF Header" section of the executable file header
         * The COFF header is present in both COFF object files (before they are linked) and in PE files where it is known as the "File header". 
         * <p>The COFF header has some information that is useful to an executable, and some information that is more useful to an object file.
         * 
         * Same as the IMAGE_FILE_HEADER struct in WinNT.h
         */
        public class COFFHeaderMap extends BufferMap
        {

            /**
             *"Sub-Class Constructor". Use when this map needs to be included in a ComplexField of another BufferMap
             * @param parent
             */
            public COFFHeaderMap(BufferMap parent)
            {
                super(parent);
            }

            public EnumNumberField<MachineId> Machine = new EnumNumberField<MachineId>("Machine", 2, MachineId.class);
            public NumberField NumberOfSections = new NumberField("Number of Sections", 2);
            public NumberField TimeDateStamp = new NumberField("Time-Date Stamp", 4);
            public NumberField PointerToSymbolTable = new NumberField("Pointer To Symbol Table", 4);
            public NumberField NumberOfSymbols = new NumberField("Number of Symbols", 4);
            public NumberField SizeOfOptionalHeader = new NumberField("Size of Optional Header", 2);
            public ComplexField<COFFHeaderCharacteristics> Characteristics
                    = new ComplexField<COFFHeaderCharacteristics>("Characteristics", COFFHeaderCharacteristics.class);
            
            /**
             * Map to define the individual bits that represents the Characteristics field in the COFF Header
             * <p>See the {@code IMAGE_FILE_} defines inside WinNT.h for the definitions
             */
            public class COFFHeaderCharacteristics extends BufferMap
            {
                /**
                 * "Sub-Class Constructor". Use when this map needs to be included in a ComplexField of another BufferMap.
                 * This is not called directly, but will instead be called inside the constructor of ComplexField via reflection
                 * @param parent
                 */
                public COFFHeaderCharacteristics(BufferMap parent)
                {
                    super(parent);
                }

                public BitField RelocsStripped = new BitField("Relocation Info Stripped");
                public BitField ExecutableImage = new BitField("Executable File");
                public BitField LineNumsStripped = new BitField("Line Numbers Stripped");
                public BitField LocalSymsStripped = new BitField("Local Symbols Stripped");
                public BitField AggressiveWsTrim = new BitField("Aggressive Working Set Trim");
                public BitField LargeAddressAware = new BitField("Large Address Aware (>2GB)");
                public BitField BytesReversedLo = new BitField("Bytes of Machine Word Reveresed (Low)");
                public ReservedBitsField Reserved = new ReservedBitsField(1);
                public BitField Machine32Bits = new BitField("32 Bit Word Machine");
                public BitField DebugStripped = new BitField("Debug Info Stripped");
                public BitField RemovableRunFromSwap = new BitField("Run From Swap File if on Removable Media");
                public BitField NetRunFromSwap = new BitField("Run From Swap File if on Net");
                public BitField System = new BitField("System File");
                public BitField DLL = new BitField("DLL File");
                public BitField UpSystemOnly = new BitField("Run on UP Machine Only");
                public BitField BytesReversedHi = new BitField("Bytes of Machine Word Reveresed (Hi)");
            }
        }

        /**
         * Map to parse the "PE Optional Header" in the header. 
         * <p>The "PE Optional Header" is not "optional" per se, because it is required in Executable files, but not in COFF object files
         * <p>Same as the IMAGE_OPTIONAL_HEADER32 and IMAGE_OPTIONAL_HEADER64 structs in WinNT.h
         */
        public class PEOptionalHeader extends BufferMap
        {
        	private static final int IMAGE_NUMBEROF_DIRECTORY_ENTRIES = 16;
        	
            /**
             * "Sub-Class Constructor". Use when this map needs to be included in a ComplexField of another BufferMap.
             * This is not called directly, but will instead be called inside the constructor of ComplexField via reflection
             * @param parent
             */
            public PEOptionalHeader(BufferMap parent)
            {
                super(parent);
            }

            public EnumNumberField<PEOptionalHeaderMagic> Magic = new EnumNumberField<PEOptionalHeaderMagic>("Magic", 2, PEOptionalHeaderMagic.class);
            public NumberField MajorLinkerVersion = new NumberField("Major Linker Version", 1);
            public NumberField MinorLinkerVersion = new NumberField("Minor Linker Version", 1);
            public NumberField SizeOfCode = new NumberField("Size of Code", 4);
            public NumberField SizeOfInitializedData = new NumberField("Size of Initialized Data", 4);
            public NumberField SizeOfUninitializedData = new NumberField("Size of Uninitialized Data", 4);
            public NumberField AddressOfEntryPoint = new NumberField("Address of Entry Point", 4);
            public NumberField BaseOfCode = new NumberField("Base of Code", 4);
            public NumberField BaseOfData = new NumberField("Base of Data", getBaseOfDataSize()); //the size of this field changes depending on whether it was compiled for 32-bit or 64-bit Windows
            public NumberField ImageBase = new NumberField("Image Base", getImageBaseSize());  //the size of this field changes depending on whether it was compiled for 32-bit or 64-bit Windows
            public NumberField SectionAlignment = new NumberField("Section Alignment", 4);
            public NumberField FileAlignment = new NumberField("File Alignment", 4);
            public NumberField MajorOSVersion = new NumberField("Major OS Version", 2);
            public NumberField MinorOSVersion = new NumberField("Minor OS Version", 2);
            public NumberField MajorImageVersion = new NumberField("Major Image Version", 2);
            public NumberField MinorImageVersion = new NumberField("Minor Image Version", 2);
            public NumberField MajorSubsystemVersion = new NumberField("Major Subsystem Version", 2);
            public NumberField MinorSubsystemVersion = new NumberField("Minor Subsystem Version", 2);
            public ReservedField Reserved = new ReservedField(4);
            public NumberField SizeOfImage = new NumberField("Size of Image", 4);
            public NumberField SizeOfHeaders = new NumberField("Size Of Headers", 4);
            public NumberField Checksum = new NumberField("Checksum", 4);
            public EnumNumberField<SubsystemId> Subsystem = new EnumNumberField<SubsystemId>("Subsystem", 2, SubsystemId.class);
            public ComplexField<DLLCharacteristics> DLLCharacteristics = 
                    new ComplexField<DLLCharacteristics>("DLL Characteristics", DLLCharacteristics.class);
            public NumberField SizeOfStackReserve = new NumberField("Size of Stack Reserve", getStackAndHeapSize()); //the size of this field changes depending on whether it was compiled for 32-bit or 64-bit Windows
            public NumberField SizeOfStackCommit = new NumberField("Size of Stack Commit", getStackAndHeapSize()); //the size of this field changes depending on whether it was compiled for 32-bit or 64-bit Windows
            public NumberField SizeOfHeapReserve = new NumberField("Size of Heap Reserve", getStackAndHeapSize()); //the size of this field changes depending on whether it was compiled for 32-bit or 64-bit Windows
            public NumberField SizeOfHeapCommit = new NumberField("Size of Heap Commit", getStackAndHeapSize()); //the size of this field changes depending on whether it was compiled for 32-bit or 64-bit Windows
            public NumberField LoaderFlags = new NumberField("Loader Flags", 4);
            public NumberField NumberOfRvaAndSizes = new NumberField("Number of Rva and Sizes", 4);
            //although technically the NumberOfRvaAndSizes field tells how many entries there are in the DataDirectory array, 
            //in reality, the size is always fixed to IMAGE_NUMBEROF_DIRECTORY_ENTRIES = 16
            public ComplexArrayField<DataDirectory> DataDirectory = 
                    new ComplexArrayField<DataDirectory>("Data Directory", DataDirectory.class, IMAGE_NUMBEROF_DIRECTORY_ENTRIES);

            /**
             *
             * @return {@code true} if the file is compiled for x64. {@code false} for x86 (32-bit)
             */
            public boolean is64Bit()
            {
                return (Magic.getValueAsEnum() == PEOptionalHeaderMagic.HDR64_MAGIC);
            }
            
            /**
             * the sizes of the BaseOfData fields differ in 64-bit files and 32-bit files.
             * It does not exist in 64-bit structures.
             * It is instead dropped to allow for the ImageBase field to expand to 64-bits 
             */
            private int getBaseOfDataSize()
            {
                return is64Bit() ? 0 : 4;
            }
            
            /**
             * the sizes of the ImageBase field differ in 64-bit files an 32-bit files
             */
            private int getImageBaseSize()
            {
                return is64Bit() ? 8 : 4;
            }

            /**
             * the sizes of the stack and heap fields differ in 64-bit files and 32-bit files
             */
            private int getStackAndHeapSize()
            {
                return is64Bit() ? 8 : 4;
            }
        }
        
        /**
         * Map to define the individual bits that represents the DLLCharacteristics field in the PE Optional Header
         * <p>See the {@code IMAGE_DLLCHARACTERISTICS_} defines inside WinNT.h for the definitions
         */
        public class DLLCharacteristics extends BufferMap
        {

            /**
             * "Sub-Class Constructor". Use when this map needs to be included in a ComplexField of another BufferMap.
             * This is not called directly, but will instead be called inside the constructor of ComplexField via reflection
             * @param parent
             */
            public DLLCharacteristics(BufferMap parent)
            {
                super(parent);
            }
            
            public ReservedBitsField Reserved = new ReservedBitsField(6);
            public BitField DynamicBase = new BitField("Dynamic Base");
            public BitField ForceIntegrity = new BitField("Force Integrity");
            public BitField NXCompat = new BitField("NX Compatible");
            public BitField NoIsolation = new BitField("Does not use isolation");
            public BitField NoSEH = new BitField("Does not use SEH");
            public BitField NoBind = new BitField("Do not bind");
            public ReservedBitsField Reserved1 = new ReservedBitsField(1);
            public BitField WDMDriver = new BitField("Driver uses WDM model");
            public ReservedBitsField Reserved2 = new ReservedBitsField(1);
            public BitField TerminalServerAware = new BitField("Terminal Server Aware");
        }
        
        /**
         * Class to parse the individual fields in the DataDirectory array inside the PE Optional Header
         * Same as the IMAGE_DATA_DIRECTORY struct in WinNT.h
         */
        public class DataDirectory extends BufferMap
        {

            /**
             * "Sub-Class Constructor". Use when this map needs to be included in a ComplexField of another BufferMap.
             * This is not called directly, but will instead be called inside the constructor of ComplexField via reflection
             * @param parent
             */
            public DataDirectory(BufferMap parent)
            {
                super(parent);
            }

            public NumberField VirtualAddress = new NumberField("Virtual Address", 4);
            public NumberField Size = new NumberField("Size", 4);
        }
    }
    
    /**
     * Enum to represent the various possible values in the Machine variable in {@code COFFHeaderMap}
     * <p>See the {@code IMAGE_FILE_MACHINE_} defines inside WinNT.h for the source list
     */
    public enum MachineId implements IntegerEnum
    {
		UNKNOWN(0), 
		INTEL_I386(0x014c), // Intel 386.
		MIPS_R3000(0x0162), // MIPS little-endian, 0x160 big-endian
		MIPS_R4000(0x0166), // MIPS little-endian
		MIPS_R10000(0x0168), // MIPS little-endian
		MIPS_WCEMIPSV2(0x0169), // MIPS little-endian WCE v2
		ALPHA_XP(0x0184), // Alpha_AXP
		SH3(0x01a2), // SH3 little-endian
		SH3DSP(0x01a3), 
		SH3E(0x01a4), // SH3E little-endian
		SH4(0x01a6), // SH4 little-endian
		SH5(0x01a8), // SH5
		ARM(0x01c0), // ARM Little-Endian
		THUMB(0x01c2), 
		AM33(0x01d3), 
		IBM_POWERPC(0x01F0), // IBM PowerPC Little-Endian
		POWERPCFP(0x01f1), 
		IA64(0x0200), // Intel 64
		MIPS16(0x0266), // MIPS
		ALPHA64(0x0284), // ALPHA64
		MIPSFPU(0x0366), // MIPS
		MIPSFPU16(0x0466), // MIPS
		INFINEON_TRICORE(0x0520), // Infineon
		EFI_BYTE_CODE(0x0EBC), // EFI Byte Code
		AMD64(0x8664), // AMD64 (K8)
		M32R(0x9041), // M32R little-endian
		;
    	
        MachineId(int id)
        {
            this.id = id;
        }
        private final int id;

        @Override
        public int getIntValue()
        {
            return id;
        }
    }
    
    
    /**
     * Enum to represent the various possible values in the Magic variable in {@code PEOptionalHeader}
     * <p>See the {@code IMAGE_NT_OPTIONAL_} defines inside WinNT.h for the full list
     *
     */
    public enum PEOptionalHeaderMagic implements IntegerEnum
    {

        HDR32_MAGIC(0x10b),
        HDR64_MAGIC(0x20b);

        PEOptionalHeaderMagic(int id)
        {
            this.id = id;
        }
        private final int id;

        @Override
        public int getIntValue()
        {
            return id;
        }
    }

    /**
     * Enum to represent the various possible values in the Subsystem variable in {@code PEOptionalHeader}
     * <p>See the {@code IMAGE_SUBSYSTEM_} defines inside WinNT.h for the full list
     */
    public enum SubsystemId implements IntegerEnum
    {
        UNKNOWN(0),
        NATIVE(1),
        WINDOWS_GUI(2),
        WINDOWS_CUI(3),
        OS2_CUI(5),
        POXIS_CUI(7),
        NATIVE_WINDOWS(8),
        WINDOWS_SE_GUI(9),
        EFI_APPLICATION(10),
        EFI_BOOT_SERVICE_DRIVER(11),
        EFI_RUNTIME_DRIVER(12),
        EFI_ROM(13),
        XBOX(14),
        WINDOWS_BOOT_APPLICATION(16),
        ;

        SubsystemId(int id)
        {
            this.id = id;
        }
        private final int id;

        @Override
        public int getIntValue()
        {
            return id;
        }
    }
}

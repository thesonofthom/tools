package com.thesonofthom.maps;

import com.thesonofthom.tools.BufferMap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Map to easily parse the PNG file format. See the following for details on the
 * structure of a PNG file:
 * <ul>
 * <li><a href="http://www.w3.org/TR/PNG/#5Chunk-layout">http://www.w3.org/TR/PNG/#5Chunk-layout</a></li>
 * <li><a href="http://www.w3.org/TR/PNG/#11Chunks">http://www.w3.org/TR/PNG/#11Chunks</a></li>
 * <li><a href="http://www.w3.org/TR/PNG-Chunks.html">http://www.w3.org/TR/PNG-Chunks.html</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Portable_Network_Graphics#Technical_details">https://en.wikipedia.org/wiki/Portable_Network_Graphics#Technical_details</a></li>
 * </ul>
 *
 * @author Kevin Thomson
 */
public class PngFileMap extends BufferMap
{

    public static final int PNG_HEADER_FIRST_BYTE = 0x89;
    public static final String PNG_SIGNATURE = "PNG";
    
    /**
     * builds a PngFileMap that parses the input file.
     * Will first only read the necessary amount of data to determine if the file is in fact a PNG
     * before reading the rest of the file
     * @param file file to parse
     * @return a PngFileMap object if the file is in fact a png
     * @throws IOException if the file could not be read
     * @throws IllegalArgumentException if the file is not a png file
     */
    public static PngFileMap buildPngFileMap(File file) throws IOException
    {
        byte[] data = new byte[PngHeader.PNG_HEADER_SIZE]; // the data in the header will always be less than 1 sector, so just read 1 sector to simplify the logic
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
        stream.read(data);
        PngHeader header = new PngHeader(data);
        //valid
        //read the whole file
        byte[] wholeFile = Files.readAllBytes(file.toPath());
        return new PngFileMap(file.getName(), wholeFile);
    }

    /**
     *
     * @param data byte array to parse
     */
    public PngFileMap(byte[] data)
    {
        super("PNG File Format", data, Endian.BIG); //all data in a PNG is Big-endian
    }

    /**
     *
     * @param filePath - the name of the file being parsed
     * @param data - byte array of the contents of the header of the file
     */
    public PngFileMap(String filePath, byte[] data)
    {
        super("PNG File Format: " + filePath, data, Endian.BIG); //all data in a PNG is Big-endian
    }

    /*
     * Fields are defined simply by declaring and initializing them in the order they appear in the buffer.
     * Standard Java conventions generally dictate to declare the field outside of the class constructor and then instantiating the object
     * inside the constructor. However, this requires 2 lines of code for every single field. 
     * Declaring the fields as shown below looks cleaner and is easier to maintain
     */
    public ComplexField<PngHeader> Header = new ComplexField<PngHeader>("PNG Header", PngHeader.class); //8 - 32
    //first chunk is guaranteed to be the IHDR chunk
    public ComplexField<IHDRChunk> IHDR = new ComplexField<IHDRChunk>("IHDR Chunk", IHDRChunk.class); //8 - 32

    /*
     * the remainder of the file is a series of Chunks
     * 
     * The ComplexArrayField class allows for a simple way to declare an array of objects without having to manually instantiate each one.
     * The class uses Java reflection to create new instances of each object in the array as needed.
     * Reflection is possible because each subclass that extends from BufferMap must implement a constructor that uses a standard format
     */
    public ComplexArrayField<Chunk> Chunks = new ComplexArrayField<Chunk>("Chunk",
            Chunk.class,
            ArraySizeType.SIZE_IN_BYTES, //we don't know how many chunks there are, so indicate this using SIZE_IN_BYTES as the size type
            getSizeOfRemainderOfBuffer()); //the size of this field is the remainder of the file
    
    /**
     *  Map to parse the header of a potential PNG file to determine if it is, in fact, a PNG file
     */
    public static class PngHeader extends BufferMap
    {
        public static final int PNG_HEADER_SIZE = 8;
        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public PngHeader(BufferMap parent)
        {
            super(parent);
        }
        
        /**
         * Will parse only the first 8 bytes of the data array to determine if the file is a valid PNG file
         * @param data 
         */
        public PngHeader(byte[] data)
        {
            super("PNG Header", data, Endian.BIG);
        }
        
        public StaticNumberField FirstByte = new StaticNumberField("First Byte", 1, PNG_HEADER_FIRST_BYTE); //0 (first byte must be 0x89 in a valid PNG file)
        public StaticStringField Signature = new StaticStringField("Signature", 3, PNG_SIGNATURE); //1 - 3 (next 3 bytes must be "PNG")
        public NumberField DOS_LineEnding = new NumberField("DOS Line Ending", 2); //4 - 5
        public NumberField DOS_EndOfFile = new NumberField("DOS End Of File", 1); //6
        public NumberField UNIX_LineEnding = new NumberField("UNIX Line Ending", 1); //7
    }

    /**
     * the IHDR chunk will always appear immediately after the standard PNG
     * header
     */
    public class IHDRChunk extends BufferMap
    {

        public static final String CHUNK_TYPE = "IHDR";

        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public IHDRChunk(BufferMap parent)
        {
            super(parent);
        }

        public NumberField Length = new NumberField("Length", 4); //length of the data (not including Chunk Type or CRC)
        public StaticStringField ChunkType = new StaticStringField("Chunk type", 4, true, CHUNK_TYPE);
        public NumberField Width = new NumberField("Width", 4);
        public NumberField Height = new NumberField("Height", 4);
        public NumberField BitDepth = new NumberField("Bit depth", 1);
        public EnumNumberField<ColorTypeEnum> ColorType = new EnumNumberField<ColorTypeEnum>("Color Type", 1, ColorTypeEnum.class);
        public NumberField CompressionMethod = new NumberField("Compression Method", 1);
        public NumberField FilterMethod = new NumberField("Filter Method", 1);
        public NumberField InterlaceMethod = new NumberField("Interlace Method", 1);
        public NumberField CRC = new NumberField("CRC", 4);

    }

    /**
     * Class for parsing all Chunk types besides IHDR
     */
    public class Chunk extends BufferMap
    {

        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public Chunk(BufferMap parent)
        {
            super(parent);
        }

        public NumberField Length = new NumberField("Length", 4); //length of the data (not including Chunk Type or CRC)
        public StringField ChunkType = new StringField("Chunk type", 4, true);
        public ComplexField<ChunkData> Data = createDataField();
        public ReservedField DataPadding = new ReservedField(Data.getValue().getRemainingLength());
        public NumberField CRC = new NumberField("CRC", 4);

        /**
         * Since each ChunkField has a unique set of data following it, we need
         * a way to parse it. Thus, we make the Data section it's own field
         * (ChunkData), and we dynamically pick the correct map to use based on
         * the data in the ChunkType field
         *
         * @return the a ComplexField object that contains the correct ChunkData
         * map
         */

        /*
        Note: Need to suppress warning here. ChunkData is the base type for all possible objects
        that will be created here. Java does not let you do:
           GenericClass<BaseClass> object = new GenericClass<ChildOfBaseClass>(),
         where class ChildOfBaseClass extends BaseClass.
        However, if you simply do GenericClass<BaseClass> object = new GenericClass(), and then
        pass in references to ChildOfBaseClass, everything works.
        */
        @SuppressWarnings( {"unchecked", "rawtypes",})
        private ComplexField<ChunkData> createDataField()
        {
            String chunkType = ChunkType.getValue();
            Class<? extends ChunkData> classType = GenericChunkData.class;
            if (chunkType.equals(BKGDChunkData.CHUNK_TYPE))
            {
                //the bKGD chunk can have 3 different formats depending on the value of the ColorType field in the IHDR chunk
                ColorTypeEnum colorType = IHDR.getValue().ColorType.getValueAsEnum();
                if (colorType != null)
                {
                    switch (colorType)
                    {
                        case Indexed_color:
                            classType = BKGDChunkDataIndexed.class;
                            break;
                        case Greyscale:
                        case GreyscaleWithAlpha:
                            classType = BKGDChunkDataGreyscale.class;
                            break;
                        case Truecolor:
                        case TrueColorWithAlpha:
                            classType = BKGDChunkDataColor.class;
                    }
                }
            }
            else if (chunkType.equals(CHRMChunkData.CHUNK_TYPE))
            {
                classType = CHRMChunkData.class;
            }
            else if (chunkType.equals(GAMAChunkData.CHUNK_TYPE))
            {
                classType = GAMAChunkData.class;
            }
            else if (chunkType.equals(ICCPChunkData.CHUNK_TYPE))
            {
                classType = ICCPChunkData.class;
            }
            else if (chunkType.equals(IDATChunkData.CHUNK_TYPE))
            {
                classType = IDATChunkData.class;
            }
            else if (chunkType.equals(ITXTChunkData.CHUNK_TYPE))
            {
                classType = ITXTChunkData.class;
            }
            else if (chunkType.equals(PHYSChunkData.CHUNK_TYPE))
            {
                classType = PHYSChunkData.class;
            }
            else if (chunkType.equals(PLTEChunkData.CHUNK_TYPE))
            {
                classType = PLTEChunkData.class;
            }
            else if (chunkType.equals(SRGBChunkData.CHUNK_TYPE))
            {
                classType = SRGBChunkData.class;
            }
            else if (chunkType.equals(SBITChunkData.CHUNK_TYPE))
            {
                ColorTypeEnum colorType = IHDR.getValue().ColorType.getValueAsEnum();
                if (colorType != null)
                {
                    switch (colorType)
                    {
                        case Greyscale:
                            classType = SBITChunkDataGrayscale.class;
                            break;
                        case Truecolor:
                        case Indexed_color:
                            classType = SBITChunkDataTruecolorIndexed.class;
                            break;
                        case GreyscaleWithAlpha:
                            classType = SBITChunkDataGreyscaleWithAlpha.class;
                            break;
                        case TrueColorWithAlpha:
                            classType = SBITChunkDataTruecolorWithAlpha.class;
                            break;
                    }
                }
            }
            else if (chunkType.equals(TEXTChunkData.CHUNK_TYPE))
            {
                classType = TEXTChunkData.class;
            }
            else if (chunkType.equals(TIMEChunkData.CHUNK_TYPE))
            {
                classType = TIMEChunkData.class;
            }
            else if (chunkType.equals(TRNSChunkData.CHUNK_TYPE))
            {
                ColorTypeEnum colorType = IHDR.getValue().ColorType.getValueAsEnum();
                if (colorType != null)
                {
                    //all others are invalid. Don't need a default since we already specified it when initializing classType
                    switch (colorType)
                    {
                        case Greyscale:
                            classType = TRNSChunkDataGreyscale.class;
                            break;
                        case Truecolor:
                            classType = TRNSChunkDataTruecolor.class;
                            break;
                        case Indexed_color:
                            classType = TRNSChunkDataIndexed.class;
                            break;
                    }
                }
            }
            else if (chunkType.equals(ZTXTChunkData.CHUNK_TYPE))
            {
                classType = ZTXTChunkData.class;
            }
            //can't use ComplexField<ChunkData> here because Java complains of "incompatible types"
            //with the classType variable. Instead, leave it unchecked. It will still work
            return new ComplexField("Data", classType, Length.getValueAsInt());
        }
    }

    /**
     * Abstract class to represent the data section of each Chunk
     */
    public abstract class ChunkData extends BufferMap
    {

        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         * @param size additionalParameter that indicates the size of the data
         * in the upcoming chunk. To access this parameter, call the
         * getTotalLength() method
         */
        public ChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        /**
         *
         * @return the total length of the ChunkData field
         */
        public int getTotalLength()
        {
            //need to call getAdditionalParameter to retrieve the "size" integer that was passed into the constructor
            //See comments inside BufferMap getAdditionalParameter method for further details as to why this is necessary
            return (Integer) getAdditionalParameter(0);
        }

        /**
         *
         * @return the amount of bytes in this ChunkData field not yet accounted
         * for by the other fields
         */
        public int getRemainingLength()
        {
            return getTotalLength() - getCurrentOffset();
        }
    }

    /**
     * Data section for "bKGD" chunks. The data is different depending on the
     * value of the ColorType field in the IHDR field.
     */
    public abstract class BKGDChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "bKGD";

        public BKGDChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }
    }

    /**
     * Data section for "bKGD" chunks when the ColorType is Indexed_color
     */
    public class BKGDChunkDataIndexed extends BKGDChunkData
    {

        public BKGDChunkDataIndexed(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField PaletteIndex = new NumberField("Palette Index", 1);
    }

    /**
     * Data section for "bKGD" chunks when the ColorType is Greyscale or
     * GreyscaleWithAlpha
     */
    public class BKGDChunkDataGreyscale extends BKGDChunkData
    {

        public BKGDChunkDataGreyscale(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField Grey = new NumberField("Greyscale", 2);
    }

    /**
     * Data section for "bKGD" chunks when the ColorType is TrueColor or
     * TrueColorWithAlpha
     */
    public class BKGDChunkDataColor extends BKGDChunkData
    {

        public BKGDChunkDataColor(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField Red = new NumberField("Red", 2);
        public NumberField Green = new NumberField("Green", 2);
        public NumberField Blue = new NumberField("Blue", 2);
    }

    /**
     * Data section for "cHRM" chunks
     */
    public class CHRMChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "cHRM";

        public CHRMChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField WhitePointX = new NumberField("White Point X", 4);
        public NumberField WhitePointY = new NumberField("White Point Y", 4);
        public NumberField RedX = new NumberField("Red X", 4);
        public NumberField RedY = new NumberField("Red Y", 4);
        public NumberField GreenX = new NumberField("Green X", 4);
        public NumberField GreenY = new NumberField("Green Y", 4);
        public NumberField BlueX = new NumberField("Blue X", 4);
        public NumberField BlueY = new NumberField("Blue Y", 4);
    }

    /**
     * Data section for "gAMA" chunks
     */
    public class GAMAChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "gAMA";

        public GAMAChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }
        public NumberField Gamma = new NumberField("Gamma", 4);
    }

    /**
     * Data section for "iCCP" chunks
     */
    public class ICCPChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "iCCP";

        public ICCPChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NullTerminatedStringField ProfileName = new NullTerminatedStringField("Profile Name");
        public NumberField CompressionMethod = new NumberField("Compression Method", 1);
        public BufferField CompressedData = new BufferField("Compressed Data", getRemainingLength());
    }

    /**
     * Data section for "IDAT" (actual data) chunks
     */
    public class IDATChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "IDAT";

        public IDATChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        //since this just contains the raw, actual data for the image, there is no need to actually show this data to the user.
        //not printing it also makes the toString() for the map a lot smaller and easier to read
        public BufferField Data = new BufferField("Chunk Data", getRemainingLength(), false);
    }

    /**
     * Data section for "iTXt" chunks
     */
    public class ITXTChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "iTXt";

        public ITXTChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        //the first field in this chunk is a string of unknown length that terminates in the NULL character '\0'
        public NullTerminatedStringField Keyword = new NullTerminatedStringField("Keyword");
        public NumberField CompressionFlag = new NumberField("Compression Flag", 1);
        public NumberField CompressionMethod = new NumberField("Compression Method", 1);
        public NullTerminatedStringField LanguageTag = new NullTerminatedStringField("Language Tag");
        public NullTerminatedStringField TranslatedKeyword = new NullTerminatedStringField("Translated Keyword");
        public AbstractField Text = buildTextField();

        private AbstractField buildTextField()
        {
            if (CompressionFlag.getValue() == 0)
            {
                return new StringField("Uncompressed Text", getRemainingLength());
            }
            else
            {
                return new BufferField("Compressed Text", getRemainingLength());
            }
        }
    }

    /**
     * Data section for "pHYs" chunks
     */
    public class PHYSChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "pHYs";

        public PHYSChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }
        public NumberField PixelsPerUnitXAxis = new NumberField("Pixels Per Unit, X Axis", 4);
        public NumberField PixelsPerUnitYAxis = new NumberField("Pixels Per Unit, Y Axis", 4);
        public EnumNumberField<UnitSpecifierEnum> UnitSpecifier = new EnumNumberField<UnitSpecifierEnum>("Unit Specifier", 1, UnitSpecifierEnum.class);
    }

    /**
     * Data section for "PLTE" chunks
     */
    public class PLTEChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "PLTE";

        public PLTEChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public ComplexArrayField<PLTEChunkEntry> PLTEEntries = new ComplexArrayField<PLTEChunkEntry>("PLTE Entries", PLTEChunkEntry.class,
                ArraySizeType.SIZE_IN_BYTES,
                getRemainingLength());

        /**
         * For each entry in the array in a "PLTE" chunk
         */
        public class PLTEChunkEntry extends BufferMap
        {

            public PLTEChunkEntry(BufferMap parent)
            {
                super(parent);
            }

            public NumberField Red = new NumberField("Red", 1);
            public NumberField Green = new NumberField("Green", 1);
            public NumberField Blue = new NumberField("Blue", 1);
        }
    }

    /**
     * Data section for "sRGB" chunks
     */
    public class SRGBChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "sRGB";

        public SRGBChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }
        public EnumNumberField<SRGBRenderingIntent> RenderingIntent
                = new EnumNumberField<SRGBRenderingIntent>("Rendering Intent", 1, SRGBRenderingIntent.class);

    }

    /**
     * Data section for "sBIT" chunks. The data is different depending on the
     * value of the ColorType field in the IHDR field.
     *
     */
    public abstract class SBITChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "sBIT";

        public SBITChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }
    }

    /**
     * Data section for "sBit" chunks when the ColorType is Greyscale
     */
    public class SBITChunkDataGrayscale extends SBITChunkData
    {

        public SBITChunkDataGrayscale(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField SignificantBits = new NumberField("Significant Greyscale Bits", 1);
    }

    /**
     * Data section for "sBit" chunks when the ColorType is TrueColor or
     * Indexed_color
     */
    public class SBITChunkDataTruecolorIndexed extends SBITChunkData
    {

        public SBITChunkDataTruecolorIndexed(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField SignificantRedBits = new NumberField("Significant Red Bits", 1);
        public NumberField SignificantGreenBits = new NumberField("Significant Green Bits", 1);
        public NumberField SignificantBlueBits = new NumberField("Significant Blue Bits", 1);
    }

    /**
     * Data section for "sBit" chunks when the ColorType is GreyscaleWithAlpha
     */
    public class SBITChunkDataGreyscaleWithAlpha extends SBITChunkDataGrayscale
    {

        public SBITChunkDataGreyscaleWithAlpha(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField SignificantAlphaBits = new NumberField("Significant Alpha Bits", 1);
    }

    /**
     * Data section for "sBit" chunks when the ColorType is TrueColorWithAlpha
     */
    public class SBITChunkDataTruecolorWithAlpha extends SBITChunkDataTruecolorIndexed
    {

        public SBITChunkDataTruecolorWithAlpha(BufferMap parent, int size)
        {
            super(parent, size);
        }

        public NumberField SignificantAlphaBits = new NumberField("Significant Alpha Bits", 1);
    }

    /**
     * Data section for "tEXt" chunks
     */
    public class TEXTChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "tEXt";

        public TEXTChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        //the first field in this chunk is a string of unknown length that terminates in the NULL character '\0'
        public NullTerminatedStringField Keyword = new NullTerminatedStringField("Keyword");
        //the subsequent field is a string that does have a known size (the remainder of the data in the chunk)
        //and does NOT have a terminating NULL character
        public StringField TextString = new StringField("Text String", getRemainingLength());
    }

    /**
     * Data section for "tIME" chunks
     */
    public class TIMEChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "tIME";

        public TIMEChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }
        public NumberField Year = new NumberField("Year", 2);
        public EnumNumberField<MonthEnum> Month = new EnumNumberField<MonthEnum>("Month", 1, MonthEnum.class);
        public NumberField Day = new NumberField("Day", 1);
        public NumberField Hour = new NumberField("Hour", 1);
        public NumberField Minute = new NumberField("Minute", 1);
        public NumberField Second = new NumberField("Second", 1);
    }

    /**
     * Data section for "tRNS" chunks. The data is different depending on the
     * value of the ColorType field in the IHDR field.
     */
    public class TRNSChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "tRNS";

        public TRNSChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }
    }

    /**
     * Data section for "tRNS" chunks when the ColorType is Greyscale
     */
    public class TRNSChunkDataGreyscale extends TRNSChunkData
    {

        public TRNSChunkDataGreyscale(BufferMap parent, int size)
        {
            super(parent, size);
        }
        public NumberField GreySampleValue = new NumberField("Grey Sample Value", 2);
    }

    /**
     * Data section for "tRNS" chunks when the ColorType is Truecolor
     */
    public class TRNSChunkDataTruecolor extends TRNSChunkData
    {

        public TRNSChunkDataTruecolor(BufferMap parent, int size)
        {
            super(parent, size);
        }
        public NumberField RedSampleValue = new NumberField("Red Sample Value", 2);
        public NumberField BlueSampleValue = new NumberField("Blue Sample Value", 2);
        public NumberField GreenSampleValue = new NumberField("Green Sample Value", 2);
    }
    
    /**
     * Data section for "tRNS" chunks when the ColorType is Indexed_color
     */
    public class TRNSChunkDataIndexed extends TRNSChunkData
    {

        public TRNSChunkDataIndexed(BufferMap parent, int size)
        {
            super(parent, size);
        }
        public BufferField AlphaForPaletteIndexes = new BufferField("Alpha for Palette Indexes", getRemainingLength(), true);
    }

    /**
     * Data section for "zTXt" chunks
     */
    public class ZTXTChunkData extends ChunkData
    {

        public static final String CHUNK_TYPE = "zTXt";

        public ZTXTChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        //the first field in this chunk is a string of unknown length that terminates in the NULL character '\0'
        public NullTerminatedStringField Keyword = new NullTerminatedStringField("Keyword");
        public NumberField CompressionMethod = new NumberField("Compression Method", 1);
        public BufferField CompressedTextDatastream = new BufferField("Compressed Text Datastream", getRemainingLength());
    }

    /*
     * TODO: Add more Chunk types. 
     * The most common types have been implemented, but there are more obscure ones in the PNG spec 
     * that I have not actually seen used in a .png file yet, for example "hIST" and "sPLT"
     */
    /**
     * for Chunks that we have not yet defined
     */
    public class GenericChunkData extends ChunkData
    {

        public GenericChunkData(BufferMap parent, int size)
        {
            super(parent, size);
        }

        //print the data in this field to the screen so the user can parse it
        public BufferField Data = new BufferField("Chunk Data", getRemainingLength(), true);
    }

    /**
     * Enum to represent the possible values of the UnitSpecifier field in the
     * "pHYs" chunk
     */
    //Don't need to use IntegerEnum here since all of the values starting from 0 that we care to parse are defined
    public enum UnitSpecifierEnum
    {

        Unit_is_unknown, //0
        Unit_is_the_meter //1
    }

    /**
     * Enum to represent the possible values of the ColorType field in the
     * "IHDR" chunk.
     */
    public enum ColorTypeEnum implements IntegerEnum
    {

        Greyscale(0),
        Truecolor(2),
        Indexed_color(3),
        GreyscaleWithAlpha(4),
        TrueColorWithAlpha(6);

        ColorTypeEnum(int value)
        {
            this.value = value;
        }

        private final int value;

        @Override
        public int getIntValue()
        {
            return value;
        }

    }

    /**
     * Enum to represent the possible values of the RenderingIntent field of the "sRGB" chunk
     */
    public enum SRGBRenderingIntent
    {
        Perceptual,
        Relative_colorimetric,
        Saturation,
        Absolute_colorimetric;
    }

    /**
     * Enum to parse the Month field in "tIME" chunk.
     * NOTE: We can't use Java's built-in Month enum because it is 0-indexed and
     * the month field in the "tIME" chunk is 1-indexed
     */
    public enum MonthEnum
    {
        INVALID_MONTH,
        January,
        February,
        March,
        April,
        May,
        June,
        July,
        August,
        September,
        October,
        November,
        December
    }
}

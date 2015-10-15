package com.thesonofthom.maps;

import java.io.File;
import java.io.IOException;

import com.thesonofthom.tools.BufferMap;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;


/**
 * Map to easily parse the BMP file format used by Windows. See the following for details on the
 * structure of a BMP file:
 * <ul>
 * <li><a href="https://en.wikipedia.org/wiki/BMP_file_format">https://en.wikipedia.org/wiki/BMP_file_format</a></li>
 * <li><a href="http://www.fileformat.info/format/bmp/egff.htm">http://www.fileformat.info/format/bmp/egff.htm</a></li>
 * <li><a href="https://msdn.microsoft.com/en-us/library/windows/desktop/dd183386(v=vs.85).aspx">https://msdn.microsoft.com/en-us/library/windows/desktop/dd183386(v=vs.85).aspx</a></li>
 * <li><a href="https://forums.adobe.com/message/3272950#3272950">https://forums.adobe.com/message/3272950#3272950</a></li>
 * <li>The file WinGDI.h</li>
 * </ul>
 *
 * @author Kevin Thomson
 */
public class BmpHeaderMap extends BufferMap
{

    public static final String SIGNATURE = "BM";
    
    /**
     * builds a BmpHeaderMap that parses the input file.
     * Will first only read the necessary amount of data to determine if the file is in fact a BMP
     * before reading the rest of the file
     * @param file file to parse
     * @return a BmpHeaderMap object if the file is in fact a BMP
     * @throws IOException if the file could not be read
     * @throws IllegalArgumentException if the file is not a BMP file
     */
    public static BmpHeaderMap buildBmpFileHeaderMap(File file) throws IOException
    {
        //read the minimum amount of data needed to validate that the file is correct
        byte[] data = new byte[BitmapFileHeader.BMP_FILE_HEADER_SIZE]; 
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
        stream.read(data);
        stream.close();
        BitmapFileHeader header = new BitmapFileHeader(data);
        //valid
        byte[] wholeFile = Files.readAllBytes(file.toPath());
        return new BmpHeaderMap(file.getName(), wholeFile);
    }

    /**
     * Constructor
     *
     * @param data byte array to parse
     */
    public BmpHeaderMap(byte[] data)
    {
        super("BMP File", data, Endian.LITTLE);
    }
    
    /**
     * Constructor
     *
     * @param filePath - the name of the file being parsed
     * @param data - byte array of the contents of the header of the file
     */
    public BmpHeaderMap(String filePath, byte[] data)
    {
        super("BMP File: " + filePath, data, Endian.LITTLE);
    }

    public ComplexField<BitmapFileHeader> FileHeader = new ComplexField<BitmapFileHeader>("Bitmap File Header", BitmapFileHeader.class);
    public ComplexField<DIBHeader> DIBHeader = buildDIBHeader();
    
    /**
     * This field may be null if it does not exist in the header
     */
    public ComplexField<BitfieldMask> OptionalBitmasks = buildOptionalBitmasks();
    /**
     * This field may be null if it does not exist in the header
     */
    public ComplexArrayField<RGBQuad> OptionalColorPalette = buildColorPaletteArray();
    
    /**
     * Map to parse the header of a potential BMP file to determine if it is, in fact, a bitmap
     */
    public static class BitmapFileHeader extends BufferMap
    {
        public static final int BMP_FILE_HEADER_SIZE = 14;
        
        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public BitmapFileHeader(BufferMap parent)
        {
            super(parent);
        }
        
        /**
         * Will parse only the first 14 bytes of the data array to determine if the file is a valid BMP file
         * @param data 
         */
        public BitmapFileHeader(byte[] data)
        {
            super("BMP File Header", data, Endian.LITTLE);
        }
        
        public StaticStringField Signature = new StaticStringField("Signature", 2, SIGNATURE); //the first 2 bytes MUST contain "BM" to be a valid BITMAP
        public NumberField FileSize = new NumberField("File Size", 4);
        public ReservedField Reserved = new ReservedField(4);
        public NumberField DataOffset = new NumberField("Data Offset", 4);
    }

    /**
     * The DIB Header can contain a number of different structures depending on the value of the first 4 bytes, which is always the size
     * @return a ComplexField object containing the correct DIBHeader map for the size field
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ComplexField<DIBHeader> buildDIBHeader()
    {
        //instantaite an instance of the base class so we can determine the size.
        //that way, we know which header is actually being used.
        DIBHeader dibHeader = new DIBHeader(this); //instantiate a DIBHeaderBase map at the current offset in the buffer
        Class<? extends DIBHeader> classType = DIBHeader.class;
        DIBHeaderSize size = dibHeader.Size.getValueAsEnum();
        if (size != null)
        {
            switch (size)
            {
                case BITMAPCOREHEADER:
                    classType = BitmapCoreHeader.class;
                    break;
                case BITMAPINFOHEADER:
                    classType = BitmapInfoHeader.class;
                    break;
                case BITMAPV3INFOHEADER:
                    classType = BitmapV3InfoHeader.class;
                    break;
                case BITMAPV4HEADER:
                    classType = BitmapV4Header.class;
                    break;
                case BITMAPV5HEADER:
                    classType = BitmapV5Header.class;
                    break;
                default: //unkown header type. Just print the data we know
                    classType = DIBHeader.class;
            }
        }

        //now actually generate a ComplexField of the correct type at the same offset the the dibHeader field was created at
        return new ComplexField("DIB Header", classType);

    }

    /**
     * The bitmasks field only occurs if the DIB Header is BITMAPINFOHEADER
     * field and the compression type is set to BI_BITFIELD
     *
     * @return a ComplexField containing the bitmask if it exists, null otherwise
     */
    private ComplexField<BitfieldMask> buildOptionalBitmasks()
    {

        if (DIBHeader.getValue().Size.getValueAsEnum() == DIBHeaderSize.BITMAPINFOHEADER)
        {
            BitmapInfoHeader bitmapInfoHeader = (BitmapInfoHeader) DIBHeader.getValue();
            if (bitmapInfoHeader.Compression.getValueAsEnum() == CompressionType.BI_BITFIELD)
            {
                return new ComplexField<BitfieldMask>("Bitfield Masks", BitfieldMask.class);
            }
        }
        return null; // no bitmap exists
    }

    /**
     * The Color Palette field only exists if the DIB Header contains a ClrUsed
     * field (BITMAPINFOHEADER through BITMAPV5HEADER) and the ClrUsed field
     * is non-0
     *
     * @return a ComplexArrayField containing the Color Palette array if it exists, null otherwise
     */
    private ComplexArrayField<RGBQuad> buildColorPaletteArray()
    {
        if (DIBHeader.getValue() instanceof BitmapInfoHeader) //if the header is at LEAST a BitmapInfoHeader
        {
            BitmapInfoHeader bitmapInfoHeader = (BitmapInfoHeader) DIBHeader.getValue();
            int NumberOfElements = bitmapInfoHeader.ClrUsed.getValueAsInt();
            return new ComplexArrayField<BmpHeaderMap.RGBQuad>("Color Palette", RGBQuad.class, NumberOfElements);
        }
        return null; //no color palette exists
    }


    /**
     * Base class for all different DIB Header types. 
     * The only common field for all of the different DIB Header formats is the Size field, 
     * which is used to determine which type of header it actually is
     */
    public class DIBHeader extends BufferMap
    {
        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public DIBHeader(BufferMap parent)
        {
            super(parent);
        }
        public EnumNumberField<DIBHeaderSize> Size = new EnumNumberField<DIBHeaderSize>("Size", 4, DIBHeaderSize.class);
    }

    /**
     * Same as the BITMAPCOREHEADER struct.
     */
    public class BitmapCoreHeader extends DIBHeader
    {

        public BitmapCoreHeader(BufferMap parent)
        {
            super(parent);
        }
        public NumberField Width = new NumberField("Width", 2);
        public NumberField Height = new NumberField("Height", 2);
        public NumberField Planes = new NumberField("Planes", 2);
        public NumberField BitCount = new NumberField("Bits-Per-Pixel", 2);
    }

    /**
     * Same as the BITMAPINFOHEADER struct.
     * <p>NOTE that it has a similar structure to BITMAPCOREHEADER but the Width and Height variables are 4 bytes instead of 2
     */
    public class BitmapInfoHeader extends DIBHeader
    {

        public BitmapInfoHeader(BufferMap parent)
        {
            super(parent);
        }
        public NumberField Width = new NumberField("Width", 4);
        public NumberField Height = new NumberField("Height", 4);
        public NumberField Planes = new NumberField("Planes", 2);
        public NumberField BitCount = new NumberField("Bits-Per-Pixel", 2);
        public EnumNumberField<CompressionType> Compression = new EnumNumberField<CompressionType>("Compression", 4, CompressionType.class);
        public NumberField SizeImage = new NumberField("Size of Image", 4);
        public NumberField XPelsPerMeter = new NumberField("Horizontal Pixels-Per-Meter", 4);
        public NumberField YPelsPerMeter = new NumberField("Vertical Pixels-Per-Meter", 4);
        public NumberField ClrUsed = new NumberField("Color Indices Used", 4);
        public NumberField ClrImportant = new NumberField("Number of Important Color Indices", 4);

    }

    /**
     * Same as the BITMAPV3INFOHEADER struct.
     * <p>NOTE: This is an extended version of the BITMAPINFOHEADER struct
     * <p>NOTE: This struct is not officially documented by Microsoft,
     * but it nonetheless will show up in certain images generated by Adobe Photoshop.
     * See <a href="https://forums.adobe.com/message/3272950#3272950">here</a> for details.
     */
    public class BitmapV3InfoHeader extends BitmapInfoHeader
    {

        public BitmapV3InfoHeader(BufferMap parent)
        {
            super(parent);
        }
        //since we are extending from BitmapInfoHeader, only need to define the fields that are new to this struct
        public NumberField RedMask = new NumberField("Red Mask", 4);
        public NumberField GreenMask = new NumberField("Green Mask", 4);
        public NumberField BlueMask = new NumberField("Blue Mask", 4);
        public NumberField AlphaMask = new NumberField("Alpha Mask", 4);
    }

    /**
     * Same as the BITMAPV4HEADER struct.
     * <p>NOTE: This is an extended version of the BITMAPV3INFOHEADER struct
     */
    public class BitmapV4Header extends BitmapV3InfoHeader
    {

        public BitmapV4Header(BufferMap parent)
        {
            super(parent);
        }

        public EnumNumberField<ColorSpaceType> CSType = new EnumNumberField<ColorSpaceType>("Color Space Type", 4, ColorSpaceType.class);
        public ComplexField<CieXYZTriple> Endpoints = new ComplexField<CieXYZTriple>("Endpoints", CieXYZTriple.class);
        public NumberField GammaRed = new NumberField("Gamma Red", 4);
        public NumberField GammaGreen = new NumberField("Gamma Green", 4);
        public NumberField GammaBlue = new NumberField("Gamma Blue", 4);


    }
    
    /**
     * Same as the BITMAPV5HEADER struct.
     * <p>NOTE: This is an extended version of the BITMAPV4HEADER struct
     */
    public class BitmapV5Header extends BitmapV4Header
    {

        public BitmapV5Header(BufferMap parent)
        {
            super(parent);
        }
        public EnumNumberField<RenderingIntent> Intent = new EnumNumberField<RenderingIntent>("Rendering Intent", 4, RenderingIntent.class);
        public NumberField ProfileData = new NumberField("Profile Data Offset", 4);
        public NumberField ProfileSize = new NumberField("Profile Data Size", 4);
        public ReservedField Reserved = new ReservedField(4);

    }

    /**
     * Same as the CIEXYZTRIPLE struct.  
     * contains the x,y, and z coordinates of the three colors 
     * that correspond to the red, green, and blue endpoints for a specified logical color space
     */
    public class CieXYZTriple extends BufferMap
    {
        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public CieXYZTriple(BufferMap parent)
        {
            super(parent);
        }
        public ComplexField<CieXYZ> CiexyzRed = new ComplexField<CieXYZ>("CIEXYZ Red", CieXYZ.class);
        public ComplexField<CieXYZ> CiexyzGreen = new ComplexField<CieXYZ>("CIEXYZ Green", CieXYZ.class);
        public ComplexField<CieXYZ> CiexyzBlue = new ComplexField<CieXYZ>("CIEXYZ Blue", CieXYZ.class);


    }
    
     /**
     * Same as the CIEXYZ struct.  
     * contains x,y, and z coordinates
     */   
    public class CieXYZ extends BufferMap
    {
        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public CieXYZ(BufferMap parent)
        {
            super(parent);
        }
        public NumberField ciexyzX = new NumberField("X", 4);
        public NumberField ciexyzY = new NumberField("Y", 4);
        public NumberField ciexyzZ = new NumberField("Z", 4);
    }

    /**
     * Structure of the Bitfield Mask that optionally following a bmp that contians a BITMAPINFOHEADER DIB Header.
     * Will only exist if the CompressionType is set to BI_BITFIELDS
     */
    public class BitfieldMask extends BufferMap
    {
        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public BitfieldMask(BufferMap parent)
        {
            super(parent);
        }
        public NumberField RedMask = new NumberField("Red Mask", 4);
        public NumberField GreenMask = new NumberField("Green Mask", 4);
        public NumberField BlueMask = new NumberField("Blue Mask", 4);
    }
    
    /**
     * Same as the RGBQUAD struct.
     * Definition for the data in a series of elements in the Color Palette array
     * that optionally follows any DIB Header that contains a ClrUsed field
     */
    public class RGBQuad extends BufferMap
    {
        /**
         * "Sub-Class Constructor". Use when this map needs to be included in a
         * ComplexField of another BufferMap. This is not called directly, but
         * will instead be called inside the constructor of ComplexField via
         * reflection
         *
         * @param parent
         */
        public RGBQuad(BufferMap parent)
        {
            super(parent);
        }

        public NumberField Blue = new NumberField("Blue", 1);
        public NumberField Green = new NumberField("Green", 1);
        public NumberField Red = new NumberField("Red", 1);
        public ReservedField Reserved = new ReservedField(1);
    }

    /**
     * The Size field in the DIB Header determines what data will follow it
     * BITMAPV3INFOHEADER - BITMAPV5INFOHEADER simply expand upon the previous header.
     * BITMAPCOREHEADER and BITMAPINFOHEADER only have the Size field in common
     */
    public enum DIBHeaderSize implements IntegerEnum
    {

        BITMAPCOREHEADER(12),
        BITMAPINFOHEADER(40),
        BITMAPV3INFOHEADER(56),
        BITMAPV4HEADER(108),
        BITMAPV5HEADER(124),
        ;

        DIBHeaderSize(int val)
        {
            value = val;
        }
        private final int value;

        @Override
        public int getIntValue()
        {
            return value;
        }

    }

    /**
     * Possible values for the Compression field in the BITMAPINFOHEADER
     */
    public enum CompressionType
    {

        BI_RGB,
        BI_RLE8,
        BI_RLE4,
        BI_BITFIELD,
        BI_JPEG,
        BI_PNG;
    }
    
    /**
     * Possible values for the ColorSpace field in the BITMAPV4HEADER
     */
    public enum ColorSpaceType
    {

        LCS_CALIBRATED_RGB,
        LCS_sRGB,
        LCS_WINDOWS_COLOR_SPACE,
        PROFILE_LINKED,
        PROFILE_EMBEDDED;
    }
    
    /**
     * Possible values for the Intent field in the BITMAPV5HEADER
     */
    public enum RenderingIntent implements IntegerEnum
    {
        
        LCS_GM_BUSINESS(1),
        LCS_GM_GRAPHICS(2),
        LCS_GM_IMAGES(4),
        LCS_GM_ABS_COLORIMETRIC(8);

        private final int value;
        RenderingIntent(int val)
        {
            this.value = val;
        }

        @Override
        public int getIntValue()
        {
            return value;
        }
    }
}

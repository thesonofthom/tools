package com.thesonofthom.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class is a powerful tool that can be used to easily parse a binary buffer of data (byte array)
 * that contains static, known data.
 * Once the structure of the buffer is defined, the class can automatically generate
 * a human-readable dump of the entire buffer without requiring the user to have to write their own toString() method,
 *  while simultaneously giving the user easy access to each field that was defined in the structure.
 * <p>
 * The purpose of this class is to eliminate the need to write separate get() and set() methods for each field that needs to be parsed,
 * as well as needing to manually iterate over each field and printing it in a human readable, aligned format when implementing the toString() method.
 * <p>
 * This class works best when the fields in the buffer being parsed are statically defined,
 * but it can also work with dynamic data with some extra effort.
 * <p>
 * It generally also works best on small files. Since an object is created for every single field in the buffer that is being parsed,
 * the memory usage of this class can grow significantly
 * <p>
 * When defining the structure of the buffer, each field is represented by an Object. These can be one of the following types:
 * <ul>
 * <li> {@code NumberField} - for basic numbers ranging from 1 to 8 bytes in size</li>
 * <li> {@code EnumNumberField} - a NumberField that can optionally also evaluate to an Enum</li>
 * <li> {@code StaticNumberField} - a NumberField that must contain the specified expected value 
 *                  or an IllegalValueInStaticFieldException is thrown when the BufferMap is constructed</li>
 * <li> {@code StringField} - for data that can be evaluated as an ASCII string. No constraints on size</li>
 * <li> {@code StaticStringField} - a StringField that must contain the specified expected value 
 *                  or an IllegalValueInStaticFieldException is thrown when the BufferMap is constructed</li>
 * <li> {@code NullTerminatedStringField} - a StringField for strings of an unknown size that terminate in the NULL character \0</li>
 * <li> {@code BitField} - for data that is 1 bit in length</li>
 * <li> {@code BitsField} - for data that is from 2 to 8 bits in length (the data MUST reside within a single byte)</li>
 * <li> {@code EnumBitsField} - like EnumNumberField, but for values less than a byte in size</li>
 * <li> {@code BufferField}- for a large amount of data that does not need to be parsed. 
 *                  User can control whether or not to print the data when toString() is called</li>
 * <li> {@code NumberArrayField} - for an array of numbers. The values in the array can each be from 1 to 8 bytes in size</li>
 * </ul>
 * Advanced Fields:
 * <ul>
 * <li> {@code ComplexField<E extends BufferMap>} - if the buffer contains a substructure that you wish to define as a single unit, 
 *                    that unit can be defined in it's own BufferMap that can be wrapped by this field
 * <li> {@code ComplexArrayField<E extends BufferMap>} - used to designate when the buffer contains an array of a complex structure.</li>
 *                    Each element int the array is described by ComplexField</li>
 * <li> {@code ReservedField} - for data that does not need to be parsed.</li>
 * <li> {@code ReservedBitsField} - for data that is not on a byte boundary that does not need to be parsed</li>
 * </ul>
 * <p>
 * For each field needing to be defined, declare an object of the relevant class with the name you wish to see printed in the toString() 
 * along with the size of the field. The offset of the field into the buffer is not specified, 
 * as it is calculated from the sizes of the previously defined fields.
 * 
 * Note that when defining the list of fields, only the size of the field is defined. 
 * The offset of the field is automatically calculated based on the order of the field in the map.
 * All bytes and bits in the map must be accounted for. Since offset is not specified, no bytes can be skipped when declaring each field.
 * For data that does not need to be parsed, use ReservedField or ReservedBitsField.
 * <p>
 * For example:
 * 
 * <pre>
 * public ExampleBufferMap extends BufferMap
 * {
 *      public ExampleBufferMap(byte[] b)
 *      {
 *          super("Example", b, Endian.LITTLE);
 *      }
 *    
 *      public NumberField FirstField = new NumberField("First Field Name", 1); //byte 0
 * 
 *      //a NumberField does not have to be aligned to the standard single byte, short (2 bytes), int (4 bytes), or long (8 bytes)
 *      public NumberField SecondField = new NumberField("Second Field Name", 3); //bytes 1-3
 * 
 *      public StringField ThirdField = new StringField("Third Field Name", 14); //bytes 4 - 17
 * 
 *      //when declaring bits field within a single byte, every single bit must be accounted for before moving on to the next byte
 *      public BitField FourthFieldBit0 = new BitField("Fourth Field, Bit 0 Name"); // byte 18, bit 0
 *      public BitsField FourthFieldBit1_5 = new BitsField("Fourth Field, Bits 1-5 Name", 5); //byte 18, bits 1-5
 *      
 *      //reserved bits fields can span across multiple bytes.
 *      public ReservedBitsField Reserved = new ReservedBitsField(10); //byte 18, bits 6-7, all of byte 19 (bits 0 - 7)
 * 
 *      //when toString() is printed, the data within this complex field will print indented from the rest of the fields
 *      public ComplexField&lt;ExampleSubMap&gt; FourthField = new ComplexField&lt;ExampleSubMap&gt;("SubSection", ExampleSubMap.class); //bytes 20 - 35 (16 bytes total)
 *      
 *      //fields with dynamic sizes can also be constructred, as shown here. In this example, let's assume FirstField.getValueAsInt() = 50
 *      public BufferField VariableLengthField = new BufferField("Variable Length Field", FirstField.getValueAsInt()); //bytes 36 - 85 (50 bytes total)
 *      public ReservedField  Reserved1 = new Reserved(34); //bytes 86 - 99
 *      
 *      //Creates an array of fields that each contain the data in ExampleSubMap2
 *      //Each element of the array will be printed as its own complex field, with the name "Complex Array[0]", "Complex Array[1]", etc.
 *      public ComplexArrayField&lt;ExampleSubMap2&gt; ArrayField = new ComplexArrayField&lt;ExampleSubMap2&gt;("Complex Array", ExampleSubMap2.class, 12); //bytes 100 - 195 (96 bytes total. 12 elements at 8 bytes each)
 * 
 *      //definitions for the "sub" fields that are used by ComplexField and ComplexArrayField
 *      public class ExampleSubMap extends BufferMap
 *      {
 *          public ExampleSubMap(BufferMap parent)
 *          {
 *              super(parent);
 *          }
 *          
 *          public NumberField SubMapFirstField = new NumberField("Sub Map First Field", 2); //bytes 0 - 1 of this section
 * 
 *          //for this field, if the data at that section evaluates to either 0, 1, or 2, the corresponding enum name will also be printed
 *          public EnumNumberField&lt;EnumExample&gt; SubMapSecondField = new EnumNumberField&lt;EnumExample&gt;("Sub Map Second Field", 8, EnumExample.class); //bytes 2 - 9 of this section
 *          
 *          public StringField SubMapStringField = new StringField("Sub Map String Field", 6); //bytes 10 - 15 of this section
 *      }
 * 
 *      public class ExampleSubMap2 extends BufferMap
 *      {
 *          public ExampleSubMap2(BufferMap parent)
 *          {
 *              super(parent);
 *          }
 * 
 *          public String Field DataString = new StringField("Data String", 4, true); // bytes 0 - 3 of this section (the second parameter indicates to print the raw hex data for the string as well as the ASCII)
 *          public NumberField Data = new NumberField("Data", 4, Signedness.SIGNED); // bytes 4 - 7 (data is treated as a signed int instead of unsigned; i.e. 0xFFFFFFFF = -1 and not 4294967295)
 *      }
 * 
 *      public enum EnumExample
 *      {
 *           NAME_IF_FIELD_EVALUATES_TO_0,
 *           NAME_IF_FIELD_EVALUATES_TO_1,
 *           NAME_IF_FIELD_EVALUATES_TO_2
 *      }
 * }
 * 
 * </pre>
 * 
 * @author Kevin Thomson
 */
public class BufferMap extends Buffer
{

    private static enum LengthUnits
    {
        BYTES,
        BITS
    }
    
    private static boolean debug = false;
    private static boolean printReservedFields = false;
    
    public static String NEWLINE = System.lineSeparator();//to account for Windows' weird line endings

    private static String INDENT = "    ";
    
    public static final String RESERVED = "Reserved";
    public static final String OBSOLETE = "Obsolete";
    
    /**
     * if the map has a parent (i.e. is part of a ComplexField inside a bigger map), this will be set to the parent
     */
    private BufferMap parent;
    
    /**
     * if, when using the subclass Constructor for BufferMap, more parameters are needed, they will be placed in this array.
     */
    private Object[]  additionalParameters;
    
  /**
   * the current offset for the next field (also, the current total size of the map)
   */
    private int currentOffset; 
    
    /**
     * the array that holds each field in the map. Each field is added to this list in the constructor of the field object
     */
    protected ArrayList<AbstractField> fields = new ArrayList<AbstractField>();
    
    /**
     * the name of the map
     */
    protected String mapName;

    /**
     * Basic constructor for {@code BufferMap}.
     * 
     * @param name - the name of the map. Will be displayed at the top of {@code toString()}
     * @param data - the data to be parsed
     * @param endian - the endianness (BIG or LITTLE) of the data in the byte array
     * @param startOffset - offset into the buffer to begin parsing
     */
    public BufferMap(String name, byte[] data, Endian endian, int startOffset)
    {
        super(data, endian, startOffset);
        if (name == null)
        {
            name = "";
        }
        this.mapName = name;
        maxFieldNameLength = 0;
    }
    
    /**
     * Basic constructor for {@code BufferMap}.
     * 
     * @param name - the name of the map. Will be displayed at the top of {@code toString()}
     * @param data - the data to be parsed
     * @param endian - the endianness (BIG or LITTLE) of the data in the byte array
     */
    public BufferMap(String name, byte[] data, Endian endian)
    {
    	this(name, data, endian, 0);
    }
    

    /**
     * "SubClass Constructor"<p>
     * Constructor to use when to have an BufferMap that contains data that is a subset of a larger map.
     * 
     * To access any of the additionalParameters specified in the constructor, call
     * <pre>
     * Object getAdditionalParameter(int i)
     * </pre>
     * 
     * <b>IMPORTANT NOTE: All subclasses that use this constructor must keep it in this exact format:
     * <p>public E(BufferMap parent, Object... additionalParams) 
     * </b>
     * 
     * @param parent
     * @param additionalParameters
     */
    public BufferMap(BufferMap parent, Object... additionalParameters)
    {
        this(parent.getMapName(), parent.getData(), parent.getEndian(), parent.getOffset() + parent.getCurrentOffset());
        this.parent = parent;
        this.additionalParameters = additionalParameters;
    }
    
    /**
     * 
     * @param i the additional parameter to be returned
     * @return the specified additional parameter that was passed into to the
     * SubClass Constructor. No bounds checking is done.
     * It is necessary to go through this mechanism when passing in an additional parameter to the SubClass Constructor
     * instead of simply creating a local variable inside the sub class and instantiating it inside that class constructor
     * after the call to super(). This is because that local variable does not get instantiated until AFTER all of the AbstractField classes
     * inside that class are constructed, and thus it would not be able to be used
     * @throws ArrayIndexOutOfBoundsException if no additional parameters were
     * specified in the constructor or an invalid index is specified
     */
    public Object getAdditionalParameter(int i)
    {
        return additionalParameters[i];
    }
    
    /**
     * @return the parent of this map, if applicable. Base maps have no parent
     */
    public BufferMap getParent()
    {
        return parent;
    }
    
    private int maxFieldNameLength; //used for formatting purposes to keep track of the length of the longest field
    private int currentBitOffset = 0; //same as currentByteOffset, but tracks the position of the current bit within the byte
    
    private boolean arrayLock; //used to specify to not add each individual field of an ArrayField to the master list. The array handles printing each of its elements on its own

    /**
     * Called within the constructor of each AbstractField to insert the field into the fields array 
     * and update the variables to keep track of our current position within the buffer
     */
    private void add(AbstractField field)
    {
        currentOffset = field.getStartOffsetOfNextField();

        if (field.sizeInBits != 0) //BitsField
        {
            currentBitOffset += field.sizeInBits;
            if (currentBitOffset == 8) //we've reached the end of the byte, so the next bit offset will start at 0 again
            {
                currentBitOffset = 0;
            }

            if (currentBitOffset > 7)
            {
                throw new IllegalValueInBufferFieldException("Field %s (byte offset %d, start bit offset %d)\n"
                        + " specified a size in bits (%d) that will cause it to go beyond the byte boundary. BitsField can only exist within a single byte",
                        field.name, field.offset, field.bitOffset, field.sizeInBits);
            }
        }
        else if (getCurrentBitOffset() != 0)
        {
            throw new IllegalValueInBufferFieldException(" Field %s (offset %d, size %d) was specifed to start not on a byte boundary!\n"
                    + "The current bit offset (based on the previously defined bit-wise fields), is %d", field.name, field.offset, field.sizeInBytes, getCurrentBitOffset());
        }


        if (field.getName().length() > maxFieldNameLength)
        {
            maxFieldNameLength = field.getName().length();
        }

        if (!arrayLock)
        {
            fields.add(field);
        }
    }
    

    /**
     * @return the byte offset that we are currently pointing at
     */
    protected int getCurrentOffset()
    {
        return currentOffset;
    }

    /**
     * @return the current bit offset. Will be 0 unless BitsFields are being processed
     */
    protected int getCurrentBitOffset()
    {
        return currentBitOffset;
    }

    /**
     * @return total size in bytes being parsed by the map. Can be less than the size of the buffer itself
     */
    public int getCurrentSizeOfMap()
    {
        return currentOffset; //the current offset will also point to the overall size in bytes of the structure
    }

    /**
     * @return the name of the map that was specified in the constructor
     */
    public String getMapName()
    {
        return mapName;
    }
    
    /**
     * @return the amount of bytes that are remaining in the buffer, based on the current fields being parsed
     */
    public int getSizeOfRemainderOfBuffer()
    {
    	return getSize() - getCurrentOffset();
    }

   
    private static String generateSpacer(int size)
    {
        StringBuilder spacer = new StringBuilder();
        for (int i = 0; i < size; i++)
        {
            spacer.append("-");
        }
        spacer.append(NEWLINE);
        return (spacer.toString());
    }

    @Override
    public String toString()
    {
        StringBuilder message = new StringBuilder();
        if (parent == null)
        {
            
            String name = mapName;
            if(debug)
            {
            	name = String.format("%s (offset: %d, size %d bytes)", mapName, getOffset(), getCurrentSizeOfMap());
            }
            message.append(generateSpacer(name.length()));
            message.append(name).append(NEWLINE);
            message.append(generateSpacer(name.length()));
        }

        boolean warningMessagePrinted = false;

        boolean firstLine = true;
        for (AbstractField field : fields)
        {
            if ((!field.isReservedField && (!field.getValueAsString().isEmpty() || debug)) || (field.isReservedField && (printReservedFields || debug)))
            {
                if (getOffset() + field.getStartOffsetOfNextField() <= getData().length)
                {
                    if (!firstLine)
                    {
                        message.append(NEWLINE);
                    }
                    firstLine = false;
                    message.append(field.toString());
                }
                else
                {
                    if (!warningMessagePrinted && parent == null)
                    {
                        message.append(String.format(NEWLINE+NEWLINE+"WARNING: The size of the buffer is %d bytes. " +
                                "The remaining fields are outside the bounds of the buffer!"+NEWLINE, getSize()));
                        warningMessagePrinted = true;
                    }
                    message.append(String.format(NEWLINE+"%s (%s)", field.getName(), field.getFieldSpecificInfo()));
                }
            }
        }
        return message.toString();
    }
    
    /**
     * Writes the toString() of this BufferMap to the specified file path
     * @param filePath - file to write to
     * @throws IOException
     */
    public void writeMapToFile(String filePath) throws IOException
    {
    	PrintWriter out = new PrintWriter(filePath);
        try
        {
	        out.println(toString());
        }
        finally
        {
        	out.close();
        }
    }

    /**
     * Will enable debugging, which will print useful information about each field, including it's offset into the buffer and size.
     * <p>This is useful if you want to make sure you didn't make a mistake creating a map and marked a field with the wrong size,
     *  as this will throw off the offsets for every field following it
     *  <p>
     *  Disabled by default
     * @param enable
     */
    public static void enableDebug(boolean enable)
    {
        debug = enable;
    }

    /**
     * will enable printing of all reserved fields in the buffer map
     * off by default. enabling debug will also enable this feature
     * 
     * @param enable
     */
    public static void enableReservedFieldPrinting(boolean enable)
    {
        printReservedFields = enable;
    }

    /**
     * This is the base class for all Fields.
     */
    public abstract class AbstractField
    {

        /**
         * byte offset of the field
         */
        protected int offset;

        /**
         * the bit offset, will be 0 unless a bit(s) field
         */
        protected int bitOffset;

        /**
         * Size of field in bytes. Will be 0 if the field is less than 1 byte  (BitsField or BitField)
         */
        protected int sizeInBytes;

        /**
         * Size of field in bits. Mutually exclusive with sizeInBytes.
         * Will be 0 unless the field is a BitsField or BitField
         */
        protected int sizeInBits;
        
        private String name;

        /**
         * Whether the field is a BIT field or a BYTE field
         */
        protected LengthUnits units;

        /**
         * Whether or not the field is marked as Reserved. Reserved fields do not print in toString().
         * Used in ReservedField and ReservedBitsField
         */
        protected boolean isReservedField = false;

        /**
         * Constructor for when the size of the field is as of yet unknown
         * @param name - name of field
         */
        public AbstractField(String name)
        {
            this(name, LengthUnits.BYTES);
        }

        /**
         * constructor
         * @param name - name of field
         * @param size - size of field in bytes
         */
        public AbstractField(String name, int size)
        {
            this(name, LengthUnits.BYTES);
            bind(size);
        }

        /**
         * constructor
         * @param name - name of field
         * @param size - size of field
         * @param units  - determine whether the size variable represents BITS or BYTES
         */
        public AbstractField(String name, int size, LengthUnits units)
        {
            this(name, units);
            bind(size);
        }
        
        private AbstractField(String name, LengthUnits units)
        {
            this.name = name.trim();
            this.units = units;
        }

        /**
         * bind the field to the internal array.
         * Called either in the base constructor or in the subclass constructor once the size of the field is determined
         * @param size - size of the field in either bytes or bits, depending on units setting
         */
        protected final void bind(int size)
        {
            //automatically calculate offset of field
            offset = getCurrentOffset();
            bitOffset = getCurrentBitOffset(); //will be 0 unless it is a BitsField

            if (units == LengthUnits.BYTES)
            {
                sizeInBytes = size;
                sizeInBits = 0;
            }
            else
            {
                sizeInBytes = 0;
                sizeInBits = size;
            }
            add(this); //automatically attach itself to the internal structure of this BufferMap
        }

        /**
         * @return the value of the field as a formatted string. Used in {@code toString()}
         */
        public abstract String getValueAsString();

        /**
         * 
         * @return the start offset of the next field in this map
         */
        public int getStartOffsetOfNextField()
        {
            return offset + sizeInBytes;
        }

        /**
         * 
         * @return The byte offset of this field in the map
         */
        public int getFieldOffset()
        {
            return offset;
        }

        /**
         * 
         * @return The size in bytes of this field
         */
        public int getFieldSizeInBytes()
        {
            return sizeInBytes;
        }

        /**
         * 
         * @return {@code true} if this field is reserved, false otherwise
         */
        public boolean isReservedField()
        {
            return isReservedField;
        }

        /**
         * 
         * @return the name of the field
         */
        public String getName()
        {
            return name;
        }

        /**
         * @return the name of the field, formatted and aligned for printed, along with debug info if debug is enabled
         */
        protected String getFormattedName()
        {
            StringBuilder message = new StringBuilder();
            message.append(String.format("%-" + maxFieldNameLength + "s: ", name));
            if (debug)
            {
                message.append(String.format("(%s): ", getFieldSpecificInfo()));
            }
            return message.toString();
        }

        /**
         * 
         * field specific info is any data that is useful for debug purposes. This is printed when debug mode is enabled
         * @return debug data for the field
         */
        protected String getFieldSpecificInfo()
        {
            return String.format("offset: %d", offset);
        }

        /**
         * @return the name and value of this field, formatted to be aligned with the rest of the structure
         */
        @Override
        public String toString()
        {
            return getFormattedName() + getValueAsString();
        }
    }

    /**
     * This is used if the field is a simple integer value between 0 and 8 bytes long
     */    
    public class NumberField extends AbstractField
    {

        private boolean printDecimal;
        private Signedness signedness;
        
        /**
         * Base Constructor
         * @param name - name of field
         * @param size - size of the field in bytes
         * @param signedness - whether the field is signed or unsigned
         * @param printDecimal - whether to also print the value in decimal if necessary (hex will always print)
         */
        public NumberField(String name, int size, Signedness signedness, boolean printDecimal)
        {
            super(name, size);
            this.printDecimal = printDecimal;
            this.signedness = signedness;
            if (size > 8 || size < 0)
            {
                throw new IllegalValueInBufferFieldException("The size of a Number field must be between 0 and 8 bytes!\n"
                        + "Field %s at offset %d specified a size of %d", name, getFieldOffset(), size);
            }
        }
        
        /**
         * Standard constructor. Defines an unsigned number of the specified size
         * 
         * @param name - name of the field
         * @param size - size in bytes
         * @param printDecimal - whether to also print the value in decimal if necessary (hex will always print)
         */
        public NumberField(String name, int size, boolean printDecimal)
        {
            this(name, size, Signedness.UNSIGNED, printDecimal);
        }
        
        /**
         * Standard constructor. Defines a number of the specified size and signedness
         *
         * @param name - name of the field
         * @param size - size in bytes
         * @param signedness - if the value is signed or unsigned
         */
        public NumberField(String name, int size, Signedness signedness)
        {
            this(name, size, signedness, true);
        }
        
        /**
         * Standard constructor. Defines an unsigned number of the specified size
         *
         * @param name - name of the field
         * @param size - size in bytes
         */
        public NumberField(String name, int size)
        {
            this(name, size, true);
        }
        
        /**
         * 
         * @return the raw, unsigned version of the value if signed. Otherwise, same as {@code getUnsignedValue()}
         */
        public long getRawValue()
        {
            return BufferMap.this.getValue(getFieldOffset(), getFieldSizeInBytes(), Signedness.UNSIGNED);
        }
        
        /**
         * 
         * @return the value of this field as a long. Allows for unsigned Integer values to maintain their correct value,
         * since Java does not natively support unsigned values
         */
        public long getValue()
        {
            return BufferMap.this.getValue(getFieldOffset(), getFieldSizeInBytes(), signedness);
        }

        /**
         * 
         * @return the value of this field as an int
         * @throws RuntimeException if the value stored in the field cannot fit in a standard Java int (-2^31 through 2^31-1)
         */
        public int getValueAsInt()
        {
            return BufferMap.this.getValueAsInt(getFieldOffset(), getFieldSizeInBytes(), signedness);
        }
        
        /**
         * 
         * will modify this field in the buffer with the specified value
         * NOTE: Any data structures that may depend on this value will not get updated. This map will need to be reinstantiated
         * @param value - value to set
         * @throws IllegalValueInBufferFieldException if the value is too large to fit into the specified number of bytes
         */
        public void setValue(long value)
        {
            BufferMap.this.setValue(getFieldOffset(), getFieldSizeInBytes(), value);
        }

        /**
         * 
         * will modify this field in the buffer with the specified value
         * NOTE: Any data structures that may depend on this value will not get updated. This map will need to be reinstantiated
         * @param value - value to set
         * @throws IllegalValueInBufferFieldException if the value is too large to fit into the specified number of bytes
         */
        public void setValue(int value)
        {
            BufferMap.this.setValue(getFieldOffset(), getFieldSizeInBytes(), value);
        }
        
        /**
         * 
         * @return the maximum value that can fit in this field if treated as an unsigned long
         */
        public long getMaxValueOfField()
        {
            return getBitMask(getFieldSizeInBytes() * 8);
        }


        @Override
        protected String getFieldSpecificInfo()
        {
            return String.format("%s, size: %s", super.getFieldSpecificInfo(), bytesToString(sizeInBytes));
        }

        /**
         * @return the value as a formatted string. The number of leading 0s in the hex string will preserve the actual size of the field
         * (i.e. a value of 2 will print as 0x0002 in a 2 byte field and 0x00000002 in a 4 byte field)
         */
        @Override
        public String getValueAsString()
        {
            return getValueAsString(printDecimal);
        }
        
        /**
         * @return a formatted string representing the value of this field.
         * allows you to force print or force hide the printing of the decimal version of this field
         * @param printDecimal - whether decimal should be printed. Decimal will only print if the value is outside of the range 0 - 9
         */
        public String getValueAsString(boolean printDecimal)
        {
            if (getFieldSizeInBytes() > 0)
            {
                StringBuilder message = new StringBuilder();
                message.append(String.format("0x%0" + (getFieldSizeInBytes() * 2) + "X", getRawValue()));
                if (printDecimal && (getValue() < 0 || getValue() > 9))
                {
                    message.append(String.format(" (%d)", getValue()));
                }
                return message.toString();
            }
            return "";
        }
        
        /**
         * standard toString() method with the optional parameter that allow
         * the ability to enable or disable printing the value in decimal along with the hex
         * @param printDecimal
         * @return string representation of the field
         */
        public String toString(boolean printDecimal)
        {
            return getFormattedName() + getValueAsString(printDecimal);
        }
    }
    
    /**
     * A NumberField where the value returned MUST equal the expectedValue specified.
     * or an IllegalValueInStaticFieldException will be thrown
     */
    public class StaticNumberField extends NumberField
    {
        /**
         * Standard constructor. Defines an unsigned number of the specified
         * size and signedness, as well as the expected value that the field must equate to
         *
         * @param name - name of the field
         * @param size - size in bytes
         * @param signedness - if the value is signed or unsigned
         * @param expectedValue - the expected value that this field must equate to
         * 
         * @throws IllegalValueInStaticFieldException thrown if the value in the field does not equal the specified value
         */
        public StaticNumberField(String name, int size, Signedness signedness, long expectedValue)
        {
            super(name, size, signedness);
            if (getValue() != expectedValue)
            {
                throw new IllegalValueInStaticFieldException("Static field \"%s\" was expected to have a value of 0x%X. "
                        + "Instead, it reported: 0x%X", getName(), expectedValue, getValue());
            }
        }

        /**
         * Standard constructor. Defines an unsigned number of the specified
         * size, as well as the expected value that the field must equate to
         *
         * @param name - name of the field
         * @param size - size in bytes
         * @param expectedValue - the expected value that this field must equate to
         * 
         * @throws IllegalValueInStaticFieldException thrown if the value in the field does not equal the specified value
         */
        public StaticNumberField(String name, int size, long expectedValue)
        {
            this(name, size, Signedness.UNSIGNED, expectedValue);
        }

        /**
         * Standard constructor. Defines a number of the specified size and signedness,
         * as well as the expected value that the field must equate to
         *
         * @param name - name of the field
         * @param size - size in bytes
         * @param signedness - if the value is signed or unsigned
         * @param expectedValue - the expected value that this field must equate to
         * 
         * @throws IllegalValueInStaticFieldException thrown if the value in the field does not equal the specified value
         */
        public StaticNumberField(String name, int size, Signedness signedness, int expectedValue)
        {
            this(name, size, signedness, 
                    (expectedValue & ((signedness == Signedness.UNSIGNED) ? 0xFFFFFFFFL : -1))); //preserve signedness of value
        }

        /**
         * Standard constructor. Defines an unsigned number of the specified size,
         * as well as the expected value that the field must equate to
         *
         * @param name - name of the field
         * @param size - size in bytes
         * @param expectedValue - the expected value that this field must equate to
         * 
         * @throws IllegalValueInStaticFieldException thrown if the value in the field does not equal the specified value
         */
        public StaticNumberField(String name, int size, int expectedValue)
        {
            this(name, size, Signedness.UNSIGNED, expectedValue);
        }
    }

    
    
    /**
     * Interface used by Enums that do not represent a contiguous range of values.
     * Enums that implement this interface should generally look like the following:
     * <pre>
     * public enum EnumExample implements IntegerEnum
     * {
     *     VAL1(1),
     *     VAL2(2),
     *     VAL3(5);
     *     private int value;
     *     private EnumExample(int val){value = val;}
     *     public int getIntValue(){return value;}
     *  }
     * </pre>
     * 
     */
    public interface IntegerEnum
    {

        /**
         * 
         * @return the integer value that this enum represents
         */
        public int getIntValue();
    }
    
    /**
     * Translate the enumConstant into it's underlying value.
     * If the enum is not an IntegerEnum, then its value is its ordinal in the enum defintion
     */
    private static int getEnumValue(Enum<?> enumConstant)
    {
        int value;
        if (enumConstant instanceof IntegerEnum)
        {
            value = ((IntegerEnum) enumConstant).getIntValue();
        }
        else
        {
            value = enumConstant.ordinal(); 
        }
        return value;
    }
    
    /**
     * This class is for defining a field where each value has a specified meaning.
     * You create an enum where each entry represents the different possible values (starting at 0, 1, 2, etc).
     * The class simply uses the ordinal of the enum to match it with the field value, 
     * so by default no values can be skipped when declaring the enum.
     * It will print the {@code toString()} of the enum when printing the buffer map. 
     * By default, this is simply the enum name, but you can override this when creating the enum.
     * 
     * If you wish to have enums for arbitrary integers that do not have a definition for every possible value,
     * have your enum implement the {@code IntegerEnum} interface.
     * The enum should look something like this:
     * 
     * <pre>
     * public enum EnumExample implements IntegerEnum
     * {
     *     VAL1(1),
     *     VAL2(2),
     *     VAL3(5);
     *     private int value;
     *     private EnumExample(int val){value = val;}
     *     public int getIntValue(){return value;}
     *  }
     * </pre>
     * 
     * 
     * Only the listed values will translate to enums. Otherwise, the number will simply be reported
     * @param <T> - generic Enum type to be used when parsing the data in the field
     */
    public class EnumNumberField<T extends Enum<?>> extends NumberField
    {
        private Class<T> enumClass;
        
        private HashMap<Integer, T> lookupTable;

        /**
         * Standard Constructor
         * @param name - the name of the field
         * @param size - the size of the field in bytes
         * @param enumType - the Enum that this data maps to
         */
        public EnumNumberField(String name, int size, Class<T> enumType)
        {
            super(name, size);
            enumClass = enumType;
            lookupTable = new HashMap<Integer, T>();
            for(T enumConstant : enumClass.getEnumConstants())
            {
                int value = getEnumValue(enumConstant);
                lookupTable.put(value, enumConstant);
            }
        }
        
        /**
         * Update the value in this field with the number indicated by the enum
         * NOTE: Any data structures that may depend on this value will not get updated. This map will need to be reinstantiated
         * @param enumConstant
         */
        public void setValue(T enumConstant)
        {
            setValue(getEnumValue(enumConstant));
        }

        /**
         * 
         * @return the value in this field as represented by the enum with the matching integer value, or null if no enum matches the value
         */
        public T getValueAsEnum()
        {
            try
            {
                return lookupTable.get(getValueAsInt());
            }
            catch (Exception e)
            {
                return null;
            }
        }

        /**
         * @return a formatted string representing the value of this field.
         * If the data maps to an enum, also print the enum
         * allows you to force print or force hide the printing of the decimal version of this field
         * @param printDecimal - whether decimal should be printed. Decimal will only print if the value is outside of the range 0 - 9
         */
        @Override
        public String getValueAsString(boolean printDecimal)
        {
            T enumValue = getValueAsEnum();
            if (enumValue != null)
            {
                return String.format("%s (%s)", super.getValueAsString(printDecimal), getValueAsEnum());
            }
            else
            {
                return super.getValueAsString(printDecimal);
            }
        }
    }
    
    /**
     * This class is for fields that are less than a single byte long.
     * this class considers the start bit offset of a field to be the lower order bit of the field 
     * (e.g. if the field is bits 3, 2, and 1 of a byte, then the start bit is 1 and the sizeInBits is 3)
     * <p>
     * When declaring the different BitsFields in a byte, declare the lower-order bit fields first 
     * (i.e start with the field that starts at bit 0, and end with the field that ends at bit 7) 
     */
    public class BitsField extends AbstractField
    {
        private boolean printDecimal;

        /**
         * Standard constructor
         * @param name - name of the field
         * @param sizeInBits - size of the field in bits
         * @param printDecimal - whether to also print the decimal representation of these bits
         */
        public BitsField(String name, int sizeInBits, boolean printDecimal)
        {
            super(name, sizeInBits, LengthUnits.BITS);
            this.printDecimal = printDecimal;
        }
        
        /**
         * Standard constructor. Will not print the decimal representation by default, unlike NumberField
         * @param name - name of the field
         * @param sizeInBits - size of the field in bits
         */
        public BitsField(String name, int sizeInBits)
        {
            this(name, sizeInBits, false);
        }

        //only this class will give public exposure to the bitOffset and sizeInBits variables, since it's the only class where they are relevant
        /**
         * 
         * @return the start bit offset of this field in the byte
         */
        public int getFieldBitOffset()
        {
            return bitOffset;
        }

        /**
         * 
         * @return size of this field in bits
         */
        public int getFieldSizeInBits()
        {
            return sizeInBits;
        }

        /**
         * 
         * @return value stored in the bits of this field
         */
        public int getValue()
        {
            return getBits(getFieldOffset(), bitOffset, sizeInBits);
        }
        
        /**
         * sets the bits in this field to the specified value
         * @param value
         */
        public void setValue(int value)
        {
            setBits(getFieldOffset(), bitOffset, sizeInBits, value);
        }

        /**
         * 
         * @return the maximum value that can fit in this bit field
         */
        public int getMaxValueOfField()
        {
            return (int)getBitMask(sizeInBits);
        }

        //need to make sure the start offset of the next field is correct
        @Override
        public int getStartOffsetOfNextField()
        {
            int startOffsetOfNextField = getFieldOffset(); //assume the next field will still be in the same byte
            if ((bitOffset + sizeInBits) == 8)//if the next field will start a new byte, then update the start offset of the next field by 1.
            {
                startOffsetOfNextField++;
            }
            return startOffsetOfNextField;
        }

        @Override
        protected String getFieldSpecificInfo()
        {
            return String.format("%s, bit offset: %d, size: %s", super.getFieldSpecificInfo(), bitOffset, bitsToString(sizeInBits));
        }

        /**
         * @return a formatted string representing the value of this field in hex, binary, and optionally decimal
         */
        @Override
        public String getValueAsString()
        {
            return getValueAsString(printDecimal);
        }
        
        /**
         * @return a formatted string representing the value of this field in hex, binary, and optionally decimal
         * @param printDecimal - whether decimal should be printed. Decimal will only print if the value is outside of the range 0 - 9
         */
        public String getValueAsString(boolean printDecimal)
        {
            int numberOfHexDigits = sizeInBits / 4 + ((sizeInBits % 4) == 0 ? 0 : 1);
            StringBuilder message = new StringBuilder();
            message.append(String.format("0x%0" + numberOfHexDigits + "X", getValue()));
            
            if (printDecimal && (getValue() < 0 || getValue() > 9))
            {
                message.append(String.format(" (%d)", getValue()));
            }
            
            String valueInBinary = String.format("%" + sizeInBits + "s", Integer.toBinaryString(getValue())).replace(' ', '0');
            message.append(String.format(" (%sb)", valueInBinary));
            return message.toString();
        }
        
        /**
         * @param printDecimal - whether to print the value as a decimal alongside the hex
         * @return - value as a string
         */
        public String toString(boolean printDecimal)
        {
            return getFormattedName() + getValueAsString(printDecimal);
        }
    }

    /**
     * This class is for fields that are a single bit long
     */
    public class BitField extends BitsField
    {

        /**
         * Standard constructor. No size needs to be specified since a BitField is always 1 bit
         * @param name - name of the field
         */
        public BitField(String name)
        {
            super(name, 1);
        }

        /**
         * 
         * @return {@code true} if the bit is 1, {@code false} if 0
         */
        public boolean getValueAsBoolean()
        {
            return (getValue() == 1);
        }
        
        /**
         * sets the bit to the corresponding value
         * @param value - {@code true} = 1, {@code false} = 0 
         */
        public void setValue(boolean value)
        {
            int valueAsInt = value ? 1 : 0;
            setValue(valueAsInt);
        }


        @Override
        public String getValueAsString(boolean printDecimal)
        {
            return String.format("%db", getValue()); //ignore printDecimal. only possible values are 0 and 1
        }
    }

    /**
     * Similar to an EnumNumberField, but this works on fields that are only a few bits long
     * @param <T>
     */
    public class EnumBitsField<T extends Enum<?>> extends BitsField
    {
        private Class<T> enumClass;
        private HashMap<Integer, T> lookupTable;

        /**
         * Standard Constructor
         * @param name - the name of the field
         * @param sizeInBits - the size of the field in bits
         * @param enumType - the Enum that this data maps to
         * @param printDecimal - whether to also print the value of this field in decimal
         */
        public EnumBitsField(String name, int sizeInBits, Class<T> enumType, boolean printDecimal)
        {
            super(name, sizeInBits, printDecimal);
            enumClass = enumType;
            lookupTable = new HashMap<Integer, T>();
            for (T enumConstant : enumClass.getEnumConstants())
            {
                int value = getEnumValue(enumConstant);
                lookupTable.put(value, enumConstant);
            }
        }
        
        /**
         * Standard Constructor
         * @param name - the name of the field
         * @param sizeInBits - the size of the field in bytes
         * @param enumType - the Enum that this data maps to
         */
        public EnumBitsField(String name, int sizeInBits, Class<T> enumType)
        {
            this(name, sizeInBits, enumType, true);
        }

        /**
         * Update the value in this field with the number indicated by the enum
         * NOTE: Any data structures that may depend on this value will not get updated. This map will need to be reinstantiated
         * @param enumConstant
         */
        public void setValue(T enumConstant)
        {
            setValue(getEnumValue(enumConstant));
        }
        
        /**
         * 
         * @return the value in this field as represented by the enum with the matching integer value, or null if no enum matches the value
         */
        public T getValueAsEnum()
        {
            try
            {
                return lookupTable.get(getValue());
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public String getValueAsString(boolean printDecimal)
        {
            T enumValue = getValueAsEnum();
            if (enumValue != null)
            {
                return String.format("%s (%s)", super.getValueAsString(printDecimal), getValueAsEnum());
            }
            else
            {
                return super.getValueAsString(printDecimal);
            }
        }
    }

    
    /**
     * This class is for fields that are interpreted as ASCII strings
     */
    public class StringField extends NumberArrayField
    {
    	/**
    	 * if {@code true}, will print the underlying hex values that make up this string along with the string.
    	 * If the data in the string contains invalid ASCII characters, the hex will automatically be printed
    	 */
        protected boolean printHex;

        /**
         * Standard constructor
         * @param name - name of the field
         * @param size - size of the string in bytes
         */
        public StringField(String name, int size)
        {
            this(name, size, false);
        }

        /**
         * Standard constructor
         * @param name - name of the field
         * @param size - size of the string in bytes
         * @param printHex - whether to also print the hexidecimal representation of thee string
         */
        public StringField(String name, int size, boolean printHex)
        {
            super(name, 1, size);
            this.printHex = printHex;
        }
        
        /**
         * Used if we don't know the size of the field at this point (used only by NullTerminatedStringField
         * @param name
         */
        protected StringField(String name)
        {
        	super(name, 1);
        }

        /**
         * 
         * @return a string with the ASCII representation of the data. The length() of the String may be less than the length of the field,
         * depending on if there are illegal ASCII characters in the array
         */
        public String getValue()
        {
            return getASCIIString(getFieldOffset(), getFieldSizeInBytes()); //will truncate any non ASCII characters
        }
        
        /**
         * @return - {@code String} object whose {@code length()} will always return the size specified, 
         * even if the string might contain invalid character
         */
        public String getValueAsRawASCII()
        {
            return getRawASCIIString(getFieldOffset(), getFieldSizeInBytes());
        }

        /**
         * 
         * will modify this field in the buffer with the specified value
         * NOTE: Any data structures that may depend on this value will not get updated. This map will need to be reinstantiated
         * @param newValue - value to set
         * @throws IllegalValueInBufferFieldException if the newValue is the wrong size
         */
        public void setValue(String newValue)
        {
            if (newValue.length() != getFieldSizeInBytes())
            {
                throw new IllegalValueInBufferFieldException("Specified String \"%s\" has a length of %d.\nThe length of the field %s is %d",
                        newValue, newValue.length(), getName(), getFieldSizeInBytes());
            }
            setASCIIString(getFieldOffset(), newValue);
        }
        
        public String getValueAsString(boolean printHex)
        {
            //if value != raw ASCII, then there are invalid ACII characters, so print the whole thing in hex as well
            if (printHex || debug || !isValidAsciiString())
            {
                return String.format("\"%s\" %s", getValue(), super.getValueAsString());
            }
            else
            {
                return String.format("\"%s\"", getValue());
            }
        }
        
        /**
         * 
         * @return the value of this field as a String with quotation marks around it (to indicate that the data is a string)
         * If any of the bytes within the string are invalid ASCII characters or printHex is enabled, the hexidecimal representation of the string
         * will also print
         */
        @Override
        public String getValueAsString()
        {
            return getValueAsString(printHex);
        }
        
        /**
         * @return {@code} true if the string contains all valid ASCII characters, {@code false} otherwise
         */
        public boolean isValidAsciiString()
        {
            return getValue().equals(getValueAsRawASCII());
        }
    }
    
    /**
     * A StaticStringField where the value returned MUST equal the expectedValue specified
     * or an IllegalValueInStaticFieldException will be thrown

     */
    public class StaticStringField extends StringField
    {
        /**
         * Standard constructor. Defines a String of the specified number of bytes, 
         * as well as the expected value that the field must equate to
         * @param name - name of the field
         * @param size - size of the string in bytes
         * @param printHex - whether to also print the hexadecimal representation of thee string
         * @param expectedValue - the expected value that this field must equate to
         * 
         * @throws IllegalValueInStaticFieldException thrown if the value in the field does not equal the specified value
         */
        public StaticStringField(String name, int size, boolean printHex, String expectedValue)
        {
            super(name, size, true);
            if(!getValue().equals(expectedValue))
            {
                throw new IllegalValueInStaticFieldException(String.format("Static field \"%s\" was expected to have a value of \"%s\". "
                        + "Instead, it reported: %s", getName(), expectedValue, getValueAsString(true)));
            }
        }
        
        /**
         * Standard constructor. Defines a String of the specified number of bytes, 
         * as well as the expected value that the field must equate to
         * @param name - name of the field
         * @param size - size of the string in bytes
         * @param expectedValue - the expected value that this field must equate to
         * 
         * @throws IllegalValueInStaticFieldException thrown if the value in the field does not equal the specified value
         */
        public StaticStringField(String name, int size, String expectedValue)
        {
            this(name, size, false, expectedValue);
        }
    }
    
    /**
     * This class is for fields that are interpreted as ASCII strings 
     * that have an unknown length but will terminate in the NULL ('\0') character
     */
    public class NullTerminatedStringField extends StringField
    {

        /**
         * Standard constructor. Since we do not know the size of the field ahead of time,
         * do not specify it.
         * @param name - name of field
         */
        public NullTerminatedStringField(String name)
    	{
    		super(name);
    		int length = 0;
    		int startOffsetoffset = getCurrentOffset();
    		while(BufferMap.this.getUnsignedValue(startOffsetoffset + length, 1) != 0)
    		{
    			addField();
    			length++;
    		}
    		addField(); //add the null terminating character
    		finalizeArray();
    	}
        
        /**
         * @return {@code} true if the string contains all valid ASCII characters and the terminating null, {@code false} otherwise.
         * return {@code false} if the string only contains the terminating null, so the NULL will print next to the empty string
         */
        @Override
        public boolean isValidAsciiString()
        {
        	int length = getValue().length();
        	return (length > 1) && length == (getFieldSizeInBytes() - 1); //-1 for null terminating character
        }
    }
    	
    
    /**
     * This class is for fields that contain a large amount of data that does not need to be parsed
     * By default, the toString() output only prints the number of bytes in the buffer.
     * However, a flag can be specified in the constructor to have it print the contents 
     * of the buffer in a formatted hex table
     */
    public class BufferField extends AbstractField
    {
    	private Buffer subBuffer;
        private boolean printBuffer;
        
        /**
         * Standard constructor - will only display the size of the field in toString(), not the actual contents of the buffer
         * @param name - name of field
         * @param sizeInBytes - size of the field in bytes
         */
        public BufferField(String name, int sizeInBytes)
        {
            this(name, sizeInBytes, false);
        }
        
        /**
         * Standard constructor
         * @param name - name of field
         * @param sizeInBytes - size of the field in bytes
         * @param printBuffer - whether to print the contents of the buffer in toString()
         */
        public BufferField(String name, int sizeInBytes, boolean printBuffer)
        {
            super(name, sizeInBytes);
            this.printBuffer = printBuffer;
            subBuffer = new Buffer(getData(), getEndian(), getOffset() + getFieldOffset(), sizeInBytes);
        }
        
        public Buffer getValue()
        {
            return subBuffer;
        }
        
        /**
         * sets up whether or not to print the contents of the buffer when calling toString()
         * @param print {@code true} to print the data in the buffer, {@code false} to only print the size of the buffer
         */
        public void enableBufferPrinting(boolean print)
        {
            printBuffer = print;
        }
          
        @Override
        protected String getFormattedName()
        {
            //for Buffer fields, the name is going to be the only thing on that line if we are printing the buffer.
            //just print the name with a ":" immediately after it with no spaces
            if (needToPrintBuffer())
            {
                StringBuilder message = new StringBuilder();
                message.append(String.format("%s: ", getName()));
                if (debug)
                {
                    message.append(String.format("(%s): ", getFieldSpecificInfo()));
                }
                return message.toString();
            }
            else
            {
                return super.getFormattedName();
            }
        }
        
        private boolean needToPrintBuffer()
        {
            return (printBuffer && getFieldSizeInBytes() > 0);
        }
        
        @Override
        protected String getFieldSpecificInfo()
        {
            return String.format("%s, size: %s", super.getFieldSpecificInfo(), bytesToString(getFieldSizeInBytes()));
        }

        @Override
        public String getValueAsString()
        {
        	return subBuffer.dumpDataToString();
        }
        
        @Override
        public String toString()
        {
            if (needToPrintBuffer())
            {
                StringBuilder message = new StringBuilder();
                message.append(getFormattedName());

                //indent each line of the value
                String lines[] = getValueAsString().split("\\r?\\n");
                for (String line : lines)
                {
                    message.append(NEWLINE).append(INDENT).append(line);
                }
                return message.toString();
            }
            else
            {
                return String.format("%s{%s}", getFormattedName(), bytesToString(getFieldSizeInBytes()));
            }
        }
    }
    
    /**
     * This class is for specifying fields that themselves are complex structures that contain other fields
     * @param <E> the class type of the field
     */
    public class ComplexField<E extends BufferMap> extends AbstractField
    {

        private E childMap;

        /**
         * Constructor. Using the class type and additional Parameters specified, uses Java reflection to build an instance of the map object.
         * Reflection is used to eliminate the need for the user to have to manually create it themselves when defining this field.
         * Also, it is needed for ComplexArrayField to be able to dynamically create an array of objects based on the sizes specifid
         * @see ComplexArrayField
         * 
         * @param name - name of the field
         * @param classType - BufferMap class that will be a sub map of this map
         * @param additionalParameters - this MUST contain a list of parameters that are in the signature of one of the constructors
         * in {@code classType}. Otherwise an Exception will be thrown
         * For example, if the constructor is: E(BufferMap parent, boolean param0), additionalParameters will be 1 element long and will contain a Boolean 
         * @throws IllegalArgumentException if no constructors were found that match the format:
         * <p>public E(BufferMap parent, additionalParameters)
         */
        public ComplexField(String name, Class<E> classType, Object... additionalParameters)
        {
            super(name);
            try
            {
                this.childMap = generateNewChildBufferMap(classType, additionalParameters); //uses reflection to create an instance of this specific BufferMap
                childMap.mapName = childMap.getParent().getMapName() + " -> " + getName();
                bind(childMap.getCurrentSizeOfMap());
            }
            catch (ReflectiveOperationException e)
            {
                throw new IllegalArgumentException("Error occurred!\n" +
                		BufferMap.this.toString(), e);
            }
        }

        /**
         *
         * @return the underlying BufferMap wrapped by this field
         */
        public E getValue()
        {
            return childMap;
        }

        /**
         * 
         * @return the contents of the specified map type as a String
         */
        @Override
        public String getValueAsString()
        {
            return childMap.toString();
        }

        @Override
        protected String getFieldSpecificInfo()
        {
            return String.format("%s, size: %s", super.getFieldSpecificInfo(), bytesToString(sizeInBytes));
        }

        @Override
        public String toString()
        {
            StringBuilder message = new StringBuilder();
            message.append(getFormattedName());

            //indent each line of the childMap. This will have a nice recursive effect if we have nested ComplexFields
            String lines[] = getValueAsString().split("\\r?\\n");
            for (String line : lines)
            {
                message.append(NEWLINE).append(INDENT).append(line);
            }
            return message.toString();
        }

        @Override
        protected String getFormattedName()
        {
            //for Complex fields, the name is going to be the only thing on that line.
            //just print the name with a : immediately after it with no spaces
            StringBuilder message = new StringBuilder();
            message.append(String.format("%s: ", getName()));
            if (debug)
            {
                message.append(String.format("(%s): ", getFieldSpecificInfo()));
            }
            return message.toString();
        }
    }
    

    /**
     * uses Java reflection to create an instance of an BufferMap object.
     *  One of the constructors of the class MUST be in the form of:
     *  <pre>
     *  public E(BufferMap parent, Object... additionalParams)
     * </pre>
     * @param <E> BufferMap class that is to be constructed
     * @param classType the class object of type E
     * @param additionalParams  this MUST contain a list of parameters that
     * are in the signature of one of the constructors in {@code classType}.
     * Otherwise an Exception will be thrown. For example, if the constructor
     * is: {@code E(BufferMap parent, boolean param0)}, additionalParameters will be 1
     * element long and will contain a Boolean
     * @return the BufferMap that was constructed
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException 
     */
    @SuppressWarnings("unchecked")
    private <E extends BufferMap> E generateNewChildBufferMap(Class<E> classType, Object... additionalParams)
            throws ReflectiveOperationException
            
    { 
        try
        {
            boolean constructorFound;
            //check each constructor in the class for a matching instance
            for (Constructor<?> constructor : classType.getConstructors())
            {
                constructorFound = true;
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                Object[] parameters = new Object[parameterTypes.length];

                int additionalParamIndex = 0;

                for (int i = 0; i < parameterTypes.length; i++)
                {
                    Class<?> parameterClassType = parameterTypes[i];
                    BufferMap target = this;
                    //The loop is due to the fact that since sub BufferMaps are generally inner classes, 
                    //there are hidden parameters in the constructor that must be accounted for.
                    //These parameters are the parent classes of these inner classes
                    do
                    {
                            //need to use ClassUtils instead of the built-in Class.isInstance method,
                        //since that will not allow for auto-boxing of primitives into their wrapper classes
                        if (ClassUtils.isInstance(parameterClassType, target))
                        {
                            parameters[i] = target;
                            break; //break out of do/while loop, as we have found the correct object
                        }
                        target = target.getParent();
                    }
                    while (target != null);

                    if (target == null) //reached the part of the constructor that takes additional parameters
                    {
                        if ((additionalParams.length <= additionalParamIndex) || !ClassUtils.isInstance(parameterClassType, additionalParams[additionalParamIndex]))
                        {
                            constructorFound = false;
                            break; //break out of the for loop. this is not the correct constructor
                        }
                        parameters[i] = additionalParams[additionalParamIndex];
                        additionalParamIndex++;
                    }
                }
                if (constructorFound)
                {
                    return (E) constructor.newInstance(parameters); //the constructor must have the correct inputs
                }
            }
        }
        catch(ReflectiveOperationException e)
        {
            if(ClassUtils.isInstance(IllegalValueInBufferFieldException.class, e.getCause()))
            {
                throw (IllegalValueInBufferFieldException)e.getCause();
            }
        }
        
        //hasn't returned. Print error
        String error = String.format("\nNo constructors exist in %s that match the specified inputs.", classType);
        error+= "\nInputs: " + Arrays.toString(additionalParams);
        error += "\nPossible Constructors:";
        for(Constructor<?> constructor : classType.getConstructors())
        {
            String constructorString = constructor.getName() + "(";
            for(int p = 0; p < constructor.getParameterTypes().length; p++)
            {
                if (p != 0)
                {
                    constructorString += ", ";
                }
                Class<?> parameterType = constructor.getParameterTypes()[p];
                constructorString += parameterType;
            }
            constructorString += ")";
            error += "\n"+constructorString; 
        }
        error +="\n";
        throw new ReflectiveOperationException(error);
    }
    
     /**
     * The abstract class for dealing with an array of the same data type.
     * Each class that extends this class MUST call setArray() within their constructors once the array has been initialized
     * @param <T> field type to place in the array
     */
    protected abstract class AbstractArrayField<T extends AbstractField> extends AbstractField
    {

        private ArrayList<T> array; //reference array to the internal array of the implementing class
        protected boolean verboseOutput;

        /**
         * Constructor
         * @param name name of the field
         * @param verboseOutput {@code true} prints each element on it's own line, {@code false} print all elements on a single line
         */
        public AbstractArrayField(String name, boolean verboseOutput)
        {
            super(name);
            arrayLock = true; //enable the arrayLock so any field created that gets attached to this field won't get added to the larger structure for this map
            this.verboseOutput = verboseOutput;
        }

        public AbstractArrayField(String name, LengthUnits units, boolean verboseOutput)
        {
            super(name, units);
            arrayLock = true;
            this.verboseOutput = verboseOutput;
        }
        
        /**
         * Implementing class constructor must call this when they begin to populate their arrays
         * @param array - internal array to populate
         */
        protected void startArray(ArrayList<T> array)
        {
             this.array = array;
             fields.add(this);
        }
        
        protected void finalizeArray()
        {
            //can't call bind(), since all the elements in the array have already been used to calculate the new currentOffset.
            //instead, calculate the fields for this array manually
            offset = array.size() > 0 ? array.get(0).offset : currentOffset;
            bitOffset = array.size() > 0 ? array.get(0).bitOffset : currentBitOffset;
            
            sizeInBytes = 0;
            for (AbstractField field : array)
            {
                sizeInBytes += field.sizeInBytes;
            }
            
            sizeInBits = 0;
            for (AbstractField field : array)
            {
                sizeInBits += field.sizeInBits;
            }

            if (sizeInBytes == 0 && sizeInBits == 0)
            {
                verboseOutput = false; //if no elements, force it to print as non-verbose, which will look like "name: []"
            }
            arrayLock = false; //now that the field has been added, release the arrayLock so subsequent fields can get added correctly again
        }
        
        /**
         * 
         * @return the number of elements in the array
         */
        public int getNumberOfElements()
        {
            return array.size();
        }
        
        /**
         * 
         * @return The underlying objects in this array as an ArrayList 
         */
        public ArrayList<T> getValueAsArrayList()
        {
            return array;
        }

        @Override
        protected String getFieldSpecificInfo()
        {
            if (units == LengthUnits.BYTES)
            {
                return String.format("%s, size: %s", super.getFieldSpecificInfo(), bytesToString(sizeInBytes));
            }
            else
            {
                return String.format("%s, bit offset: %d, size: %s", super.getFieldSpecificInfo(), bitOffset, bitsToString(sizeInBits));
            }
        }

        /**
         * for verbose mode, print each element of the array on its own line
         * <p>for non-verbose mode, simply print the value of each element on the same line
         * @return contents of this array as a formatted string
         */
        @Override
        public String getValueAsString()
        {
            StringBuilder message = new StringBuilder();
            if (verboseOutput)
            {
                for (int i = 0; i < array.size(); i++)
                {
                    if(array.get(i) == null)
                    {
                        continue;
                    }
                    if (i != 0)
                    {
                        message.append(NEWLINE);
                    }
                    message.append(array.get(i).toString());
                }
            }
            else //non-verbose mode
            {
                message.append("[");
                for (int i = 0; i < array.size(); i++)
                {
                    if (array.get(i) == null)
                    {
                        continue;
                    }
                    if (i != 0)
                    {
                        message.append(",");
                    }
                    message.append(array.get(i).getValueAsString());
                }
                message.append("]");
            }

            return message.toString();
        }

        @Override
        public String toString()
        {
            if (verboseOutput)
            {
                return getValueAsString(); //don't print the overall name of the array, each element will have its own name printed (which will be name[i])
            }
            else
            {
                return getFormattedName() + getValueAsString();
            }
        }
    }

    private static String arrayElementName(String name, int index)
    {
        return name + "[" + index + "]";
    }

    /**
     * Class for representing a field that is simply an array of numbers.
     * Similar to BufferField, but can be used to represent an array of Integers (4 bytes each) for instance
     */
    public class NumberArrayField extends AbstractArrayField<NumberField>
    {

        private ArrayList<NumberField> elements;
        private int elementSize;
        private Signedness elementSignedness;

        /**
         * constructor
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         * @param numberOfElements number of elements in the array
         */
        public NumberArrayField(String name, int elementSizeInBytes, int numberOfElements)
        {
            this(name, elementSizeInBytes, numberOfElements, false);
        }
       
        /**
         * Constructor
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         * @param numberOfElements number of elements in the array
         * @param signedness whether or not each element in the array is signed or unsigned
         */
        public NumberArrayField(String name, int elementSizeInBytes, int numberOfElements, Signedness signedness)
        {
            this(name, elementSizeInBytes, numberOfElements, signedness, false);
        }
        
        /**
         * Constructor
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         * @param numberOfElements number of elements in the array
         * @param verboseOutput {@code true} prints each element on it's own line, {@code false} print all elements on a single line
         */
        public NumberArrayField(String name, int elementSizeInBytes, int numberOfElements, boolean verboseOutput)
        {
            this(name, elementSizeInBytes, numberOfElements, Signedness.UNSIGNED, verboseOutput);
        }

        /**
         * Constructor
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         * @param numberOfElements number of elements in the array
         * @param signedness whether or not each element in the array is signed or unsigned
         * @param verboseOutput {@code true} prints each element on it's own line, {@code false} print all elements on a single line
         */
        public NumberArrayField(String name, int elementSizeInBytes, int numberOfElements, Signedness signedness, boolean verboseOutput)
        {
            this(name, elementSizeInBytes, signedness, verboseOutput);
            for (int i = 0; i < numberOfElements; i++)
            {
                addField();
            }
            finalizeArray();
        }

        /**
         * Constructor for when the array has an unknown number of elements. 
         * Calling constructor MUST call finalizeArray() inside their constructor
         * once the number of elements has been determined
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         * @param signedness whether or not each element in the array is signed or unsigned
         * @param verboseOutput {@code true} prints each element on it's own line, {@code false} print all elements on a single line
         */
        protected NumberArrayField(String name, int elementSizeInBytes, Signedness signedness, boolean verboseOutput)
        {
            super(name, verboseOutput);
            elements = new ArrayList<NumberField>();
            elementSize = elementSizeInBytes;
            elementSignedness = signedness;
            startArray(elements);
        }
        
        /**
         * Constructor. verboseOutput is false
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         * @param signedness whether or not each element in the array is signed or unsigned
         */
        public NumberArrayField(String name, int elementSizeInBytes, Signedness signedness)
        {
            this(name, elementSizeInBytes, signedness, false);
        }
        
        /**
         * Constructor. Each element is treated as an unsigned variable
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         * @param verboseOutput {@code true} prints each element on it's own line, {@code false} print all elements on a single line
         */
        public NumberArrayField(String name, int elementSizeInBytes, boolean verboseOutput)
        {
            this(name, elementSizeInBytes, Signedness.UNSIGNED, verboseOutput);
        }

        /**
         * Constructor. Each element is treated as an unsigned variable and verboseOutput is false
         * @param name name of array
         * @param elementSizeInBytes size in bytes of each number within the array
         */
        protected NumberArrayField(String name, int elementSizeInBytes)
        {
            this(name, elementSizeInBytes, false);
        }
        
        /**
         * adds a new NumberField to the array of the specified size and signedness
         */
        protected final void addField()
        {
            NumberField newField = new NumberField(arrayElementName(getName(), elements.size()), elementSize, elementSignedness, verboseOutput);
            elements.add(newField);
        }


        /**
         * @param index array index
         * @return the value at the specified index in the array
         */
        public long getValueAtIndex(int index)
        {
            return elements.get(index).getValue();
        }

        /**
         *
         * @param index array index
         * @return the value at the specified index in the array as an integer
         * @throws RuntimeException if the value stored in the field cannot fit in a standard Java int (-2^31 through 2^31-1)
         */
        public int getValueAtIndexAsInt(int index)
        {
            return elements.get(index).getValueAsInt();
        }

        /**
         * sets the value in the array the specified index to the specified value
         * @param index array index
         * @param value value to set
         * NOTE: Any data structures that may depend on this value will not get updated. This map will need to be reinstantiated
         */
        public void setValueAtIndex(int index, long value)
        {
            elements.get(index).setValue(value);
        }

        /**
         * sets the value in the array the specified index to the specified value
         * @param index array index
         * @param value value to set
         * NOTE: Any data structures that may depend on this value will not get updated. This map will need to be reinstantiated
         */
        public void setValueAtIndex(int index, int value)
        {
            elements.get(index).setValue(value);
        }

        @Override
        protected String getFieldSpecificInfo()
        {
            if(this instanceof StringField) //these fields will always have an element size of 1 byte, no need to print it
            {
                return super.getFieldSpecificInfo();
            }
            else
            {
                return String.format("%s [length: %d, element size: %s]", super.getFieldSpecificInfo(), getNumberOfElements(), bytesToString(elementSize));
            }
        }
    }
    
    /**
     * enum for ComplexArrayField to indicate whether the size specified in the constructor 
     * is the number of elements in the array or the total size of the array in bytes
     */
    public enum ArraySizeType
    {

        NUMBER_OF_ELEMENTS,
        SIZE_IN_BYTES
    }
    
    /**
     * Class for representing a field that is an array of complex structures
     * @param <E> the class type of each element in the array
     */
    public class ComplexArrayField<E extends BufferMap> extends AbstractArrayField<ComplexField<E>>
    {
        private ArrayList<ComplexField<E>> elements;
        
        /**
         * Constructor. Using the class type and additional Parameters specified, uses Java reflection to build an array of instances of the map object.
         * Reflection is needed to allow this class to create an array of instances of the specified class at runtime, which eliminates the need for the user
         * to have to manually create this array
         * @param name name of the field
         * @param classType BufferMap class that describes the contents of each element in the array
         * @param numberOfElements number of elements in the array
         * @param additionalParameters - this MUST contain a list of parameters that are in the signature of one of the constructors
         * in {@code classType}. Otherwise an Exception will be thrown.
         * For example, if the constructor is: E(BufferMap parent, boolean param0), additionalParameters will be 1 element long and will contain a Boolean 
         */
        public ComplexArrayField(String name, Class<E> classType, int numberOfElements, Object... additionalParameters)
        {
            this(name, classType, ArraySizeType.NUMBER_OF_ELEMENTS, numberOfElements, additionalParameters) ;
        }
        
        /**
         * Constructor. Using the class type and additional Parameters specified, uses Java reflection to build an array of instances of the map object.
         * Reflection is needed to allow this class to create an array of instances of the specified class at runtime, which eliminates the need for the user
         * to have to manually create this array
         * @param name name of the field
         * @param classType BufferMap class that describes the contents of each element in the array
         * @param sizeType whether the size variable represents NUMBER_OF_ELEMENTS or SIZE_IN_BYTES
         * @param size size of the array. Either NUMBER_OF_ELEMENTS or SIZE_IN_BYTES
         * @param additionalParameters - this MUST contain a list of parameters that are in the signature of one of the constructors
         * in {@code classType}. Otherwise an Exception will be thrown.
         * For example, if the constructor is: E(BufferMap parent, boolean param0), additionalParameters will be 1 element long and will contain a Boolean 
         */
        public ComplexArrayField(String name, Class<E> classType, ArraySizeType sizeType, int size, Object... additionalParameters)
        {
            super(name, true); //always do verbose output with complex array fields
            if (sizeType == ArraySizeType.NUMBER_OF_ELEMENTS)
            {
                elements = new ArrayList<ComplexField<E>>(size);
                startArray(elements);
                for (int i = 0; i < size; i++)
                {
                    addField(classType, additionalParameters); //uses reflection to create a new instance of the class at the correct offset into the buffer
                }
            }
            else //SIZE_IN_BYTES. We don't have access to the number of elements this array will contain, but we do know how big in bytes the total array is
            {
                elements = new ArrayList<ComplexField<E>>(size);
                startArray(elements);
                int currentSize = 0;
                while (currentSize < size)
                {
                    ComplexField<E> newField = addField(classType, additionalParameters);
                    currentSize += newField.getFieldSizeInBytes();
                }
                if(currentSize != size)
                {
                    throw new IllegalValueInBufferFieldException("Array field %s of was specified with a total size of %d bytes. "
                            + "However, after %d elements, size was %d bytes:\n%s",
                            name, size, elements.size(), currentSize, toString());
                }
            }
            finalizeArray();
        }
        
        /**
         * Uses reflection to create a new instance of the class type and adds it to the element array
         * @param classType BufferMap class that describes the contents of each element in the array
         * @param additionalParameters - this MUST contain a list of parameters that are in the signature of one of the constructors
         * in {@code classType}. Otherwise an Exception will be thrown.
         * For example, if the constructor is: E(BufferMap parent, boolean param0), additionalParameters will be 1 element long and will contain a Boolean 
         * @return the field that was created
         */
        protected final ComplexField<E> addField(Class<E> classType, Object... additionalParameters)
        {
            int i = elements.size();
            ComplexField<E> newField = new ComplexField<E>(arrayElementName(getName(), i), classType, additionalParameters);
            elements.add(newField);
            return newField;
        }
        
        /**
         *
         * @param index array index
         * @return the BufferMap at the specified index
         */
        public E getValueAtIndex(int index)
        {
            ComplexField<E> field = elements.get(index);
            return field.getValue();
        }
        
        @Override
        protected String getFieldSpecificInfo()
        {
            return String.format("%s [length: %d]", super.getFieldSpecificInfo(), getNumberOfElements());
        }
    }

    /**
     * Class used to mark a series of bytes as reserved. This field can be any number of bytes long (is not limited to 8 bytes like a NumberField is)
     */
    public class ReservedField extends BufferField
    {

        /**
         *
         * @param size size in bytes of the field. Can be any non-negative number
         */
        public ReservedField(int size)
        {
            super(RESERVED, size);
            isReservedField = true;
        }
    }

    /**
     * Class used to mark a series of bits that are reserved. This field can be any number of bits long (isn't limited to 8 bits like a BitsField is)
     */
    public class ReservedBitsField extends AbstractArrayField<BitsField>
    {

        private final ArrayList<BitsField> bitsFields;

        /**
         *
         * @param sizeInBits size of the field in bits. can be any number of bits long (isn't limited to 8 bits like a BitsField is)
         */
        public ReservedBitsField(int sizeInBits)
        {
            super(RESERVED, LengthUnits.BITS, false);
            //as individual BitsFields are only allowed to be 0-8 bits long, we must split up this field into individual fields of the allowed sizes
            bitsFields = new ArrayList<BitsField>();
            startArray(bitsFields);
            isReservedField = true;
            int number = 0;
            int sizeOfFirstEntry = Math.min(8 - currentBitOffset, sizeInBits);
            int bitsRemaining = sizeInBits - sizeOfFirstEntry;
            bitsFields.add(new BitsField(arrayElementName(getName(), number), sizeOfFirstEntry));
            number++;
            while (bitsRemaining >= 8)
            {
                bitsFields.add(new BitsField(arrayElementName(getName(), number), 8));
                bitsRemaining -= 8;
                number++;
            }
            if (bitsRemaining > 0)
            {
                bitsFields.add(new BitsField(arrayElementName(getName(), number), bitsRemaining));
            }
            finalizeArray();
        }

        /**
         *
         * @return the start bit offset of this field
         */
        public int getBitOffset()
        {
            return bitOffset;
        }

        /**
         * @return total size of this field in bits
         */
        public int getSizeInBits()
        {
            return sizeInBits;
        }

        @Override
        public int getStartOffsetOfNextField()
        {
            if(bitsFields.isEmpty())
            {
                return super.getStartOffsetOfNextField();
            }
            else
            {
                return bitsFields.get(bitsFields.size() - 1).getStartOffsetOfNextField();
            }
        }
    }
    
    //special exceptions
    
    /**
     * Exception to indicate that, when building the list of AbstractField objects in the BufferMap,
     * an error occurred because the structure defined is invalid in some way
     */
    public class IllegalValueInBufferFieldException extends IllegalArgumentException
    {
        public IllegalValueInBufferFieldException(String format, Object... params)
        {
            super(String.format(format, params));
        }
    }

    /**
     * Exception to indicate that the value in a StaticNumberField or StaticStringFiled
     * did not match the expected value
     */
    public class IllegalValueInStaticFieldException extends IllegalValueInBufferFieldException
    {
        public IllegalValueInStaticFieldException(String format, Object... params)
        {
            super(format, params);
        }
    }
}
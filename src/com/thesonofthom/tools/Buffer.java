package com.thesonofthom.tools;

/**
 * This class is a wrapper around a binary buffer of data (byte array) 
 * that allows easy parsing and modification of data within the array
 * 
 * @author Kevin Thomson
 */
public class Buffer
{

    public static final int BYTE_SIZE = 1;
    public static final int SHORT_SIZE = 2;
    public static final int INT_SIZE = 4;
    public static final int LONG_SIZE = 8;

    /**
     * indicates whether the data is big or endian
     */
    public static enum Endian
    {
        BIG,
        LITTLE
    }

    /**
     * Indicates whether a value is signed or unsigned
     */
    public static enum Signedness
    {
        UNSIGNED,
        SIGNED;
    }

    private final byte[] data;
    private final Endian endian;
    
    private final int offset;
    private final int size;

    /**
     *  Standard constructor. Allows access to the entire byte array
     * @param data - binary data array
     * @param endian - whether to parse this data in BIG or LITTLE endian
     */
    public Buffer(byte[] data, Endian endian)
    {
        this(data, endian, 0, data.length);
    }
    
    /**
     * Constructor where the user can specify an offset into the byte array to begin parsing.
     * The class will act as if the underlying data array starts at the specified offset.
     * The size of this buffer is the length of the actual data minus the start offset
     * @param data - binary data array
     * @param endian - whether to parse this data in BIG or LITTLE endian
     * @param offset - offset into the array to treat as offset 0
     */
    public Buffer(byte[] data, Endian endian, int offset)
    {
        this(data, endian, offset, data.length - offset);
    }
    
    /**
     * Constructor where the user can specify an offset into the byte array to begin parsing.
     * The class will act as if the underlying data array starts at the specified offset
     * and will only allow reading of data up to (offset + size) bytes into the array
     * @param data - binary data array
     * @param endian - whether to parse this data in BIG or LITTLE endian
     * @param offset - offset into the array to treat as offset 0
     * @param size - number of bytes within the byte array to map to the buffer
     */
    public Buffer(byte[] data, Endian endian, int offset, int size)
    {
        validateOffsetAndSize(data, offset, size);
        this.data = data;
        this.endian = endian;
        this.offset = offset;
        this.size = size;
    }
    
    /**
     * Constructor to create a Buffer around a new 0-filled byte array of the specified size
     * @param endian - whether the data should be treated as BIG or LITTLE endian
     * @param size - size in bytes of the new byte array
     */
    public Buffer(Endian endian, int size)
    {
    	this(new byte[size], endian, 0, size);
    }
    
    private static void validateOffsetAndSize(byte[] data, int offset, int size)
    {
        if(offset < 0)
        {
            throw new IllegalArgumentException("Offset must be a non-negative number! Specified offset: " + offset);
        }
        if (offset + size > data.length)
        {
            throw new IllegalArgumentException(String.format("Total size of buffer is %d bytes. "
                    + "However, offset %d + size %d = %d exceeds size of buffer", data.length, offset, size, (offset + size)));
        }
    }
    
    /**
     * 
     * @return the offset into the data array where this buffer starts 
     */
    public int getOffset()
    {
        return offset;
    }
 
    private int getAbsoluteOffset(int offset)
    {
        return this.offset + offset;
    }

    /**
     * 
     * @return the size of the Buffer in bytes. 
     * May be less than the size of the actual underlying data array.
     */
    public int getSize()
    {
        return size;
    }

    /**
     *
     * @return the endianness of the data in the Buffer
     */
    public Endian getEndian()
    {
        return endian;
    }

    /**
     *
     * @return the underlying data array
     */
    public byte[] getData()
    {
        return data;
    }
    
    private void validateOffset(int offset)
    {
        if(offset >= size)
        {
            throw new IllegalArgumentException(String.format("Buffer is only %s in size, but the indicated offset is %d", bytesToString(size), offset));
        }
    }

    /**
     *
     * @param offset - byte offset into the Buffer
     * @return a single byte at the specified offset in the Buffer.
     * Will always return a non-negative value. To convert to byte, simply cast the output to (byte)
     */
    public int getByte(int offset)
    {
        validateOffset(offset);
        return data[getAbsoluteOffset(offset)] & 0xFF; //to prevent sign extension
    }

    /**
     * Sets the specified byte offset in the Buffer to the specified value
     * @param offset - byte offset into the Buffer
     * @param value - value to set. Must be within the range of a single byte (0x00 - 0xFF).
     * @throws IllegalArgumentException if the value to set is outside the legal range
     */
    public void setByte(int offset, int value)
    {
        if (value >= 0 && value <= 0xFF)
        {
            setByte(offset, (byte) value);
        }
        else
        {
            throw new IllegalArgumentException("Value must be between 0 and 0xFF. Instead, value was " + value);
        }
    }

    /**
     * Sets the specified byte offset in the Buffer to the specified value
     * @param offset - byte offset into the Buffer
     * @param value - value to set
     */
    public void setByte(int offset, byte value)
    {
        validateOffset(offset);
        data[getAbsoluteOffset(offset)] = value;
    }

    /**
     * @param offset - byte offset into the Buffer
     * @param sizeInBytes - number of bytes to read
     * @return the the specified amount of data in the array as an unsigned value starting at the specified offset
     * @throws IllegalArgumentException if the size specified is outside the range of 0 to 8 bytes
     */
    public long getUnsignedValue(int offset, int sizeInBytes)
    {
        return getValue(offset, sizeInBytes, Signedness.UNSIGNED);
    }
    
    /**
     * @param offset - byte offset into the Buffer
     * @param sizeInBytes - number of bytes to read
     * @return the the specified amount of data in the array as a signed value starting at the specified offset
     * @throws IllegalArgumentException if the size specified is outside the range of 0 to 8 bytes
     */
    public long getSignedValue(int offset, int sizeInBytes)
    {
        return getValue(offset, sizeInBytes, Signedness.SIGNED);
    }

    /**
     *
     * @param offset - byte offset into the Buffer
     * @param sizeInBytes - number of bytes to read
     * @param signedness - whether the data should be treated as a signed or unsigned variable
     * @return the the specified amount of data in the array starting at the specified offset
     */
    public long getValue(int offset, int sizeInBytes, Signedness signedness)
    {
        if (sizeInBytes > 8 || sizeInBytes < 0)
        {
            throw new IllegalArgumentException("Size must be between 0 and 8. Specified size is " + sizeInBytes);
        }

        boolean mostSignficantBit = false;
        long value = 0;
        for (int i = 0; i < sizeInBytes; i++)
        {
            int indexOfMSB = (endian == Endian.BIG) ? i : (sizeInBytes - i - 1);
            int actualOffset = offset + indexOfMSB;

            int byteValue = getByte(actualOffset);

            if (i == 0) //most significant byte
            {
                mostSignficantBit = ((byteValue & 0x80) >> 7) == 1;
            }

            value = value << 8;
            value = value | byteValue;
        }

        if (signedness == Signedness.SIGNED && mostSignficantBit)
        {
            //sign extend the value
            long mask = -1 & ~getBitMask(sizeInBytes * 8);
            value |= mask;
        }

        return value;
    }

    /**
    *
    * @param offset - byte offset into the Buffer
    * @param sizeInBytes - number of bytes to read
    * @return the the specified amount of data in the array as an unsigned integer starting at the specified offset.
    * @throws RuntimeException if the data is too large to fit in a standard Java int (max value 2^31 - 1)
    */
    public int getUnsignedValueAsInt(int offset, int sizeInBytes)
    {
        return getValueAsInt(offset, sizeInBytes, Signedness.UNSIGNED);
    }

    /**
    *
    * @param offset - byte offset into the Buffer
    * @param sizeInBytes - number of bytes to read
    * @return the the specified amount of data in the array as a signed integer starting at the specified offset.
    * @throws RuntimeException if the data is too large to fit in a standard Java int (-2^31 through 2^31 -1)
    */
    public int getSignedValueAsInt(int offset, int sizeInBytes)
    {
        return getValueAsInt(offset, sizeInBytes, Signedness.SIGNED);
    }

    /**
    *
    * @param offset - byte offset into the Buffer
    * @param sizeInBytes - number of bytes to read
    * @param signedness - whether the data should be treated as a signed or unsigned variable
    * @return the the specified amount of data in the array as an integer starting at the specified offset
    * @throws RuntimeException if the data is too large to fit in a standard Java int (-2^31 through 2^31 -1)
    */
    public int getValueAsInt(int offset, int sizeInBytes, Signedness signedness)
    {
        long value = getValue(offset, sizeInBytes, signedness);
        if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE)
        {
            return (int) value;
        }
        else
        {
            throw new RuntimeException(String.format("The value of the field (0x%X) (%d) (offset %d, size %d) "
                    + "is outside the range that can fit in an Integer (%d - %d)", 
                    value, value, offset, sizeInBytes, Integer.MIN_VALUE, Integer.MAX_VALUE));
        }
    }

    /**
     *
     * @param offset - byte offset into the Buffer
     * @return the the specified amount of data in the array as an short (2 bytes) starting at the specified offset. 
     * Data will always be unsigned. To convert to a signed short, simply cast the return result to (short)
     */
    public int getShort(int offset)
    {
        return getUnsignedValueAsInt(offset, SHORT_SIZE);
    }

    /**
     *
     * @param offset - byte offset into the Buffer
     * @return an unsigned integer (4 bytes) at the specified offset (as a long, since Java doesn't have unsigned integer primitives)
     */
    public long getUnsignedInt(int offset)
    {
        return getUnsignedValue(offset, INT_SIZE);
    }
    
    /**
     *
     * @param offset - byte offset into the Buffer
     * @return a signed integer (4 bytes) at the specified offset
     */
    public int getInt(int offset)
    {
        return getSignedValueAsInt(offset, INT_SIZE);
    }

    /**
     *
     * @param offset - byte offset into the Buffer
     * @return a long (8 bytes) at the specified offset
     */
    public long getLong(int offset)
    {
        return getUnsignedValue(offset, LONG_SIZE);
    }

    /**
     * Updatas the specified range of bytes in the Buffer to the specified value
     * @param offset - byte offset into the Buffer
     * @param sizeInBytes - the amount of bytes to overwrites with the specified value
     * @param value - the value to be written into the buffer
     * @throws IllegalArgumentException if the size in bytes is outside the range of 0 to 8 
     *               or if the value is too large to fit into the specified number of bytes
     */
    public void setValue(int offset, int sizeInBytes, long value)
    {
        if (sizeInBytes > 8 || sizeInBytes < 0)
        {
            throw new IllegalArgumentException("Size must be between 0 and 8. Specified size is " + sizeInBytes);
        }

        long maxPossibleValueOfField = getBitMask(sizeInBytes * 8);

        if ((value & ~maxPossibleValueOfField) != 0)
        {
            throw new IllegalArgumentException(String.format("Value specified (0x%X) is greater than the max that can fit in this field (0x%X)", value, maxPossibleValueOfField));
        }

        for (int i = 0; i < sizeInBytes; i++)
        {
            int indexOfLSB = (endian == Endian.LITTLE) ? i : (sizeInBytes - i - 1);
            int actualOffset = offset + indexOfLSB;

            int currentByte = (int) (value & 0xFF);
            value = value >>> 8;
            setByte(actualOffset, currentByte);
        }
    }
    
    /**
    * Updates 2 bytes in the Buffer at the specified offset to the specified value
    * @param offset - byte offset into the Buffer
    * @param value - the value to be written into the buffer
    * @throws IllegalArgumentException if the value is too large to fit into a short (0xFFFF)
    */
    public void setShort(int offset, int value)
    {
    	setValue(offset, SHORT_SIZE, value);
    }
    

    
    /**
    * Updates 2 bytes in the Buffer at the specified offset to the specified value
    * @param offset - byte offset into the Buffer
    * @param value - the value to be written into the buffer
    */
    public void setShort(int offset, short value)
    {
    	setValue(offset, SHORT_SIZE, (int)value & 0xFFFF); //to prevent sign extension when upcasting from short to int
    }
    
    /**
    * Updates 4 bytes in the Buffer at the specified offset to the specified value
    * @param offset - byte offset into the Buffer
    * @param value - the value to be written into the buffer
    */
    public void setInt(int offset, int value)
    {
    	setValue(offset, INT_SIZE, value);
    }
    
    /**
    * Updates 8 bytes in the Buffer at the specified offset to the specified value
    * @param offset - byte offset into the Buffer
    * @param value - the value to be written into the buffer
    */
    public void setLong(int offset, long value)
    {
    	setValue(offset, LONG_SIZE, value);
    }

    /**
    *
    * @param offset - byte offset into the Buffer
    * @param sizeInBytes - the amount of bytes to overwrites with the specified value
    * @param value - the value to be written into the buffer
    * @throws IllegalArgumentException if the size in bytes is outside the range of 0 to 8 or if the value is too large to fit into the specified number of bytes
    */
    public void setValue(int offset, int sizeInBytes, int value)
    {
        setValue(offset, sizeInBytes, value & 0x0FFFFFFFFL); //to prevent sign extension when converting from int to long
    }


    /**
    *
    * @param byteOffset - byte offset into the Buffer
    * @param startBit - start bit within the byte to read (must be within 0 and 7)
    * @param numberOfBits - numberOfBits within the byte to be read (must be between 0 and 7. startBit + numberOfBits cannot exceed 8)
    * @return the value of the range of bits specified within the byte
    * @throws IllegalArgumentException - if any of the following conditions occur:
    *              <ul>
    *              <li> if the start bit is outside the range 0 to 7</li>
    *              <li> if startBit + numberOfBits > 8</li>
    *              </ul>
    */
    public int getBits(int byteOffset, int startBit, int numberOfBits)
    {
    	validateBitRange(startBit, numberOfBits);
    	
        int mask = (int)getBitMask(numberOfBits);
        mask = mask << startBit; //shift the mask to the correct offset in the byte
        
        int data = getByte(byteOffset) & mask;
        return data  >> startBit; //normalize the value
    }
    
    private static void validateBitRange(int startBit, int numberOfBits)
    {
    	if(startBit < 0 || startBit > 7)
        {
            throw new IllegalArgumentException("Start bit within the byte must be between 0 and 7. " + startBit + " was specified");
        }
        if(numberOfBits < 0)
        {
            throw new IllegalArgumentException("Number of bits must be non-negative! Specified: " + numberOfBits);
        }
    	long endBit = startBit + numberOfBits;
    	if(endBit < 0 || endBit > 8)
    	{
    		throw new IllegalArgumentException(String.format("Range specified (%d bits starting at offset %d) exceeds the range of a single byte", numberOfBits, startBit));
    	}
    }

    /**
    *
    * @param byteOffset - byte offset into the Buffer
    * @param startBit - start bit within the byte to read (must be within 0 and 7)
    * @param numberOfBits - numberOfBits within the byte to be read (must be between 0 and 7. startBit + numberOfBits cannot exceed 8)
    * @param value - the value to set into the specified bit range
    * @throws IllegalArgumentException - if any of the following conditions occur:
    *              <ul>
    *              <li> if the start bit is outside the range 0 to 7</li>
    *              <li> if startBit + numberOfBits > 8</li>
    *              <li> if the value specified is larger than can fit within the bit range</li>
    *              </ul>
    */
    public final void setBits(int byteOffset, int startBit, int numberOfBits, int value)
    {
    	validateBitRange(startBit, numberOfBits);
    	int maxValue = (int)getBitMask(numberOfBits);
    	if(value < 0 || value > maxValue)
    	{
    		throw new IllegalArgumentException(String.format("Value specified (0x%X) is larger than can fit " +
    				"in the specified number of bits (%d bits, max value of 0x%X)", value, numberOfBits, maxValue));
    	}
    	
        int mask = maxValue << startBit; //create a mask at the correct offset in the byte
        //update the range of bits in the byte without overwriting the other data in the byte
        setByte(byteOffset, (getByte(byteOffset) & ~mask) | (value & mask));
    }

    /**
     *
     * @param byteOffset - byte offset into the buffer
     * @param bitOffset - bit offset into the specified byte
     * @return {@code true} if the bit at the specified byte and bitOffset is 1, {@code false} if 0
     * @throws IllegalArgumentException if the bit specified is outside the range 0 to 7
     */
    public boolean getBit(int byteOffset, int bitOffset)
    {
        return getBits(byteOffset, bitOffset, 1) != 0;
        
    }

    /**
    * Sets the specified bit to either 1 (true) or 0 (false)
    * @param byteOffset - byte offset into the buffer
    * @param bitOffset - bit offset into the specified byte
    * @param value true=1, false=0
    * @throws IllegalArgumentException if the bit specified is outside the range 0 to 7
    */
    public void setBit(int byteOffset, int bitOffset, boolean value)
    {
    	int bitValue = value ? 1 : 0;
    	setBits(byteOffset, bitOffset, 1, bitValue);
    }
    
    /**
     * 
     * @param absoluteBitOffset - the absolute bit offset within the buffer.
     * For example, absolute bit offset 12 is the bit at byte offset 1, bit offset 4.
     * @return {@code true} if the specified bit is 1, {@code false} if 0
     */
    public boolean getBit(int absoluteBitOffset)
    {
        int byteOffset = absoluteBitOffset / 8;
        int actualBitOffset = absoluteBitOffset % 8;
        return getBit(byteOffset, actualBitOffset);
    }
    
    /**
     * Sets the specified bit to either 1 (true) or 0 (false)
     * @param absoluteBitOffset - the absolute bit offset within the buffer.
     * For example, absolute bit offset 12 is the bit at byte offset 1, bit offset 4.
     * @param value true=1, false=0
     */
    public void setBit(int absoluteBitOffset, boolean value)
    {
        int byteOffset = absoluteBitOffset / 8;
        int actualBitOffset = absoluteBitOffset % 8;
        setBit(byteOffset, actualBitOffset, value);
    }

    /**
     * Returns a valid ASCII string from the data starting at the specified offset
     * @param offset start offset
     * @param size amount of bytes to read
     * @return a String representing the data in the String. Any non-ASCII characters (outside the range of 0x20 - 0x7E)
     *         are stripped from the String, so the output's {@code length()} may be less than the specified size
     */
    public String getASCIIString(int offset, int size)
    {
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < size; i++)
        {
            int value = getByte(offset + i);
            if (value >= 0x20 && value <= 0x7E) //valid range of characters to include in an ASCII string
            {
                ascii.append((char) value);
            }
        }
        return ascii.toString();
    }

    /**
     * same as {@code getASCIIString()} but it always preserves the specified size of the String, even if the string contains invalid characters
     * @param offset - start offset
     * @param size - total size
     * @return - {@code String} object whose {@code length()} will always return the size specified, even if the string might contain invalid character
     */
    public String getRawASCIIString(int offset, int size)
    {
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < size; i++)
        {
            ascii.append((char)getByte(offset + i));
        }
        return ascii.toString();
    }

    /**
     *  Sets data starting at the specified offset with the characters in the given String
     * @param offset - start byte offset
     * @param value - value to set
     */
    public void setASCIIString(int offset, String value)
    {
        for (int i = 0; i < value.length(); i++)
        {
            data[offset + i] = (byte) value.charAt(i);
        }
    }
    
    /**
     * 
     * @return the data contained in this buffer in a formatted text string 
     */
    public String dumpDataToString()
    {
    	return bufferToString(data, offset, size);
    }
    
    /**
     * @return a text string of the data in the byte array dumped in a grid view
     * where each row is 16 bytes long (0x0 through 0xF) and each byte is printed in hexadecimal.
     * <p>Output will look like the following:
     * <pre>
     *         0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F 
     *        -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- 
     * 0x0X | 0E 1F BA 0E 00 B4 09 CD 21 B8 01 4C CD 21 54 68 
     * 0x1X | 69 73 20 70 72 6F 67 72 61 6D 20 63 61 6E 6E 6F 
     * 0x2X | 74 20 62 65 20 72 75 6E 20 69 6E 20 44 4F 53 20 
     * 0x3X | 6D 6F 64 65 2E 0D 0D 0A 24 00 00 00 00 00 00 00 
     * </pre>
     * @param data - byte array to dump
     */
    public static String bufferToString(byte[] data)
    {
    	return bufferToString(data, 0, data.length);
    }
    
    /**

     * @param data - byte array to dump
     * @param offset - start offset from which to start dumping (will be treated as offset 0 in output)
     * @param size - number of bytes in the array to dump
     * @return a text string of the data in the byte array dumped in a grid view
     * where each row is 16 bytes long (0x0 through 0xF) and each byte is printed in hexadecimal.
     * <p>Output will look like the following:
     * <pre>
     *         0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F 
     *        -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- 
     * 0x0X | 0E 1F BA 0E 00 B4 09 CD 21 B8 01 4C CD 21 54 68 
     * 0x1X | 69 73 20 70 72 6F 67 72 61 6D 20 63 61 6E 6E 6F 
     * 0x2X | 74 20 62 65 20 72 75 6E 20 69 6E 20 44 4F 53 20 
     * 0x3X | 6D 6F 64 65 2E 0D 0D 0A 24 00 00 00 00 00 00 00 
     * </pre>
     */
    public static String bufferToString(byte[] data, int offset, int size)
    {
        validateOffsetAndSize(data, offset, size);
        if (size == 0)
        {
            return "[]";
        }
        StringBuilder message = new StringBuilder();

        // setup initial variables
        int alignmentSize = Math.max(Integer.toHexString(size).length() - 1, 1);
        String FORMAT_STRING = "0x%0" + alignmentSize + "XX | ";
        int initialIndent = String.format(FORMAT_STRING, 0).length();
        int maxLen = Math.min(size, 16);

        // build header
        message.append(indent(initialIndent));
        for (int i = 0; i < maxLen; i++)
        {
            message.append(String.format(" %X ", i));
        }
        message.append("\n").append(indent(initialIndent));
        for (int i = 0; i < maxLen; i++)
        {
            message.append("-- ");
        }

        // fill in actual buffer
        int byteOffset = 0;
        for (int byteIndex = 0; byteIndex < size; byteIndex++)
        {
            if ((byteIndex % 16) == 0)
            {
                message.append(String.format("\n" + FORMAT_STRING, byteOffset));
                byteOffset++;
            }
            message.append(String.format("%02X ", data[offset + byteIndex]));
        }
        return message.toString();
    }

    private static String indent(int numberOfSpaces)
    {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfSpaces; i++)
        {
            result.append(" ");
        }
        return result.toString();
    }
    
    /**
     * Given a number of bits, returns a bit mask that has that number of bits filled
     * @param sizeInBits number of bits to set in the mask
     * @return a value where the indicated number of bits are set at the LSB
     */
    public static long getBitMask(int sizeInBits)
    {
        switch (sizeInBits)
        {
            case 0:
                return 0;
            case 1:
                return 0x1;
            case 2:
                return 0x3;
            case 3:
                return 0x7;
            case 4:
                return 0xF;
            default:
                return getBitMask(sizeInBits - 4) << 4 | getBitMask(4);
        }
    }
    
    /**
     * 
     * @param bytes  number of bytes
     * @return a String indicating the number of bytes in proper English. i.e. "0 bytes", "1 byte", "2 bytes", etc
     */
    public static String bytesToString(int bytes)
    {
        return String.format("%d %s", bytes, (bytes == 1 ? "byte" : "bytes"));
    }

    /**
     *
     * @param bits number of bits
     * @return a String indicating the number of bits in proper English. i.e. "0 bits", "1 bit", "2 bits", etc
     */
    public static String bitsToString(int bits)
    {
        return String.format("%d %s", bits, (bits == 1 ? "bit" : "bits"));
    }

}

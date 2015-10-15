package com.thesonofthom.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to assist with converting between primitives and their Object counterparts when using reflection
 * 
 * @author Kevin Thomson
 */
public class ClassUtils
{
    public final static Map<Class<?>, Class<?>> primitiveToWrapperClassMap = new HashMap<Class<?>, Class<?>>();
    static
    {
        primitiveToWrapperClassMap.put(Boolean.TYPE, Boolean.class);
        primitiveToWrapperClassMap.put(Character.TYPE, Character.class);
        primitiveToWrapperClassMap.put(Byte.TYPE, Byte.class);
        primitiveToWrapperClassMap.put(Short.TYPE, Short.class);
        primitiveToWrapperClassMap.put(Integer.TYPE, Integer.class);
        primitiveToWrapperClassMap.put(Long.TYPE, Long.class);
        primitiveToWrapperClassMap.put(Float.TYPE, Float.class);
        primitiveToWrapperClassMap.put(Double.TYPE, Double.class);
        primitiveToWrapperClassMap.put(Void.TYPE, Void.class);
    }

    /**
     * Static wrapper around the {@code isInstance(Object)} method of {Class}
     * The primitive class ("int", "boolean", "double", etc) require special handling since Java treats int != Integer
     * and it has no built in methods for converting between the two
     * @param classType
     * @param   obj the object to check
     * @return  true if {@code obj} is an instance of the specified classType
     * @see Class
     */
    public static boolean isInstance(Class<?> classType, Object obj)
    {
        if (classType.isPrimitive()) //convert to real class type
        {
            classType = primitiveToWrapperClassMap.get(classType);
        }
        return obj != null && classType.isInstance(obj);
    }
}

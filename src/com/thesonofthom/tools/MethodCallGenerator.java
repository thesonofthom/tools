package com.thesonofthom.tools;

/**
 * This class is used to generate a String that can be displayed to show the user how to 
 * build the method or constructor with specific inputs
 * It currently works with the following types of parameters:
 * <ul>
 * <li>Enum, String, Boolean, Byte, Short, Integer, Long, and the primitives boolean, byte, short, int, and long </li>
 * </ul>
 * 
 * For any parameters that are of a more complex class, you can use the DeclaredObject class,
 * which will cause the method to output the variable name specified. 
 * You may also call the buildObject method to build the DeclaredObject
 * It is up to the caller to generate the necessary strings for the construction of the declared object.
 * 
 * @author Kevin Thomson
 */
public class MethodCallGenerator 
{
	private static final String NULL = "null";
    /**
     * @param className class where the static method is defined
     * @param methodName name of the static method
     * @param methodParameters parameters being passed into the method
     * @return a string showing a call to the specified static method in the specified class
     */
    
    public static String staticMethodToString(Class<?> className, String methodName, Object... methodParameters)
    {
        //calling a static method is really like calling a declared method but with the class name instead of an object name
        return declaredMethodToString(className.getName(), methodName, methodParameters);
    }
    
    /** 
     * @param objectName name of the declared object where the  method is defined
     * @param methodName name of the method
     * @param methodParameters parameters being passed into the method
     * @return a string showing a call to the specified declared method in the specified object
     */
    public static String declaredMethodToString(String objectName, String methodName, Object... methodParameters)
    {
        return String.format("%s.%s(%s);", objectName, methodName, parametersToString(methodParameters));
    }
    
    /**
     *
     * @param className class being constructed
     * @param variableName variable name being assigned to the newly constructed object
     * @param constructorParameters parameters being passed into the constructor 
     * @return a string showing a call to instantiate a new instance of the specified class using the indicated constructor
     */
    public static String constructorToString(Class<?> className, String variableName, Object... constructorParameters)
    {
        return String.format("%s %s = new %s(%s);", 
                className.getName(), variableName, className.getName(), parametersToString(constructorParameters));
    }
    
    private static String parametersToString(Object...parameters)
    {
        String parametersFormatString = "";
        Object[] formatList = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++)
        {
            formatList[i] = getFormattedString(parameters[i]);
            if (i != 0)
            {
                parametersFormatString += ", ";
            }
            parametersFormatString += "%s";
        }
        return String.format(parametersFormatString, formatList);
    }
    

    private static String getFormattedString(Object object)
    {
		if (object instanceof Enum)
        {
            return getFormattedString((Enum<?>) object);
        }
        else if (object instanceof String)
        {
            return getFormattedString((String) object);
        }
        else if (object instanceof Byte)
        {
            return getFormattedString((Byte) object);
        }
        else if (object instanceof Short)
        {
            return getFormattedString((Short) object);
        }
        else if (object instanceof Long)
        {
            return getFormattedString((Long) object);
        }
        else
        {
            return object == null ? NULL : object.toString();
        }
    }


    private static  String getFormattedString(Enum<?> enumValue)
    {
        return (enumValue == null) ? NULL : enumValue.getClass().getName() + "." + enumValue.name();
    }

    private static String getFormattedString(String string)
    {
        return (string == null) ? NULL : "\"" + string + "\"";
    }
    
    private static String getFormattedString(Byte byteValue)
    {
        return ((byteValue == null) ? NULL : "(byte)"+byteValue);
    }
    
    private static String getFormattedString(Short shortValue)
    {
        return ((shortValue == null) ? NULL : "(short)"+shortValue);
    }
    
    private static String getFormattedString(Long longValue)
    {
        return (longValue == null) ? NULL : longValue + "L";
    }
    
    /**
     * Builds a new DeclaredObject instance to be used in this classes methods
     * 
     * @param object object being passed into the MethodCallGenerator
     * @param name variable name assigned to the object
     * @return new DeclaredObject reference
     */
    public static DeclaredObject buildObject(Object object, String name)
    {
    	return new DeclaredObject(object, name);
    }
    
    
    /**
     * Used to differentiate between strings that need quotes around them and objects
     */
    public static class DeclaredObject
    {
        private Object object;
        private String name;
        
        /**
         * Constructor 
         * @param object object being passed into the MethodCallGenerator
         * @param name variable name assigned to the object
         */
        public DeclaredObject(Object object, String name)
        {
            this.object = object;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return object == null ? NULL: name ;
        }
    }
}


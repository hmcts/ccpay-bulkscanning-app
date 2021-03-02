package uk.gov.hmcts.reform.bulkscanning.utils;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExcelGeneratorUtilTest {

    @Test
    public void testUtilityClassWithOneConstructor() {
        Constructor[] constructors = ExcelGeneratorUtil.class.getDeclaredConstructors();
        assertEquals("Utility class should only have one constructor",
                     1, constructors.length);
    }

    @Test
    public  void  testUtilityClassConstructorToBeInaccessible(){
        Constructor[] constructors = ExcelGeneratorUtil.class.getDeclaredConstructors();
        assertFalse("Utility class constructor should be inaccessible",
                    constructors[0].isAccessible());
    }
}

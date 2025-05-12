/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.asmtools.attribute.ConstantValue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.openjdk.asmtools.lib.action.Jcoder;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.transform.ResultChecker;
import org.openjdk.asmtools.lib.transform.TransformLoader;
import org.openjdk.asmtools.lib.transform.pipeline.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
// TODO: The -g option should be added to JDIS. It will allow keeping incorrect references to CP, and after the JASM-to-class step, the class will be loaded with CFE.
public class ConstantValueAttributeTests extends ResultChecker {
    private static final boolean DEBUG = false;

    private static final String PACKAGE_PREFIX = "javasoft.sqe.tests.vm.classfmt.atr";

    @ParameterizedTest(name = "{0} - {2}")
    @ArgumentsSource(ConstantValueAttributeJcodProvider_1.class)
    void testClassJasmClassChaining(String jcodFileName, String fqClassName,
                                    Class<Throwable> expectedException) throws IOException {
        JcodToClassConverter inputConverter = new JcodToClassConverter();

        // Define the pipeline
        Pipeline<Jcod, Clazz> pipeline = new Pipeline<>(inputConverter)
                .addStage(new ClassToJasmConverter())
                .addStage(new JasmToClassConverter())
                .addStage(new ClassToJcodConverter())
                .addStage(new JcodToClassConverter());

        Path jcodFilePath = Path.of(loadJcodFile(jcodFileName));
        Jcod jcodInput = new Jcod(new Pipeline.Status(jcodFilePath), true);

        // Execute the pipeline
        Clazz finalOutput = pipeline.execute(jcodInput);

        byte[] classBytes = finalOutput.record().byteOutput().getOutputs().get(0).getBody();

        // Step 3: Load the transformed class
        TransformLoader transformLoader = new TransformLoader(ConstantValueAttributeTests.class.getClassLoader())
                .setTransformFilter(className -> className.contains(PACKAGE_PREFIX));

        if (expectedException == null) {
            // Positive scenario: Class file should load successfully
            Optional<Object> loadedClass = Optional.empty();
            try {
                loadedClass = transformLoader.loadClassFromBuffer(
                        PACKAGE_PREFIX.concat(".").concat(fqClassName), classBytes, false
                );
            } catch (Exception e) {
                fail(e);
            }
            assertTrue(loadedClass.isPresent(), "Class loading failed: No object returned.");
            assertEquals(Class.class, loadedClass.get().getClass(), "Loaded object is not a Class.");
        } else {
            // Negative scenario: Class file fails to load and throws an expected exception
            assertThrows(expectedException,
                    () -> transformLoader.loadClassFromBuffer(
                            PACKAGE_PREFIX.concat(".").concat(fqClassName), classBytes, false)

            );
        }
    }

    /**
     * The test tries to use 'jdis' tool to compile jasm file that is illegal in content. Generation should fail.
     * <ul>
     *     <li>Setup: Locate the JCod file. Resolves the jcod file path stored in the test repository and compile this using 'jcoder' tool</li>
     *     <li>Step 1: Generate (Class -> Jasm). Generate jasm file from the class file 'jdis' tool</li>
     *     <ul>
     *         <li>Positive scenario: None</li>
     *         <li>Negative scenario: 'jdis' operation fails with Fatal error in file</li>
     *     </ul>
     * </ul>
     *
     * @param jcodFileName - jcod File in the test resources repository
     * @throws IOException - Thrown from the GeneratorAction, CompileAction and temporary file operations.
     */
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(ProviderForToolError_1.class)
    void testJdisError_InvalidAttrLength_01(String jcodFileName) throws IOException {
        final String expectedStageToFail = "classToJasm2";
        var jcodToClass = new JcodToClassConverter("jcodToClass1");
        var classToJasmFails = new ClassToJasmConverter(expectedStageToFail); //Failure is expected here
        var jasmToClass = new JasmToClassConverter("jasmToClass3");
        var classToJcod = new ClassToJcodConverter("classToJcod4");
        // Define the pipeline
        Pipeline<Jcod, Jcod> pipeline = new Pipeline<>(jcodToClass)
                .addStage(classToJasmFails)
                .addStage(jasmToClass)
                .addStage(classToJcod);

        Path jcodFilePath = Path.of(loadJcodFile(jcodFileName));
        Jcod jcodInput = new Jcod(new Pipeline.Status(jcodFilePath), true);

        // Execute the pipeline
        Jcod finalOutput = pipeline.execute(jcodInput);

        // Step 1.1: Verify jdis output
        assertThat(classToJasmFails.record().log().toString(), Matchers.allOf(
                        Matchers.containsString("jdis   - ERROR: ATT_ConstantValue: Invalid attribute length #4"),
                        Matchers.containsString("1 error(s) in the file: bytes/bytes")
                )
        );

        assertThat(finalOutput.record().log().toString(), Matchers.allOf(
                        Matchers.containsString("jdis   - ERROR: ATT_ConstantValue: Invalid attribute length #4"),
                        Matchers.containsString("1 error(s) in the file: bytes/bytes")
                )
        );
    }

    @Disabled
    @Test
        //The field_info structure contains two ConstantValue attributes in the attributes table.
    void testClassJasmClassChainingX() throws IOException {
        String jcodFileName = "atrcvl00301m1n.jcod";
        String fqClassName = "atrcvl003.atrcvl00301m1.atrcvl00301m1n";

        JcodToClassConverter inputConverter = new JcodToClassConverter();
        ClassToJasmConverter warningConverter = new ClassToJasmConverter();
        JasmToClassConverter xx = new JasmToClassConverter();
        // Define the pipeline
        Pipeline<Jcod, Clazz> pipeline = new Pipeline<>(inputConverter)
                .addStage(warningConverter)
                .addStage(new JasmToClassConverter())
                .addStage(new ClassToJcodConverter())
                .addStage(new JcodToClassConverter());

        Path jcodFilePath = Path.of(loadJcodFile(jcodFileName));
        Jcod jcodInput = new Jcod(new Pipeline.Status(jcodFilePath), true);

        // Execute the pipeline
        Clazz finalOutput = pipeline.execute(jcodInput);
        byte[] classBytes = finalOutput.record().byteOutput().getOutputs().get(0).getBody();

        assertThat(warningConverter.record().log().toString(), Matchers.allOf(
                        Matchers.containsString("WARN: There is more than one \"ATT_ConstantValue\" " +
                                "attribute in the attributes table of a \"field_info\" structure. " +
                                "The last one is used")
                )
        );

        // Step 3: Load the transformed class
        TransformLoader transformLoader = new TransformLoader(ConstantValueAttributeTests.class.getClassLoader())
                .setTransformFilter(className -> className.contains(PACKAGE_PREFIX));

        Optional<Object> loadedClass = Optional.empty();
        try {
            loadedClass = transformLoader.loadClassFromBuffer(
                    PACKAGE_PREFIX.concat(".").concat(fqClassName), classBytes, false
            );
        } catch (Exception e) {
            fail(e);
        }
        assertTrue(loadedClass.isPresent(), "Class loading failed: No object returned.");
        assertEquals(Class.class, loadedClass.get().getClass(), "Loaded object is not a Class.");
    }

    /**
     * Locate the JCod file. Resolves the jcod file path stored in the test repository and compile this using 'jcoder' tool
     *
     * @param jcodFileName jcod File in the test resources repository
     * @return Output of the CompileAction
     * @throws IOException CompileAction fails
     */
    private LogAndBinResults produceClassInputFromJcod(String jcodFileName) throws IOException {
        // Step 1: Locate the JCod file
        String jcodFilePath = loadJcodFile(jcodFileName);

        // Step 1.1: Compile JCod -> Class
        LogAndBinResults compileResult = new Jcoder().compile(List.of(jcodFilePath));
        println("Compile JCod to Class", compileResult.log.toString());
        return compileResult;
    }

    /**
     * Loads a JCOD file from the resources' directory.
     *
     * @param jcodFileName The name of the JCOD file.
     * @return The JCOD file content as a byte array.
     */
    private String loadJcodFile(String jcodFileName) {
        String resourceName = String.format("/jcod-files/%s", jcodFileName);
        File resourceDir = new File(Objects.requireNonNull(this.getClass().getResource(resourceName)).getFile()).getParentFile();
        return resourceDir + File.separator + jcodFileName;
    }

    /**
     * Writes the given byte array to a temporary file with the specified file name and extension.
     *
     * @param fileName The prefix for the temporary file name.
     * @param ext      The suffix (extension) for the temporary file.
     * @param buffer   The byte array to write to the file.
     * @return The path to the created temporary file.
     * @throws IOException          If an I/O error occurs.
     * @throws NullPointerException If any of the arguments are null.
     */
    public static Path writeBytesToFile(String fileName, String ext, byte[] buffer) throws IOException {
        // Validate input parameters
        Objects.requireNonNull(fileName, "File name must not be null");
        Objects.requireNonNull(ext, "File extension must not be null");
        Objects.requireNonNull(buffer, "Buffer must not be null");

        // Create a temporary file with the given prefix and suffix
        Path tempFile = Files.createTempFile(fileName, ext);

        // Write the byte array to the file
        Files.write(tempFile, buffer, StandardOpenOption.WRITE);

        return tempFile;
    }

    /**
     * Provides test arguments for the parameterized test.
     */
    static class ConstantValueAttributeJcodProvider_1 implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of("atrcvl00101m10p.jcod", "atrcvl001.atrcvl00101m1.atrcvl00101m10p", null),
                    Arguments.of("atrcvl00101m1t0p.jcod", "atrcvl001.atrcvl00101m1t.atrcvl00101m1t0p", null),
                    Arguments.of("atrcvl00102m10p.jcod", "atrcvl001.atrcvl00102m1.atrcvl00102m10p", null),
                    Arguments.of("atrcvl00102m1t0p.jcod", "atrcvl001.atrcvl00102m1t.atrcvl00102m1t0p", null),
                    // The field_info structure contains two ConstantValue attributes in the attributes table.
                    //TODO:  Warning is thrown (not error)
                    Arguments.of("atrcvl00301m1n.jcod", "atrcvl003.atrcvl00301m1.atrcvl00301m1n", null),
                    Arguments.of("atrcvl00301m1p.jcod", "atrcvl003.atrcvl00301m1.atrcvl00301m1p", null),
                    // TODO: Need a way to compile a .java file for p to access n file
                    // Arguments.of("atrcvl00401m1p.jcod", "atrcvl004.atrcvl00401m1.atrcvl00401m1p", null),
                    // The value of the attribute_name_index item is equal to zero.
                    //TODO:  Warning is thrown (not error)
                    Arguments.of("atrcvl00501m1n.jcod", "atrcvl005.atrcvl00501m1.atrcvl00501m1n", null),
                    Arguments.of("atrcvl00501m1p.jcod", "atrcvl005.atrcvl00501m1.atrcvl00501m1p", null),
                    // The value of the attribute_name_index item is equal to constant_pool_length.
                    //TODO:  Warning is thrown (not error)
                    Arguments.of("atrcvl00502m1n.jcod", "atrcvl005.atrcvl00502m1.atrcvl00502m1n", null),
                    Arguments.of("atrcvl00502m1p.jcod", "atrcvl005.atrcvl00502m1.atrcvl00502m1p", null),
                    // The constant_pool entry at the attribute_name_index is not a CONSTANT_Utf8_info structure.
                    //TODO:  Warning is thrown (not error)
                    Arguments.of("atrcvl00601m1n.jcod", "atrcvl006.atrcvl00601m1.atrcvl00601m1n", null),
                    Arguments.of("atrcvl00601m1p.jcod", "atrcvl006.atrcvl00601m1.atrcvl00601m1p", null),
                    Arguments.of("atrcvl00701m10p.jcod", "atrcvl007.atrcvl00701m1.atrcvl00701m10p", null),
                    Arguments.of("atrcvl00702m10p.jcod", "atrcvl007.atrcvl00702m1.atrcvl00702m10p", null),
                    Arguments.of("atrcvl00702m11p.jcod", "atrcvl007.atrcvl00702m1.atrcvl00702m11p", null),
                    //TODO:  Warning is thrown (not error)
                    Arguments.of("atrcvl00801m1n.jcod", "atrcvl008.atrcvl00801m1.atrcvl00801m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00801m1p.jcod", "atrcvl008.atrcvl00801m1.atrcvl00801m1p", null),
                    Arguments.of("atrcvl00802m1n.jcod", "atrcvl008.atrcvl00802m1.atrcvl00802m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00802m1p.jcod", "atrcvl008.atrcvl00802m1.atrcvl00802m1p", null),
                    Arguments.of("atrcvl00901m1n.jcod", "atrcvl009.atrcvl00901m1.atrcvl00901m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00901m1p.jcod", "atrcvl009.atrcvl00901m1.atrcvl00901m1p", null),
                    Arguments.of("atrcvl00902m1n.jcod", "atrcvl009.atrcvl00902m1.atrcvl00902m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00902m1p.jcod", "atrcvl009.atrcvl00902m1.atrcvl00902m1p", null),
                    Arguments.of("atrcvl00903m1n.jcod", "atrcvl009.atrcvl00903m1.atrcvl00903m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00903m1p.jcod", "atrcvl009.atrcvl00903m1.atrcvl00903m1p", null),
                    Arguments.of("atrcvl00904m1n.jcod", "atrcvl009.atrcvl00904m1.atrcvl00904m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00904m1p.jcod", "atrcvl009.atrcvl00904m1.atrcvl00904m1p", null),
                    Arguments.of("atrcvl00905m1n.jcod", "atrcvl009.atrcvl00905m1.atrcvl00905m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00905m1p.jcod", "atrcvl009.atrcvl00905m1.atrcvl00905m1p", null),
                    Arguments.of("atrcvl00906m1n.jcod", "atrcvl009.atrcvl00906m1.atrcvl00906m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00906m1p.jcod", "atrcvl009.atrcvl00906m1.atrcvl00906m1p", null),
                    Arguments.of("atrcvl00907m1n.jcod", "atrcvl009.atrcvl00907m1.atrcvl00907m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00907m1p.jcod", "atrcvl009.atrcvl00907m1.atrcvl00907m1p", null),
                    Arguments.of("atrcvl00908m1n.jcod", "atrcvl009.atrcvl00908m1.atrcvl00908m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00908m1p.jcod", "atrcvl009.atrcvl00908m1.atrcvl00908m1p", null),
                    Arguments.of("atrcvl00909m1n.jcod", "atrcvl009.atrcvl00909m1.atrcvl00909m1n", ClassFormatError.class),
                    Arguments.of("atrcvl00909m1p.jcod", "atrcvl009.atrcvl00909m1.atrcvl00909m1p", null),
                    Arguments.of("atrcvl01001m1n.jcod", "atrcvl010.atrcvl01001m1.atrcvl01001m1n", ClassFormatError.class),
                    Arguments.of("atrcvl01001m1p.jcod", "atrcvl010.atrcvl01001m1.atrcvl01001m1p", null),
                    Arguments.of("atrcvl01002m10p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m10p", null),
                    Arguments.of("atrcvl01002m110p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m110p", null),
                    Arguments.of("atrcvl01002m111n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m111n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m112n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m112n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m113n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m113n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m114n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m114n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m115p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m115p", null),
                    Arguments.of("atrcvl01002m116n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m116n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m117n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m117n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m118n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m118n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m119n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m119n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m11n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m11n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m120p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m120p", null),
                    Arguments.of("atrcvl01002m121n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m121n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m122n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m122n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m123n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m123n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m124n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m124n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m125n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m125n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m126n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m126n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m127n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m127n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m128n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m128n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m129n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m129n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m12n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m12n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m130p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m130p", null),
                    Arguments.of("atrcvl01002m131n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m131n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m132n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m132n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m133n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m133n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m134n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m134n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m135n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m135n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m136n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m136n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m137n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m137n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m138n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m138n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m139n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m139n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m13n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m13n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m140p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m140p", null),
                    Arguments.of("atrcvl01002m141n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m141n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m142n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m142n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m143n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m143n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m144n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m144n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m145n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m145n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m146n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m146n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m147n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m147n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m148n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m148n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m149n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m149n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m14n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m14n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m150p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m150p", null),
                    Arguments.of("atrcvl01002m151n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m151n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m152n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m152n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m153n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m153n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m154n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m154n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m155n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m155n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m156n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m156n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m157n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m157n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m158n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m158n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m159n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m159n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m15p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m15p", null),
                    Arguments.of("atrcvl01002m160p.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m160p", null),
                    Arguments.of("atrcvl01002m16n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m16n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m17n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m17n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m18n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m18n", ClassFormatError.class),
                    Arguments.of("atrcvl01002m19n.jcod", "atrcvl010.atrcvl01002m1.atrcvl01002m19n", ClassFormatError.class),

                    Arguments.of("atrcod00101m1p.jcod", "atrcod001.atrcod00101m1.atrcod00101m1p", null),
                    // TODO: CFE not found
                    Arguments.of("atrcod00101m1n.jcod", "atrcod001.atrcod00101m1.atrcod00101m1n", null)
            );
        }
    }

    /**
     * Provides test arguments for the parameterized test.
     */
    static class ProviderForToolError_1 implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    // Instead of 2, length of the ConstantValue attribute is set to 4.
                    Arguments.of("atrcvl00701m11n.jcod", "atrcvl007.atrcvl00701m1.atrcvl00701m11n", null),
                    // Length of an attribute with a name like "ConstantValue" is set to 4.
                    Arguments.of("atrcvl00702m12n.jcod", "atrcvl007.atrcvl00702m1.atrcvl00702m12n", ClassFormatError.class)
            );
        }
    }

    /*
    Debug logger
    */
    private void println(String context, String text) {
        if (DEBUG && text != null && !text.isEmpty())
            System.out.printf("[%s] %s%n", context, text);
    }
}

# OpenJDK AsmTools

The AsmTools open source project is used to develop tools for the production of proper and improper Java `.class` files. AsmTools are being opened in order to facilitate a community of Java .class file production for various testing and other OpenJDK development applications.

Tool usage is covered in the <a href="docs/UsingTools.md">Using AsmTools</a> documentation.

AsmTools consist of a set of (Java class file) assembler/disassemblers:

+ **Jasm/Jdis** - an assembler language that provides a Java-like declaration of member signatures, while providing Java VM specification compliant mnemonics for byte-code instructions. Jasm also provides high-level syntax for constructs often found within classfile attributes. Jasm encoded tests are useful for sequencing byte codes in a way that Javac compiled code might not normally sequence byte-codes.

+ **JCod/JDec** - an assembler language that provides byte-code containers of class-file constructs. JCod encoded tests are useful for testing the well-formedness of class-files, as well as creating collections within a class-file construct that might be size-bounded by a normal Java compiler.   JCod can also be used to 'fuzz' class files in a methodical way that respects class-file constructs.

AsmTools are completely reflexive - Java binary (.class) files may be disassembled into textual representations, which in turn can be assembled back to the same binary file.

AsmTools are developed to support the latest class file formats, in lock-step with JDK development.

Other open source Java assembler tools and binary classfile frameworks exist.  They can be used for the purpose of synthesizing classfiles, however:
- they typically are designed to enforce the limits imposed by the VM specification of the class file format.  They are not designed to produce classes that violate those limits.
- other assembler tools may not necessarily follow strict Java mnemonics as defined in the Java VM spec.
- other assembler tools may not stay in lock-step with the current generation of the JDK and VM specifications.
- class file libraries are harder to use for simple manipulations of any given class file.  Typically, one has to create a program in that framework to parse and modify a class for a specific change to a given class.

The AsmTools open source project is part of the  [Code Tools Project](http://openjdk.java.net/projects/code-tools/ "Code Tools Project"). It exists to promote a community that will improve it, further its development, and use it to develop test suites. We encourage you to browse, download, contribute, and get involved.

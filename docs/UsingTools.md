###  Java Assembler Tools (AsmTools) User’s Guide

---

### Using the AsmTools

This chapter describes general principles and techniques for using the AsmTools.

If no command-line options are provided, or they are invalid, the tools display
error messages and usage information. To get the help message, launch AsmTools
without parameters:

```bash
java -jar asmtools.jar
```

The help system describes how to use all the AsmTools components and contains 
the following topics described in this chapter.

-   [Assemblers and Disassemblers](#BADGCIGA)
-   [JASM vs JCOD](#BADGCIGB)
-   [Tool Usage](#BADCEIIF)
    -  [ASMTools (Launcher)](BADCEABC) 
    -  [JASM](#BADEFIIJ)
    -  [JDIS](#BADCBFCE)
    -  [JCODER](#BADIFAIE)
    -  [JDEC](#BADHJAHI)

---

<span id="BADGCIGA"></span>
### Assemblers and Dissassemblers 

Assembly and disassembly are reflexive operations. One tool’s output can be fed
into another to reproduce the same file.

```bash
java -jar asmtools.jar jdec   Foo.class   # produces Foo.jcod
java -jar asmtools.jar jcoder Foo.jcod    # produces Foo.class

java -jar asmtools.jar jdis   Foo.class   # produces Foo.jasm
java -jar asmtools.jar jasm   Foo.jasm    # produces Foo.class
```

For a given `foo.class`, the result of disassembly followed by reassembly is the
same `foo.class`.

---

<span id="BADGCIGB"></span>
### JASM vs JCOD 

Which format to use depends on the task you are trying to do. 
We can describe some generalizations of when you might wish to use the `JASM` format versus the `JCOD` format.

#### JASM 

The biggest difference between the two formats is that `JASM` specifically focuses on representing byte-code instructions in the VM format 
(while providing minimal description of the structure of the rest of the class file).
Generally, `JASM` is more convenient for semantic changes, like change to instruction flow. 

Typical JASM use cases:

-   Producing invalid classes in which two methods have the same signature
-   Producing invalid class references that use illegal types
-   Generating invalid classes with missing or removed instructions
-   Inserting instrumentation or profiling instructions into methods
-   Creating classes in which language keywords are used as identifiers
-   Verifying that two classes produced by different compilers are equivalent

#### JCOD

`JCOD` provides good support for describing the structure of a class file 
(as well as writing incorrect bytes outside of this structure), 
and provides no support for specifying byte-code instructions (simply raw bytes for instructions).
`JCOD` is typically used for VMs to test Well-formedness of class files (e.g. extra or missing bytes), 
boundary issues, constant-pool coherence, constant-pool index coherence, attribute well-formedness, etc.

Typical JCOD use cases:

-   Examining specific parts of a class file, such as:
    -   the constant pool (for dependency analysis)
    -   constant values
    -   inheritance chains (superclasses)
    -   interface implementation and resolution
-   Producing malformed or structurally invalid class files for JVM testing
-   Validating class-file well-formedness rules
-   Testing boundary conditions and structural constraints
-   Verifying attribute structure and consistency
 
---

<span id="BADCEIIF"></span>
### Tool Usage 

AsmTools consists of the following utilities: 

- [jasm](#BADEFIIJ) – Generates class files from `JASM`
- [jdis](#BADCBFCE) – Disassembles class files into `JASM`
- [jcoder](#BADIFAIE) – Generates class files from `JCOD`
- [jdec](#BADHJAHI) – Disassembles class files into `JCOD`

Each utility can be invoked as:

```bash
java -jar asmtools.jar UTILITY [options] File1 ...
```
or
```bash
java -cp asmtools.jar com.sun.asmtools.UTILITY.Main [options] File1 ...
```
Each utility supports own set of options. 

---
<font size=-1>**Note**: <i>See the following sections for the options associated with each tool.</i></font>

---

<span id="BADCEABC"></span>
### ASMTools (Launcher)

The `asmtools.jar` launcher provides a single entry point to run one of the AsmTools utilities (`jasm`, `jdis`, `jcoder`, or `jdec`) 
and to display global help/version information.

**Usage**:
```text
java -jar asmtools.jar <jasm|jdis|jcoder|jdec> <options> <source files>     run jasm, jdis, jcoder, or jdec tool
or: java -jar asmtools.jar -?|-h|-help                                      print Help (this message) and exit
or: java -jar asmtools.jar -version                                         print version information and exit

use -dls switch to return the ancient dual stream logging
```

---

<span id="BADEFIIJ"></span>
### JASM

`JASM` assembles a `.jasm` source file, written according to the [JASM Specification](JASM_SPEC), into a `.class` file 
for use with a Java Virtual Machine.

**Usage**:
```text
java -jar asmtools.jar jasm [options] <jasm source files>|-
```
or  
```text
java -cp asmtools.jar org.openjdk.asmtools.jasm.Main [options] <jasm source files>|-
``` 

**Note**: <i>if `-` is provided, `<stdin>` is used as the input stream.</i>

#### Options:

```text
  -d <directory>                      Specify where to place generated class files, otherwise <stdout>
  -w <directory>                      Specify where to place generated class files, without considering the classpath, otherwise <stdout>
  -nowrite                            Do not write generated class files
  -nowarn                             Do not print warnings
  -strict                             Consider warnings as errors
  -cv <major.minor>                   Set operating class file version if not specified in the source file (by default 45.3)
  -fixcv <major.minor>                Override class file version in source file(s)
  -fixcv <threshold-major.minor>      Update class file version to major.minor if file's version is below the threshold(<major.minor>)
  -t                                  Print debug, trace information
  -v                                  Print additional information
  -version                            Print the jasm version
```
#### Notes:
1. **Class-file generation behavior**
   
    <br>The `-nowrite` option always suppresses generation of the `.class` file.<br>
    Without `-nowrite`, warnings prevent class-file generation only when `-strict` is specified; otherwise, the class file is written.
    <br><br>

2. **Class‑file version selection (`-cv` vs `-fixcv`)**

    <br>In typical usage, a `.jasm` file explicitly specifies the class‑file version in
    its header, for example:
    
    ```text
    public super class Foo version 55:0 {}
    ```
    
    If the version is not specified in the source file, `JASM` defaults to **45.3**.
    
    The `-cv` option provides a *fallback* version and is used *only if the source
    file does not declare a version*. If a version is present in the `.jasm` file,
    it takes precedence over `-cv`.
    
    To force the class‑file version regardless of whether the source declares one,
    use `-fixcv`. This option overrides the version unconditionally.
    
    The `-cv` and `-fixcv` options were added primarily to support batch updates of
    large sets of `.jasm` files. In general, it is preferable to specify the correct
    class‑file version directly in the `.jasm` source.

---

<span id="BADCBFCE"></span>
### JDIS

`JDIS` is a disassembler that accepts a `.class` file specified by filename, translates it into plain-text jasm source, 
and writes the result to standard output or, when `-d <directory>` is specified, to a generated `.jasm` file in the given directory.

**Usage**:
```text
java -jar asmtools.jar jdis [options] <class files>|-
```
or
```text
java -cp asmtools.jar org.openjdk.asmtools.jdis.Main [options] <class files>|-
``` 

**Note**: <i>if `-` is provided, `<stdin>` is used as the input stream.</i>

#### Options:

```text
  -d <directory>        Specify where to place generated class files, otherwise <stdout>
  -w <directory>        Specify where to place generated class files, without considering the classpath, otherwise <stdout>
  -g                    Generate a detailed output format.
  -gg                   Generate a detailed output format. This includes displaying
                        the pair of this_class and super_class.
  -nc                   Don't print comments
  -table                Print specific attributes in a table format resembling the style of the 'javap' command.
  -hx                   Generate floating-point constants in hexadecimal format.
  -pc                   Print instruction offsets when the output is not detailed with the options -g or -gg.
  -sysinfo              Show system info (path, size, date, SHA-256 hash) of class being processed
  -lnt:<numbers,lines,table,all>
                        Print the LineNumberTable attribute in a Code attribute:
                        table   - print the LineNumberTable attribute as a table
                        numbers - print numbers of source lines in inlined comments
                        lines   - print Java source lines if a class file with LineNumberTable attribute and Java source file are in the same folder
                        all     - print both line numbers and Java source lines in inlined comments, and LineNumberTable attribute as a table
                        The '-lnt' without parameters functions the same way as '-lnt:all'
  -lvt:<vars,types,all>
                        Print LocalVariableTable,LocalVariableTypeTable attributes in a Code attribute:
                        vars    - print LocalVariableTable attribute
                        types   - print LocalVariableTypeTable attribute
                        all     - print both LocalVariableTable and LocalVariableTypeTable attributes
                        The '-lvt' without parameters functions the same way as '-lvt:all'
  -drop:<source,classes,all>
                        Discard some attributes or their groups where:
                        source  - SourceFile attribute
                        classes - this_class, super_class pair
                        all     - SourceFile attribute, this_class and super_class pair
                        The '-drop' without parameters functions the same way as '-drop:all'
  -best-effort          Print as much information as possible despite errors; suppresses the -v option.
  -version              Print the program version
  -t                    Print debug, trace information
  -v                    Print additional information
```
#### Notes:
1. **Line Number and Source Line Generation (-lnt option)**

    <br>The `-lnt[:numbers|lines|table|all]` option controls how `LineNumberTable` information is printed.
    Depending on the mode, it can print line numbers as inline comments, include Java source lines above the corresponding instructions, 
    display the LineNumberTable attribute as a table, or combine all of these. 
    Specifying `-lnt` without parameters is equivalent to `-lnt:all`.
    Printing source lines in comments requires both the `LineNumberTable` and `SourceFile` attributes to be present, 
    and the corresponding *Java source file must be located in the current working directory*.

Refer to the [JASM Assembler](JASM_SPEC) documentation for information on the structure of the resultant `.jasm` file.  
 

---

<span id="BADIFAIE"></span>
### JCODER

`JCODER` is a low-level assembler that accepts text conforming to the [Jcoder Specification.](JCODER_SPEC) and 
produces a `.class` file for use by a Java Virtual Machine. 

Its primary purpose is to generate specialized tests for validating JVM implementations.

**Usage**:
```text
java -jar asmtools.jar jcoder [options] <jcod source files>|-
```
or
```text
java -cp asmtools.jar org.openjdk.asmtools.jcoder.Main [options] <jcod source files>|-
``` 

**Note**: <i>if `-` is provided, `<stdin>` is used as the input stream.</i>

#### Options:
```text
  -d <directory>                      Specify where to place generated class files, otherwise <stdout>
  -w <directory>                      Specify where to place generated class files, without considering the classpath, otherwise <stdout>
  -nowrite                            Do not write generated class files
  -ignore                             Ignore non-fatal error(s) that suppress writing class files
  -fixcv <major:minor>                Override class file version in source file(s)
  -fixcv <threshold-major:minor>      Update class file version to major:minor if file's version is below the threshold(<major:minor>)
  -t                                  Print debug, trace information
  -v                                  Print additional information
  -version                            Print the program version
```

---

<span id="BADHJAHI"></span>
### JDEC

`JDEC` is a low-level disassembler that accepts a `.class` file specified by filename, translates it into plain-text `jcov` source,
and writes the result to standard output or, when `-d <directory>` is specified, to a generated `.jcov` file in the given directory.

**Usage**:
```text
java -jar asmtools.jar jdec [options] <class files>|-
```
or
```text
java -cp asmtools.jar org.openjdk.asmtools.jdec.Main [options] <class files>|-
``` 

**Note**: <i>if `-` is provided, `<stdin>` is used as the input stream.</i>

#### Options:

```text
  -d <directory>        Specify where to place generated class files, otherwise <stdout>
  -w <directory>        Specify where to place generated class files, without considering the classpath, otherwise <stdout>
  -g                    Generate a detailed output format
  -v                    Print additional information
  -version              Print the program version
```

Refer to the [Jcoder Low-Level Assembler](JCODER_SPEC) documentation for information on the structure of the resultant `.jcod`
file.

---
*Java Assembler Tools (AsmTools) User’s Guide*

---
Copyright © 2012, 2025, Oracle and/or its affiliates. All rights reserved.

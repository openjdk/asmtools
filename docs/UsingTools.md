<span id="top"></span>

<table dir="ltr"
data-summary="Navigation bar, includes the book title and navigation buttons"
width="100%" data-border="0" data-cellpadding="0" data-cellspacing="0">
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd" data-bgcolor="#cccccc">
<td class="navbartitle" style="text-align: left;" abbr="ChapTitle"><span
id="Z400012b9112"></span>Java Assembler Tools (AsmTools) User's
Guide</td>
<td style="text-align: right;" abbr="NavButtons" data-valign="top"><a
href="index.html"><img src="shared/toc01.gif" title="Table Of Contents"
data-border="0" width="30" height="26" alt="Table Of Contents" /></a><a
href="chapter1.html"><img src="shared/prev01.gif"
title="Previous Chapter" data-border="0" width="30" height="26"
alt="Previous Chapter" /></a><a href="chapter3.html"><img
src="shared/next01.gif" title="Next Chapter" data-border="0" width="30"
height="26" alt="Next Chapter" /></a><a href="ix.html"><img
src="shared/index01.gif" title="Book Index" data-border="0" width="30"
height="26" alt="Book Index" /></a></td>
</tr>
</tbody>
</table>

  
  

<table dir="ltr" data-summary="Chapter Number" abbr="ChapNum"
width="100%" data-border="0">
<tbody>
<tr class="odd">
<td class="ChapNumber" style="text-align: right;"><span
class="ChapNumPrefix">CHAPTER</span>  <span
class="ChapNumNum">2</span><span class="ChapNumSuffix"></span></td>
</tr>
</tbody>
</table>

------------------------------------------------------------------------

# <span id="d0e1017"></span> Using the AsmTools

<span id="d0e1021"></span> This chapter describes general principles and
techniques for using the AsmTools. For detailed information about the
syntax of each component and command line examples, see [Appendix
B](appendix2.html#Z400013211728). If no command-line options are
provided or they are invalid, the tools provide error messages and usage
information. To get the help message, launch AsmTools without any
parameters as follows:

<span id="d0e1026"></span><span class="kbd command">java  
  
-jar asmtools.jar</span>

<span id="d0e1030"></span>The help system describes how to use all of
the AsmTools components and contains the following topics described in
this chapter.

-   <span id="d0e1035"></span> [Assemblers and Disassemblers](#BADGCIGA)

-   <span id="d0e1035"></span>[JASM vs JCOD  
    ](chapter2.html#BADGCIGB)

-   <span id="d0e1039"></span>[Tool Usage  
    ](#BADCEIIF)

    -   <span id="d0e1044"></span>[JASM](#BADEFIIJ)

    -   <span id="d0e1048"></span>[JDIS](#BADCBFCE)

    -   <span id="d0e1052"></span>[JCODER](#BADIFAIE)

    -   <span id="d0e1056"></span>[JDEC](#BADHJAHI)

    -   <span id="d0e1060"></span>[JCDEC](#BADBIGAE)

------------------------------------------------------------------------

<span id="BADGCIGA"></span>

# Assemblers and Dissassemblers 

Assembly and Dissassembly are reflexive operations.  You can feed one
tool into another to achieve the same file.  For example  
  
**java -jar asmtools.jar jdec foo.class \# produces foo.jcod**  
**java -jar asmtools.jar jcod foo.jcod \# produces foo.class**  

For a given class foo.class, the product of dissassembly, and
re-assembly is the same foo.class.  

  

------------------------------------------------------------------------

<span id="BADGCIGB"></span>

# JASM vs. JCod 

Which format to use depends on the task you are trying to do. We can
describe some generalizations of when you might wish to use the JASM
format versus the JCOD format.    

#### JASM 

The biggest difference between the two formats is that JASM specifically
focuses on representing byte-code instructions in the VM format (while
providing minimal description of the structure of  the rest of the class
file).  Generally, JASM is more convenient for semantic changes, like
change to instruction flow.  

#### JCOD

JCOD provides good support for describing the structure of a class file
(as well as writing incorrect bytes outside of this structure), and
provides no support for specifying byte-code instructions (simply raw
bytes for instructions).   JCOD is typically used for VMs to test
Well-formedness of class files (eg extra or missing bytes), boundary
issues, constant-pool coherence, constant-pool index coherence,
attribute well-formedness, etc..  

#### Use Cases

Below are typical cases of usage of both formats:  
  
JASM usages:  
  

-    To obtain an invalid class where two methods have the same
    signature
-    To obtain an invalid class reference where an illegal type is used
-    To obtain an invalid class with missing/removed instructions
-    To insert profiling instructions in methods
-    To obtain a class where a keyword is used as an identifier
-    To check that two classes produced by different compilers are
    equivalent  

  
JCOD usages:  
  

-    To examine specific parts of a classfile  

-   -   eg. constant-pool (for dependency analysis)
    -   constant values
    -   inheritance chains (super classes)
    -   implementation fullfillment (interface resolution)

  

------------------------------------------------------------------------

  
<span id="BADCEIIF"></span>

# Tool Usage 

Asmtools consist of five utilities:  
  

-    jasm - Generates class files from the JASM representation
-    jdis - Represents class file in JASM format
-    jcoder - Generates class files from the JCOD representation
-    jdec - Represents class file in JCOD format
-    jcdec - Represents JavaCard cap and exp files in JCOD format

  
Each utility can be invoked from the command line as shown below:  
  
<span style="font-weight: bold;">$ java -jar asmtools.jar UTILITY
\[options\] File1 ...</span>  
  
or  
  
<span style="font-weight: bold;">$ java -cp asmtools.jar 
com.sun.asmtools.UTILITY.Main \[options\] File1 ...</span>  
  
Each utility supports own set of options  

------------------------------------------------------------------------

<table width="100%" data-cellpadding="2" data-cellspacing="2">
<tbody>
<tr class="odd">
<td data-valign="top"><p><span id="d0e1281"></span><strong>Note
-</strong> See the following sections for the options associated with
each tool.<br />
</p></td>
</tr>
</tbody>
</table>

------------------------------------------------------------------------

  

------------------------------------------------------------------------

<span id="BADEFIIJ"></span>

## JASM

<span id="DDE_LINK"></span><span style="font-style: italic;">jasm</span>
is an assembler that accepts a text file based on the JASM
Specification, and produces a .class file for use with a Java Virtual
Machine.  

#### Usage: 

<span style="font-weight: bold;">$ java -jar asmtools.jar jasm
\[options\] filename.jasm</span>  
  
or  
  
<span style="font-weight: bold;">$ java -cp asmtools.jar
com.sun.asmtools.jasm.Main \[options\] filename.jasm</span>  
  

#### Options:

<span style="font-weight: bold;">-version</span> Print jasm version  
  
<span style="font-weight: bold;">-d destdir </span>Specifies a directory
to place resulting .class files. If a destdir is not provided, the
.class file will be written in the current directory.  
  
<span style="font-weight: bold;">-g </span>Add debug information to
.class file.  
  
<span style="font-weight: bold;">-nowrite</span> Do not write resulting
.class files. This option may be used to verify the integrity of your
source jasm file.  
  
<span style="font-weight: bold;">-strict</span> Consider warnings as
errors.  
  
<span style="font-weight: bold;">-nowarn</span> Do not print warnings.  
  
<span style="font-weight: bold;">-cv major.minor</span> Set the
operating class file version (by default 45.3).

------------------------------------------------------------------------

<table width="100%" data-cellpadding="2" data-cellspacing="2">
<tbody>
<tr class="odd">
<td data-valign="top"><p><span id="d0e1281"></span><strong>Note
-</strong> If the optional class attribute '<span
style="font-style: italic;">version</span>'defines (in source of class)
the class file version then it overrides default class file version set
by <span style="font-style: italic;">-cv</span> option.</p></td>
</tr>
</tbody>
</table>

------------------------------------------------------------------------

<span style="font-weight: bold;"></span>  

#### Description:

To use jasm, specify the filename of the .jasm file you wish to develop
a .class file from. The Jasm Specification contains information relative
to the format of a .jasm file.  

  

------------------------------------------------------------------------

<span id="BADCBFCE"></span>

## JDIS

<span id="DDE_LINK"></span>*jdis* is a disassembler that accepts a
`.class` file, and prints the plain-text translation of `jasm` source
file to the standard output.

#### Usage: 

<span style="font-weight: bold;">$ java -jar asmtools.jar jdis
\[options\] filename.class</span>  
  
or  
  
<span style="font-weight: bold;">$ java -cp asmtools.jar
com.sun.asmtools.jdis.Main \[options\] filename.class</span>  
  

#### Options:

<span style="font-weight: bold;">-version</span> Print jdis
version<span style="font-weight: bold;"></span>  
  
<span style="font-weight: bold;">-g </span>Generate a detailed output
format. Constants from constant pool are printed, and instructions in
methods are preceded with source line numbers (if attribute
LineNumberTable is available) and with bytecode program counters.  
  
<span style="font-weight: bold;">-s1</span> Generate source lines in
comments. Commented lines of the source file, from which given .class
file is obtained, are printed above the corresponding instruction. Both
attributes LineNumberTable and SourceFile must be available. The source
file should be placed in the current working directory.  
  
<span style="font-weight: bold;">-hx</span> Generate floating-point
constants in hexadecimal format.

#### Description:

To use jdis, specify a *filename*`.class` that you wish to
disassemble.  
You may redirect standard output to a *filename* `.jasm` file. Jdis will
disassemble a `.class` file and create a resultant `.jasm` source file.

Refer to the [Jasm
Assembler](../../work/asmtools/asm-tools-4.1.2-build/release/doc/misc/jasmspec.html)
documentation for information on the structure of the resultant `.jasm`
file.  

  

------------------------------------------------------------------------

<span id="BADIFAIE"></span>

## JCoder

<span id="DDE_LINK"></span>*jcoder* is a low-level assembler that
accepts text based on the [Jcoder
Specification.](../../work/asmtools/asm-tools-4.1.2-build/release/doc/misc/jcoderspec.html)
and produces a `.class` file for use with a Java Virtual Machine.
Jcoder's primary use is as a tool for producing specialized tests for
testing a JVM implementation.

#### Usage: 

<span style="font-weight: bold;">$ java -jar asmtools.jar jcoder
\[options\] filename.jcod</span>  
  
or  
  
<span style="font-weight: bold;">$ java -cp asmtools.jar
com.sun.asmtools.jcoder.Main \[options\] filename.jcod</span>  
  

#### Options:

<span style="font-weight: bold;">-version</span> Print jcoder version  
  
<span style="font-weight: bold;">-d destdir </span>Specifies a directory
to place resulting .class files. If a destdir is not provided, the
.class file will be written in the current directory.  
  
<span style="font-weight: bold;">-nowrite</span> Do not write resulting
.class files. This option may be used to verify the integrity of your
source jcoder file.  

#### Description:

To use jcoder, specify the *filename*`.jcod` file you wish to develop a
`.class` file from. The [Jcoder
Specification](../../work/asmtools/asm-tools-4.1.2-build/release/doc/misc/jcoderspec.html)
contains information relative to the format of a `.jcod` file.

  

------------------------------------------------------------------------

<span id="BADHJAHI"></span>

## JDec

<span id="DDE_LINK"></span>*jdec* is a low-level disassembler that
accepts `.class` file and prints a plain text of `jcod` source file to
the standard output.

#### Usage: 

<span style="font-weight: bold;">$ java -jar asmtools.jar jdec
\[options\] filename.class \[&gt; filename.jcod\]</span>  
  
or  
  
<span style="font-weight: bold;">$ java -cp asmtools.jar
com.sun.asmtools.jdec.Main \[options\]
filename.class</span><span style="font-weight: bold;"> \[&gt;
filename.jcod\]</span>  
  

#### Options:

<span style="font-weight: bold;">-version</span> Print jdec
version<span style="font-weight: bold;"></span>  
  
<span style="font-weight: bold;">-g </span>Generate a detailed output
format.  

#### Description:

To use jdec, specify a *`filename.class`* that you wish to
disassemble.  
You may redirect standard output to a *`filename.jcod`* file. *jdec*
will disassemble `.class` file and create a resultant `.jcod` plain
source file.

Refer to the [Jcoder Low-Level
Assembler](../../work/asmtools/asm-tools-4.1.2-build/release/doc/misc/jcoderspec.html)
documentation for information on the structure of the resultant `.jcod`
file.  

  

------------------------------------------------------------------------

<span id="BADBIGAE"></span>

## JCDec

<span id="DDE_LINK"></span>*jcdec* is a low-level disassembler that
accepts `.class` file and prints a plain text of `jcod` source file to
the standard output.

#### Usage: 

<span style="font-weight: bold;">$ java -jar asmtools.jar jcdec
\[options\] filename.exp | filename.cap \[&gt; filename.jcod\]</span>  
  
or  
  
<span style="font-weight: bold;">$ java -cp asmtools.jar
com.sun.asmtools.jcdec.Main \[options\]
</span><span style="font-weight: bold;">filename.exp |
filename.cap</span><span style="font-weight: bold;"> \[&gt;
filename.jcod\]</span>  
  

#### Options:

<span style="font-weight: bold;">-version</span> Print jcdec
version<span style="font-weight: bold;"></span>  
  
<span style="font-weight: bold;">-g </span>Generate a detailed output
format.  

#### Description:

To use jcdec, specify a *`filename.exp`* or *`filename.cap`* that you
wish to disassemble.  
You may redirect standard output to a *`filename.jcod`* file. *jcdec*
will disassemble the file and create a resultant `.jcod` plain source
file.

Refer to the [Jcoder Low-Level
Assembler](../../work/asmtools/asm-tools-4.1.2-build/release/doc/misc/jcoderspec.html)
documentation for information on the structure of the resultant ` .jcod`
file.

  

<table width="100%" data-border="0" data-cellpadding="0"
data-cellspacing="0">
<tbody>
<tr class="odd" data-bgcolor="#cccccc">
<td><p>Java Assembler Tools (AsmTools) User's Guide</p></td>
<td><p>000-0000-00</p></td>
<td data-valign="top"><p><a href="index.html"><img
src="shared/toc01.gif" id="graphics14" data-align="bottom"
data-border="0" width="30" height="26" alt="Table Of Contents" /></a> <a
href="chapter1.html"><img src="shared/prev01.gif" id="graphics15"
data-align="bottom" data-border="0" width="30" height="26"
alt="Previous Chapter" /></a><a href="chapter3.html"><img
src="shared/next01.gif" id="graphics16" data-align="bottom"
data-border="0" width="30" height="26" alt="Next Chapter" /></a><a
href="ix.html"><img src="shared/index01.gif" id="graphics17"
data-align="bottom" data-border="0" width="30" height="26"
alt="Book Index" /></a></p></td>
</tr>
</tbody>
</table>

------------------------------------------------------------------------

Copyright © 2012, 2017, Oracle and/or its affiliates. All rights
reserved.

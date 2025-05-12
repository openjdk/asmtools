
# JASM Syntax

This chapter describes _JASM syntax_, and how to encode class files using this syntax. _Jasm_ is a java
assembler that accepts text in the JASM format and produces a `.class` file for use with a Java Virtual Machine.
_Jasm_'s primary use is as a tool for producing specialized tests for testing a JVM implementation.  

This chapter describes JASM syntax in the following sections:

- [General Class Structure](#general-class-structure)
- [General Module Structure](#general-module-structure)
- [General Source File Structure](#general-source-file-structure)
- [The Constant Pool and Constant Elements](#the-constant-pool-and-constant-elements)
- [Constant Declarations](#constant-declarations)
- [Field Variables](#field-variables)
- [Method Declarations](#method-declarations)
- [Instructions](#instructions)
  - [Pseudo Instructions](#pseudo-instructions)
- [Inner Class Declarations](#inner-class-declarations)
- [Annotation Declarations](#annotation-declarations)
  - [Member Annotations](#member-annotations)
      - [Synopsis](#synopsis)
      - [Examples](#examples)
  - [Type Annotations](#type-annotations)
      - [Synopsis](#synopsis-1)
  - [Parameter Names and Parameter Annotations](#parameter-names-and-parameter-annotations)
      - [Synopsis](#synopsis-2)
      - [Examples](#examples-1)
  - [Default Annotations](#default-annotations)
      - [Synopsis](#synopsis-3)
      - [Examples](#examples-2)
- [Module properties](#module-properties)
  - [Requires](#requires)
  - [Exports](#exports)
  - [Opens](#opens)
  - [Uses](#uses)
  - [Provides](#provides)
- [PicoJava Instructions](#picojava-instructions)

---

## JASM Syntax Overview

JASM syntax comes in two forms: short-form and verbose-form:

1. **Short-form:** Uses Java-style names to refer to items in the constant-pool.
2. **Verbose-form:** Uses constant-pool indexes to explicitly refer to items.
By default, the **JDIS** tool outputs short-form JASM files. To generate verbose-form output, use the `-g` option, like this:

``` shell
java -jar asmtools.jar jdis -g
```

## File Naming in JASM

A JASM file can start with a line specifying the name of the output file. This does not affect the file’s content but determines the file name. The first line can be one of these:

- `file FILENAME;`
- `classfile CLASSNAME;` (adds `.class` to the name).

You can define the destination directory using these options:

- `-d`: Specifies the root directory for the output, following the class package structure.
- `-w`: Specifies the exact directory for the output file.

If neither option is used, JASM outputs to `<stdout>`.

## Class Structure in JASM

A JASM file defines class or interface items as follows:

1. **Optional Package Declaration:**

```java
package package_name;
```

2. **Class or Interface Declaration**:

```java
[CLASS_MODIFIERS] class|interface CLASSNAME [extends SUPERCLASSNAME] { [CLASS_BODY] }
```

- The `CLASSNAME` determines the resulting file name.
- If `this_class` is defined in CLASS_BODY, the name does not affect the file's content but only its name.

If both `file FILENAME;` ( or `classfile CLASSNAME;`) and `class CLASSNAME { [CLASS_BODY] }` are present, the file(classfile) declaration takes priority.

### Example Command and Output

Here is an example of compiling a JASM file:

**Input Command:**

```shell
java -jar asmtools.jar jasm -d . FILE.jasm
```

**JASM File Content:**

```java
class FILENAME.data {
this_class  CLASSNAME;
super_class SUPERCLASSNAME;
}
```

**Output:**

A binary file FILENAME.data is created. Decompiling this file (`java -jar asmtools.jar jdis FILENAME.data`) produces:

```java
super class CLASSNAME extends SUPERCLASSNAME version 45:0 {
}
```

---

## Description formats

|               |                                                                                                                                        |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| TERM1\|TERM2  | TERM1 or TERM2 (not both)                                                                                                              |
| [ TERM ]      | TERM is optional                                                                                                                       |
| TERM...       | TERM repeated 1 or more times                                                                                                          |
| [TERM...]     | TERM repeated 0 or more times                                                                                                          |
| "sequence of" | all the following terms are mandatory, in the order given.                                                                             |
| "set of"      | any of following terms, or none of them, may appear in any order. However, repetitions are not llowed.                                 |
| "list of"     | any of following terms, or none of them, may appear in any sequence. If more than one term appear, they are separated by commas (',')  |

---
## Lexical Structure


# TODO

> ------------------------------------------------------------------------
>
>
> The source text file can be free form (newlines, tabs, and blank
> spaces are equivalent). Additionally, the source may contain standard
> Java and C++ comments.
>
> `STRING`, `NUMBER`, and `IDENT` are treated the same as in the Java
> Language Specification. One difference is that LETTERs include also
> \`/', \`&lt;', \`&gt;', \`(', and \`)' .
>
> _**`STRING`**_:  
> `" [ STRING_CHARACTER... ] "`
>
> _**`NUMBER`**_:  
> `DIGIT...`
>
> _**`IDENT`**_:  
> `LETTER [ LETTER_OR_DIGIT ...]`
>
> _**`ACCESS`**_ (depends on the context): set of  
> `abstract final interface native private protected public static super synchronized transient volatile deprecated synthetic bridge varargs`
>
> Not all access bits make sense for all declarations: for example, the
> "super" and "interface" access flags are applied to classes only.
>
> If an access bit is used improperly, the assembler prints a warning,
> but places the bit in the access set.
>
> Note that `deprecated` and `synthetic` keywords are not translated to
> access flags in the Java sense. For these jasm generates a
> corresponding `Deprecated` or `Synthetic` attributes instead of access
> bits. The `synthetic` access flag is used to mark compiler generated
> members not seen in the source (for example, a field reference to an
> anonymous outer class).
>
> _**`TAG`**_: one of  
> `int float long double Asciz String class Field Method NameAndType InterfaceMethod MethodType MethodHandle InvokeDynamic Dynamic`
>
> Local names represent labels, rangePC-labels and local variables.
> Their scope is constrained by method parenthesis.
>
> _**`LOCAL_NAME`**_:  
> `IDENT`
>
> 
>
> _**`CONSTANT_INDEX`**_:  
> `#NUMBER`
>
> Each CONSTANT\_INDEX represents a reference into the constant pool at
> the specified location.

------------------------------------------------------------------------

<span id="genstruct"></span>

# General Class Structure

_**`INTERFACES`**_:list of  
`CONSTANT_CELL(class|@interface|interface)`

_**`TOP_LEVEL_COMPONENT`**_: one of  
`CONSTANT_DECLARATION FIELD_DECLARATION METHOD_DECLARATION INNER_CLASS_DECLARATIONS`

_**`CLASS`**_: sequence of  
`ANNOTATIONS CLASS_ACCESS CONSTANT_CELL(class|@interface|interface) [extends CONSTANT_CELL(class)] [implements INTERFACES] [version INTEGER:INTEGER] { [TOP_LEVEL_COMPONENT...] }`

<span style="font-weight: bold; font-style: italic;">`CLASS_ACCESS`</span>`: list of`  
`[public]``[final]``[super]``[interface]``[abstract]``[synthetic]``[annotation]``[enum]`

The `extends CONSTANT_CELL(class)` clause places the "super" element of
the class file. The `implements INTERFACES` clause places the table of
interfaces. Since the assembler does not distinguish interfaces and
ordinary classes (the only difference is one access bit), the table of
interfaces of an interface class must be declared with `implements`
keyword, and not `extends`, as in Java language.

**Note:**The last two rules allow `TOP_LEVEL_COMPONENT` to appear in any
order and number. For example, you can split constant pool table into
several parts, mixing constants and method declarations.

------------------------------------------------------------------------

<span id="genmodule"></span>

# General Module Structure

_**`MODULE`**_: sequence of  
`ANNOTATIONS [MODULE_FLAGS] moduleContent CONSTANT_CELL(moduleContent) [version INTEGER:INTEGER] {[TOP_LEVEL_MODULE_PROPERTIES...]}`

_**`TOP_LEVEL_MODULE_PROPERTIES`**_: one of  
`MODULE_REQUIRES` `MODULE_EXPORTS` `MODULE_OPENS` `MODULE_USES`
`MODULE_PROVIDES`

<span style="font-weight: bold; font-style: italic;">`MODULE_FLAGS`</span>`:set of`  
`[open]`

------------------------------------------------------------------------

<span id="source"></span>

# General Source File Structure

_**`PACKAGE_DECLARATION`**_:  
`package IDENT;`

Package declaration can appear only once in source file.

_**`CLASS_FILE`**_: sequence of  
`PACKAGE_DECLARATION CLASS...`

<!-- -->

_**`MODULE_FILE`**_:  
`MODULE...`

<!-- -->

_**`SOURCE_FILE`**_:  
`MODULE_FILE|CLASS_FILE`

------------------------------------------------------------------------

<span id="cp"></span>

# The Constant Pool and Constant Elements

A `CONSTANT_CELL` refers to an element in the constant pool. It may
refer to the element either by its index or its value:

_**`CONSTANT_CELL`**_:  
`CONSTANT_INDEX`  
`TAGGED_CONSTANT_VALUE`

Generic rule for TAGGED\_CONSTANT\_VALUE is:

_**`TAGGED_CONSTANT_VALUE`**_:  
`[TAG] CONSTANT_VALUE`

A TAG may be omitted when the context only allows one kind of a tag. For
example, the argument of an `anewarray` instruction should be a
`CONSTANT_CELL` which represents a class, so instead of

        anewarray class java/lang/Object

one may write:

        anewarray java/lang/Object

It is possible to write another tag, e.g.:

        anewarray String java/lang/Object

However, the resulting program will be incorrect.

Another example of an implicit tag (eg. a context which implies tag) is
the header of a class declaration. You may write:

        aClass {
        }

which is equivalent to:

        class aClass {
        }

Below, the tag implied by context will be included in the rules, e.g.:

        CONSTANT_VALUE(int).

The exact notation of `CONSTANT_VALUE` depends on the (explicit or
implicit) `TAG.`

_**`TAGGED_CONSTANT_VALUE`**_:

> <table data-border="0">
> <tbody>
> <tr class="odd">
> <td><code> int </code></td>
> <td><code> INTEGER </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> long </code></td>
> <td><code> [INTEGER|LONG] </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="odd">
> <td><code> float </code></td>
> <td><code> [FLOAT|INTEGER] </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> float </code></td>
> <td><code> bits INTEGER </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="odd">
> <td><code> double </code></td>
> <td><code> [FLOAT|DOUBLE|INTEGER|LONG] </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> double </code></td>
> <td><code> [bits INTEGER | bits LONG] </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="odd">
> <td><code> Asciz </code></td>
> <td><code> EXTERNAL_NAME </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> class </code></td>
> <td><code> CONSTANT_NAME </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="odd">
> <td><code> String </code></td>
> <td><code> CONSTANT_NAME </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> NameAndType </code></td>
> <td><code> NAME_AND_TYPE </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="odd">
> <td><code> Field </code></td>
> <td><code> CONSTANT_FIELD </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> Method </code></td>
> <td><code> CONSTANT_FIELD </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="odd">
> <td><code> ReferenceIndex </code></td>
> <td><code> [Method|InterfaceMethod]</code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> MethodHandle </code></td>
> <td><code> [INVOKESUBTAG|INVOKESUBTAG_INDEX]</code></td>
> <td><code> :</code></td>
> <td><code> CONSTANT_FIELD | [FIELDREF|METHODREF|INTERFACEMETHODREF] </code></td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="odd">
> <td><code> MethodType </code></td>
> <td colspan="3"><code> CONSTANT_NAME</code></td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><span
> id="invokedynamicconstant"></span><code> InvokeDynamic </code></td>
> <td><code> INVOKESUBTAG</code></td>
> <td><code> :</code></td>
> <td><code> CONSTANT_FIELD</code></td>
> <td><code> :</code></td>
> <td><code> NAME_AND_TYPE</code></td>
> <td><code> [INVOKEDYNAMIC_STATIC_ARGS]</code></td>
> </tr>
> <tr class="odd">
> <td><code> moduleContent </code></td>
> <td><code> CONSTANT_NAME </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> <tr class="even">
> <td><code> package </code></td>
> <td><code> CONSTANT_NAME </code></td>
> <td colspan="2"> </td>
> <td></td>
> <td></td>
> <td></td>
> </tr>
> </tbody>
> </table>

> <u>_Note_</u>  
> When the JASM parser encounters an InvokeDynamic constant, it creates
> an entry in the _BootstrapMethods_ attribute (the _BootstrapMethods_
> attribute is produced if it has not already been created). The entry
> contains a reference to the _MethodHandle_ item in the constant pool,
> and, optionally, a sequence of references to additional static
> arguments (_ldc_-type constants) to the _bootstrap method_

INVOKESUBTAGs for MethodHandle and (const) InvokeDynamic are defined as
follows and can be presented as either an index or a tag:

_**`INVOKESUBTAG:             [INVOKESUBTAG_INDEX]`**_

> <table data-border="0">
> <tbody>
> <tr class="odd">
> <td><code> REF_GETFIELD </code></td>
> <td><code> [1] </code></td>
> </tr>
> <tr class="even">
> <td><code> REF_GETSTATIC </code></td>
> <td><code> [2] </code></td>
> </tr>
> <tr class="odd">
> <td><code> REF_PUTFIELD </code></td>
> <td><code> [3] </code></td>
> </tr>
> <tr class="even">
> <td><code> REF_PUTSTATIC </code></td>
> <td><code> [4] </code></td>
> </tr>
> <tr class="odd">
> <td><code> REF_INVOKEVIRTUAL </code></td>
> <td><code> [5] </code></td>
> </tr>
> <tr class="even">
> <td><code> REF_INVOKESTATIC </code></td>
> <td><code> [6] </code></td>
> </tr>
> <tr class="odd">
> <td><code> REF_INVOKESPECIAL </code></td>
> <td><code> [7] </code></td>
> </tr>
> <tr class="even">
> <td><code> REF_NEWINVOKESPECIAL</code></td>
> <td><code> [8] </code></td>
> </tr>
> <tr class="odd">
> <td><code> REF_INVOKEINTERFACE </code></td>
> <td><code> [9] </code></td>
> </tr>
> </tbody>
> </table>

Static arguments for an InvokeDynamic constant are defined as follows:

_**`INVOKEDYNAMIC_STATIC_ARGUMENTS`**_:  
`INVOKEDYNAMIC_STATIC_ARG ',' ...`

_**`INVOKEDYNAMIC_STATIC_ARG`**_: (one of)  
`INVOKEDYNAMIC_STATIC_ARG_CONSTANT_VALUE`

_**`INVOKEDYNAMIC_STATIC_ARG_CONSTANT_VALUE`**_:

> <table data-border="0">
> <tbody>
> <tr class="odd">
> <td><code> int </code></td>
> <td><code> INTEGER </code></td>
> </tr>
> <tr class="even">
> <td><code> long</code></td>
> <td><code> [INTEGER|LONG] </code></td>
> </tr>
> <tr class="odd">
> <td><code> float </code></td>
> <td><code> [FLOAT|INTEGER] </code></td>
> </tr>
> <tr class="even">
> <td><code> double </code></td>
> <td><code> [FLOAT|DOUBLE|INTEGER|LONG] </code></td>
> </tr>
> <tr class="odd">
> <td><code> class </code></td>
> <td><code> CONSTANT_NAME </code></td>
> </tr>
> <tr class="even">
> <td><code> String </code></td>
> <td><code> CONSTANT_NAME </code></td>
> </tr>
> <tr class="odd">
> <td><code> MethodHandle </code></td>
> <td><code> INVOKESUBTAG:CONSTANT_FIELD </code></td>
> </tr>
> <tr class="even">
> <td><code> MethodType </code></td>
> <td><code> CONSTANT_NAME </code></td>
> </tr>
> </tbody>
> </table>

`INTEGER`, `LONG`, `FLOAT`, and `DOUBLE` correspond to `IntegerLiteral`
and `FloatingPointLiteral` as described in [The Java Language
Specification](https://docs.oracle.com/javase/specs/jls/se8/html/index.html).
If a double-word constant (`LONG` or `DOUBLE`) is represented with a
single-word value (`INTEGER` or `FLOAT`, respectively), single-word
value is simply promoted to double-word, as described in [The Java
Language
Specification](https://docs.oracle.com/javase/specs/jls/se8/html/index.html).
If floating-point constant (`FLOAT` or `DOUBLE`) is represented with an
integral value (`INTEGER` or `LONG`, respectively), the result depends
on whether the integral number is preceded with the keyword "bits". If
"bits" is not used, the result is a floating-point number closest in
value to the decimal number. If the keyword "bits" is used, the
floating-point constant takes bits of the integral value without
conversion.

Thus,

       float 2;
    means the same as 
       float 2.0f;
    and the same as 
       float bits 0x40000000;
    while 
       float bits 2;
    actually means the same as 
       float bits 0x00000002;
    and the same as
       float 2.8026e-45f

_**`CONSTANT_NAME`**_:  
`CONSTANT_INDEX`  
`EXTERNAL_NAME`

_**`EXTERNAL_NAME`**_:  
`IDENT STRING`

External names are names of class, method, field, or type, which stay in
resulting .class file, and may be represented both by `IDENT` or by
`STRING` (which is useful when name contains non-letter characters).

_**`NAME_AND_TYPE`**_:  
`CONSTANT_INDEX`  
`CONSTANT_NAME:CONSTANT_NAME`

In this second example, the first `CONSTANT_NAME` denotes the name of a
field and second denotes its type.

_**`CONSTANT_FIELD`**_:  
`CONSTANT_INDEX`  
`[CONSTANT_NAME.]NAME_AND_TYPE`

In this third example, `CONSTANT_NAME` denotes to the class of a field.
If `CONSTANT_NAME` is omitted, the current class is assumed.

------------------------------------------------------------------------

<span id="constant"></span>

# Constant Declarations

Constant declarations are demonstrated in the examples below:

       const #1=int 1234
           , #2=String "a string"
          , #3=Method get:I
       ;

_**`CONSTANT_DECLARATION`**_:  
`const CONSTANT_DECLARATORS ;`

_**`CONSTANT_DECLARATORS`**_: list of  
`CONSTANT_DECLARATOR`

_**`CONSTANT_DECLARATOR`**_:  
`CONSTANT_INDEX = TAGGED_CONSTANT_VALUE`

------------------------------------------------------------------------

<span id="field"></span>

# Field Variables

_**`FIELD_DECLARATION`**_:  
`ANNOTATIONS ``FIELD_ACCESS Field FIELD_DECLARATORS ;`

_**`FIELD_DECLARATORS`**_: list of  
`FIELD_DECLARATOR`

_**`FIELD_DECLARATOR`**_:  
`EXTERNAL_NAME:CONSTANT_NAME [:SIGNATURE] [ = TAGGED_CONSTANT_VALUE ]`

<span style="font-weight: bold; font-style: italic;">`FIELD_ACCESS`</span>`: list of`  
`[public|private|protected]``[final]``[static]``[volatile]``[transient]``[synthetic]``[enum]`

Example:

       public static Field 
            field1:I = int 1234,
            field2:"Ljava/lang/String;",
            field3:"Ljava/util/List;":"Ljava/util/List<+Ljava/lang/String;>" 
       ;

Access bits (public and static) are applied both to field1 and field2.
The `EXTERNAL_NAME` denotes the name of the field, `CONSTANT_NAME`
denotes its type, `TAGGED_CONSTANT_VALUE` denotes initial value.

------------------------------------------------------------------------

<span id="method"></span>

# Method Declarations

_**`METHOD_DECLARATION`**_: sequence of  
`ANNOTATIONS ``METHOD_ACCESS Method`  
`EXTERNAL_NAME:CONSTANT_NAME`  
`[THROWS]`  
`STACK_SIZE`  
`[LOCAL_VAR_SIZE]`  
`{ INSTRUCTION_STATEMENT...`  
`ANNOTATIONS }`

The `EXTERNAL_NAME` denotes the name of the method, `CONSTANT_NAME`
denotes its type.

<span style="font-weight: bold; font-style: italic;">`METHOD_ACCESS`</span>`: list of`  
`[public|private|protected]``[static][final]``[synthetic]``[bridge]``[varargs]``[native][abstract]``[strict]``[synthetic]`

<!-- -->

_**`THROWS`**_:  
`throws EXCEPTIONS`

_**`EXCEPTIONS`**_: list of  
`CONSTANT_CELL(class)`

The meaning of the `THROWS` clause is the same as in Java Language
Specification - it forms Exceptions attribute of a method. Jasm itself
does not use this attribute in any way.

_**`STACK_SIZE`**_:  
`stack NUMBER`

The `NUMBER` denotes maximum operand stack size of the method.

_**`LOCAL_VAR_SIZE`**_:  
`locals NUMBER`

The `NUMBER` denotes number of local variables of the method. If
omitted, it is calculated by assembler according to the signature of the
method and local variable declarations.

------------------------------------------------------------------------

<span id="instructions"></span>

# Instructions

> <span id="vminstructions"></span>
>
> ## VM Instructions
>
> _**`INSTRUCTION_STATEMENT`**_:  
> `[NUMBER] [LABEL:] INSTRUCTION|PSEUDO_INSTRUCTION ;`
>
> Jasm allows for a `NUMBER` (which is ignored) at the beginning of each
> line. This is allowed in order to remain consistent with the jdis
> disassembler. Jdis puts line numbers in disassembled code that may be
> reassembled using Jasm without any additional modifications.
>
> _**`INSTRUCTION`**_:  
> `OPCODE [ARGUMENTS]`
>
> _**`ARGUMENTS`**_: list of  
> `ARGUMENT`
>
> _**`ARGUMENT`**_:  
> `NUMBER LABEL LOCAL_VARIABLE TRAP_IDENT CONSTANT_CELL SWITCHTABLE TYPE`
>
> _**`LABEL`**_:  
> `NUMBER IDENT`
>
> _**`LOCAL_VARIABLE`**_:  
> `NUMBER IDENT`
>
> _**`TRAP_IDENT`**_:  
> `IDENT`
>
> _**`TYPE`**_:  
> `NUMBER boolean byte char int float long double class`
>
> _**`SWITCHTABLE`**_:  
> `{ [NUMBER:LABEL...] [default:LABEL] }`
>
> SWITCHTABLE example: Java\_text
>
>          switch (x) {
>      case 11:
>     x=1;
>     break;
>      case 12:
>      x=2;
>     break;
>      default:
>      x=3;
>      }
>
>   
>
> will be coded in assembler as follows:
>
>          tableswitch  {
>    11: L24;
>     12: L29;
>     default: L34
>      }
> L24: iconst_1; 
>      istore_1;
>      goto  L36;
> L29: iconst_2 ;
>      istore_1;
>      goto  L36;
> L34: iconst_3; 
>      istore_1;
> L36:    ....
>
>   
>
> OPCODE is any mnemocode from the instruction set. If mnemocode needs
> an ARGUMENT, it cannot be omitted. Moreover, the kind (and number) of
> the argument(s) must match the kind (and number) required by the
> mnemocode:
>
> <table style="width: 847px; height: 651px;" data-border="1">
> <colgroup>
> <col style="width: 50%" />
> <col style="width: 50%" />
> </colgroup>
> <tbody>
> <tr class="odd">
> <td>aload, astore, fload, fstore, iload, istore, lload, lstore, dload,
> dstore, ver, endvar:</td>
> <td><code>LOCAL_VARIABLE</code></td>
> </tr>
> <tr class="even">
> <td>iinc:</td>
> <td><code>LOCAL_VARIABLE, NUMBER</code></td>
> </tr>
> <tr class="odd">
> <td>sipush, bipush, bytecode:</td>
> <td><code>NUMBER</code></td>
> </tr>
> <tr class="even">
> <td>tableswitch, lookupswitch:</td>
> <td><code>SWITCHTABLE</code></td>
> </tr>
> <tr class="odd">
> <td>newarray:</td>
> <td><code>TYPE</code></td>
> </tr>
> <tr class="even">
> <td>jsr, goto, ifeq, ifge, ifgt, ifle, iflt, ifne, if_icmpeq, if_icmpne,
> if_icmpge, if_icmpgt, if_icmple, if_icmplt, if_acmpeq, if_acmpne,
> ifnull, ifnonnull, try, endtry:</td>
> <td><code>LABEL</code></td>
> </tr>
> <tr class="odd">
> <td>jsr_w, goto_w:</td>
> <td><code>LABEL</code></td>
> </tr>
> <tr class="even">
> <td>ldc_w, ldc2_w, ldc:</td>
> <td><code>CONSTANT_CELL</code></td>
> </tr>
> <tr class="odd">
> <td>new, anewarray, instanceof, checkcast,</td>
> <td><code>CONSTANT_CELL(class)</code></td>
> </tr>
> <tr class="even">
> <td style="vertical-align: top">multianewarray</td>
> <td
> style="vertical-align: top"><code>NUMBER, </code><code>CONSTANT_CELL(class)</code></td>
> </tr>
> <tr class="odd">
> <td>putstatic, getstatic, putfield, getfield:</td>
> <td><code>CONSTANT_CELL(Field)</code></td>
> </tr>
> <tr class="even">
> <td>invokevirtual, invokenonvirtual, invokestatic:</td>
> <td><code>CONSTANT_CELL(Method)</code></td>
> </tr>
> <tr class="odd">
> <td>invokeinterface:</td>
> <td><code>NUMBER, CONSTANT_CELL(Method)</code></td>
> </tr>
> <tr class="even">
> <td>invokedynamic:</td>
> <td><code>CONSTANT_CELL(InvokeDynamic)</code></td>
> </tr>
> <tr class="odd">
> <td style="vertical-align: top"> aaload,  aastore,  aconst_null,
> aload_0,  aload_1,  aload_2,  aload_3,  aload_w ,  areturn,
> arraylength,  astore_0,  astore_1,  astore_2,  astore_3,  astore_w,
> athrow,  baload,  bastore,  caload,  castore,  d2f,  d2i,  d2l,  dadd,
> daload,  dastore,  dcmpg,  dcmpl,  dconst_0,  dconst_1,  ddiv,  dead,
> dload_0,  dload_1,  dload_2,  dload_3,  dload_w ,  dmul,  dneg,  drem,
> dreturn,  dstore_0,  dstore_1,  dstore_2,  dstore_3,  dstore_w,  dsub,
> dup,  dup2,  dup2_x1,  dup2_x2,  dup_x1,  dup_x2,  f2d,  f2i,  f2l,
> fadd,  faload,  fastore,  fcmpg,  fcmpl,  fconst_0,  fconst_1,
> fconst_2,  fdiv,  fload_0,  fload_1,  fload_2,  fload_3,  fload_w,
> fmul,  fneg,  frem,  freturn ,  fstore_0,  fstore_1,  fstore_2,
> fstore_3,  fstore_w,  fsub ,  i2b,  i2c,  i2d,  i2f,  i2l,  i2s,  iadd,
> iaload,  iand,  iastore,  iconst_0,  iconst_1,  iconst_2,  iconst_3,
> iconst_4,  iconst_5,  iconst_m1,  idiv,  iinc_w,  iload_0,  iload_1,
> iload_2,  iload_3,  iload_w,  imul,  ineg,  int2byte,  int2char,
> int2short,  ior,  irem,  ireturn,  ishl,  ishr,  istore_0,  istore_1,
> istore_2,  istore_3,  istore_w,  isub,  iushr,  ixor,  l2d,  l2f,  l2i,
> label,  ladd,  laload,  land,  lastore,  lcmp,  lconst_0,  lconst_1,
> ldiv,  lload_0,  lload_1,  lload_2,  lload_3,  lload_w,  lmul,  lneg,
> lor,  lrem,  lreturn,  lshl,  lshr,  lstore_0,  lstore_1,  lstore_2,
> lstore_3,  lstore_w,  lsub,  lushr,  lxor,  monitorenter,  monitorexit,
> nonpriv,  nop,  pop,  pop2,  priv,  ret,  return,  ret_w,  saload,
> sastore,  swap,   wide<br />
> </td>
> <td style="vertical-align: top"><code>&lt;No Arguments&gt; </code></td>
> </tr>
> </tbody>
> </table>
>
> <span id="invokedynamicinstructions"></span>
>
> ## InvokeDynamic Instructions
>
> _InvokeDynamic instructions_ are instructions that allow dynamic
> binding of methods to a call site. These instructions in JASM form are
> rather complex, and the JASM assembler does some of the necessary work
> to create a _BootstrapMethods_ attribute for entries of binding
> methods.
>
> >     class Test
> >       version 51:0
> > {
> >     Method m:"()V"
> >       stack 0 locals 1
> >     {
> >        invokedynamic InvokeDynamic REF_invokeSpecial:bsmName:"()V"   // information about bootstrap method
> >                                                     :methName:"(I)I" // dynamic call-site name ("methName") plus the argument and return types of the call ("(I)I")
> >                                                      int 1, long 2l; // optional sequence of additional static arguments to the bootstrap method (ldc-type constants)
> >     }
> > } // end Class Test
>
> This JASM code has an _invokedynamic_ instruction of the form:
> _**invokedynamic InvokeDynamic (CONSTANT\_CELL(INVOKEDYNAMIC))**_
> where the INVOKEDYNAMIC constant is represented as
> [specified](#invokedynamicconstant)  
> 
> (i.e. _invokedynamic InvokeDynamic INVOKESUBTAG : CONSTANT\_FIELD
> (bootstrapmethod signature) : NAME\_AND\_TYPE (CallSite) \[Arguments
> (Optional)\]_ ).
>
> The JASM assembler creates the appropriate constant entries and
> entries into the BootstrapMethods attribute in a resulting class file.
>
> You can also create InvokeDynamic constants and BootstrapMethods
> explicitly:
>
> >        #22; //class Test3
> >   version 51:0
> > {
> >
> > const #1 = InvokeDynamic   0:#11;  //  REF_invokeSpecial:Test3.bsmName:"()V":name:"(I)I" int 1, long 2l
> > const #2 = Asciz    "Test3";
> > const #3 = long 2l;
> > const #5 = class #6; //  java/lang/Object
> > const #6 = Asciz    "java/lang/Object";
> > const #7 = Asciz "name";
> > const #8 = int   1;
> > const #9 = Asciz  "SourceFile";
> > const #10 = Asciz  "Test3.jasm";
> > const #11 = NameAndType    #7:#21; //  name:"(I)I"
> > const #12 = Asciz    "()V";
> > const #13 = Method    #22.#17;    //  Test3.bsmName:"()V"
> > const #14 = Asciz    "Code";
> > const #15 = Asciz    "m";
> > const #16 = Asciz   "BootstrapMethods";
> > const #17 = NameAndType  #20:#12;    //  bsmName:"()V"
> > const #18 = Asciz  "LineNumberTable";
> > const #19 = MethodHandle  7:#13;  //  REF_invokeSpecial:Test3.bsmName:"()V"
> > const #20 = Asciz  "bsmName";
> > const #21 = Asciz "(I)I";
> > const #22 = class    #2; //  Test3
> > const #23 = class  #6; //  java/lang/Object
> >
> >
> >
> > Method #15:#12
> >   stack 0 locals 1
> > {
> >    0:  invokedynamic InvokeDynamic #1; //  InvokeDynamic REF_invokeSpecial:Test3.bsmName:"()V":name:"(I)I" int 1, long 2l;
> > }
> >
> > BootstrapMethod #19 #8 #3;
> >
> > } // end Class Test3
>
> In this example, `const #1 = InvokeDynamic 0:#11;` is the
> InvokeDynamic constant that refers to BootstrapMethod at index '0' in
> the BootstrapMethods Attribute (`BootstrapMethod #19 #8 #3;` which
> refers to the _MethodHandle_ at const \#19, plus 2 other static args
> (at const \#8 and const \#3).

<span id="pseudoinstructions"></span>

## Pseudo Instructions

> Pseudo instructions are 'assembler directives', and not really
> instructions (in the VM sense) They typically come in two forms:
> Code-generating Pseudo-Instructions, and Attribute-Generating
> Pseudo-Instructions.
>
> <span id="codepseudo"></span>
>
> ### Code-Generating Pseudo-Instructions
>
> The _bytecode_ directive instructs the assembler to put a collection
> of raw bytes into the code attribute of a methodK  
> 
>
> _**`bytecode NUMBERS`**_  
> NUMBERS is list of NUMBERs (divided by COMMA).  
> Insert bytes in place of the instruction. May have any number of
> numeric arguments, each of them to be converted into a byte and
> inserted in method's code.
>
> <span id="atrpseudo"></span>
>
> ### Attribute-Generating Pseudo-Instructions
>
> The rest of pseudo\_instructions do not produce any bytecodes, and are
> used to form tables: local variable table, exception table,  
> Stack Maps, and Stack Map Frames. Line Number Tables can not be
> specified, but they are constructed by the assembler itself.
>
> > #### Local Variable Table Attribute Generation
> >
> > _**`var LOCAL_VARIABLE`**_  
> > Starts local variable range
> >
> > _**`endvar LOCAL_VARIABLE`**_  
> > Ends local variable range. LOCAL\_VARIABLE means name or index of
> > local variable table entry.
> >
> > <u>Example</u>:
> >
> >         static void main (String[] args) {
> >  Tester inst = new Tester();
> >         inst.callSub();
> >     }
> >
> > will be coded in assembler as follows:
> >
> > > > >     static Method #8:#9   // main:"([Ljava/lang/String;)V"
> > > > >    stack 2 locals 2
> > > > > {
> > > > > 4       var 0; // args:"[Ljava/lang/String;"
> > > > >  0:  new #1; //  class Tester;
> > > > >    3:  dup;
> > > > >     4:  invokespecial   #2; //  Method "<init>":"()V";
> > > > >     7:  astore_1;
> > > > > 6       var 1; // inst:"LTester;"
> > > > >     8:  aload_1;
> > > > >     9:  invokevirtual   #3; //  Method callSub:"()V";
> > > > > 7  12: return;
> > > > >         endvar 0, 1;
> > > > >  
> > > > > }
> >
> > #### Exception Table Attribute Generation
> >
> > To generate exception table, three pseudo-instructions are used.
> >
> > > _**`try TRAP_IDENT`**_  
> > > Starts rangePC range
> > >
> > > _**`endtry TRAP_IDENT`**_  
> > > Ends rangePC range
> > >
> > > _**`catch TRAP_IDENT CONSTANT_CELL(class)`**_  
> > > Starts exception handler.
> >
> > `TRAP_IDENT` represents the name or number of an exception table
> > entry. `CONSTANT_CELL` in "catch" pseudo\_instruction means catch
> > type. Each exception table entry contains 4 values:start-pc, end-pc,
> > catch-pc, catch-type. In jasm, each entry is denoted with some
> > (local) identifier, as an example: `TRAP_IDENT`.
> >
> > To set start-pc, place "try TRAP\_IDENT" before the instruction with
> > the desirable program counter. Similarly, use "endtry TRAP\_IDENT"
> > for end-pc and "catch TRAP\_IDENT, catch-type" for catch-pc and
> > catch-type (which is usually a constant pool reference). Try,
> > endtry, and catch pseudoinstructions may be placed in any order. The
> > order of entries in exception table is significant (see JVM
> > specification). However, the only way to control this order is to
> > place catch-clauses in appropriate textual order: assembler adds an
> > entry in the exception table each time it encounters a catch-clause.
> >
> > Example:
> >
> >          try {
> >       try {
> >           throw new Exception("EXC");
> >       } catch (NullPointerException e){
> >           throw e;
> >          } catch (Exception e){
> >          throw e;
> >          }
> >    } catch (Throwable e){
> >       throw e;
> >     }
> >
> > will be coded in assembler as follows:
> >
> > >         try R1, R2; // single "try" or "endtry" can start several regions
> > >         new class java/lang/Exception;
> > >       dup;
> > >         ldc String "EXC";
> > >        invokespecial java/lang/Exception.<init>:"(Ljava/lang/String;)V";
> > >      athrow;
> > >     endtry R1;
> > >     catch R1 java/lang/NullPointerException; // only one "catch" per entry allowed
> > >        astore_1;
> > >        aload_1;
> > >         athrow;
> > >     catch R1 java/lang/Exception; // same region (R1) can appear in different catches
> > >         astore_1;
> > >        aload_1;
> > >         athrow;
> > >     endtry R2;
> > >     catch R2 java/lang/Throwable;
> > >         astore_1;
> > >        aload_1;
> > >         athrow;
> > >
> > >       
> >
> > #### StackMap Table Attribute Generation
> >
> > Stack Maps are denoted by the pseudo-op opcode _stack\_map, and they
> > can be identified by three basic items:_
> >
> > _**`StackMapStatement =`**_``stack\_map
> > _**`(stackMap_Item_MapType |stackMap_Item_Object | stackMap_Item_NewObject)`**_``
> >
> > _**`stackMap_Item_MapType = (`_**bogus | int | float | double | long
> > | null | this | CP**_`)`**_``  
> > 
> > _**`stackMap_Item_Object = CONSTANT_CELL_CLASS`**_  
> > _**`stackMap_Item_NewObject =`**_``at``_**`LABEL`**_``
> >
> > All stack\_map directives are collected by the assembler, and are
> > used to create a StackMap Table attribute.
> >
> > <span style="text-decoration: underline;">Example 1
> > (MapType):</span>  
> >
> >     public Method "<init>":"()V"
> >     stack 1 locals 1
> > {
> >         aload_0;
> >         invokespecial    Method java/lang/Object."<init>":"()V";
> >         return;
> >         stack_frame_type full;
> >         stack_map bogus;
> >         ...
> > }
> >
> > 
> > <u>Example 2 (Object):</u>  
> > 
> > <span style="font-family: monospace;">public Method
> > "&lt;init&gt;":"()V"  
> > stack 2 locals 1  
> > {  
> > ...  
> > ***stack\_map class java/lang/Object;***  
> > nop;  
> > return;  
> > }  
> > </span>  
> > <u>Example 3 (NewObject):</u>  
> > 
> > <span style="font-family: monospace;">public Method
> > "&lt;init&gt;":"()V"  
> > stack 2 locals 1  
> > {  
> > ...  
> > ***stack\_map at L5;***  
> > nop;  
> > return;  
> > }  
> > 
> > </span>  
> >
> > #### StackFrameType Table Attribute Generation
> >
> > StackFrameTypes are similar assembler directives as StackMap. These
> > directives can appear anywhere in the code, and the assembler will
> > collect them to produce a StackFrameType attribute.
> >
> > _**`StackFrameStatement =`**_``stack\_frame\_type``_**`frame_type`**_
> >
> > _**`frame_type = (`**_ same | stack1 | stack1\_ex | chop1 | chop2 |
> > chop3 | same\_ex | append | full _**`)`**_  
> > 
> > <u>Example 1 (full _stack frame type_):</u>  
> >
> >     public Method "<init>":"()V"
> >     stack 1 locals 1
> > {
> >         aload_0;
> >         invokespecial    Method java/lang/Object."<init>":"()V";
> >         return;
> >         stack_frame_type full;
> >         stack_map bogus;
> >         ...
> > }
> >
> > <u>Example 2 (append, chop2, and same _stack frame types_):</u>  
> >
> >     public Method foo:"(Z)V"
> >    stack 2 locals 5
> > {
> >         ...
> >      iload_2;
> >         iconst_2;
> >        if_icmpge   L30;
> >     L27:    stack_frame_type append;
> >      locals_map int, int;
> >         iconst_2;
> >        istore  4;
> >     L30:    stack_frame_type chop2;
> >       goto    L9;
> >     L33:    stack_frame_type same;
> >        getstatic   Field java/lang/System.out:"Ljava/io/PrintStream;";
> >      ldc String "Chop2 attribute test";
> >       invokevirtual   Method java/io/PrintStream.println:"(Ljava/lang/String;)V";
> >      return;
> >         ...
> > }
> >
> > #### LocalsMap Table
> >
> > Locals Maps are typically associated with a _stack\_frame\_type_,
> > and are accumulated per stack frame. They typically follow a
> > _stack\_frame\_type_ directive.
> >
> > _**`LocalsMapStatement =`**_``locals\_map``_**`locals_type (, locals_type )*`**_
> >
> > _**`locals_type = stackMap_Item_MapType | CONSTANT_CELL_CLASS`**_``  
> > 
> > <u>Example (a _locals map_ specifying 2 ints):</u>
> >
> >     public Method foo:"(Z)V"
> >    stack 2 locals 5
> > {
> >         ...
> >      iload_2;
> >         iconst_2;
> >        if_icmpge   L30;
> >     L27:    stack_frame_type append;
> >         locals_map int, int;
> >      iconst_2;
> >        istore  4;
> >   L30:    stack_frame_type chop2;
> >      goto    L9;
> >  L33:    stack_frame_type same;
> >       getstatic   Field java/lang/System.out:"Ljava/io/PrintStream;";
> >      ldc String "Chop2 attribute test";
> >       invokevirtual   Method java/io/PrintStream.println:"(Ljava/lang/String;)V";
> >      return;
> >         ...
> > }

------------------------------------------------------------------------

<span id="innercl"></span>

# Inner Class Declarations

_**`INNER_CLASS_DECLARATIONS`**_: list of  
`INNER_CLASS_DECLARATION`

_**`INNER_CLASS_DECLARATION`**_:  
`INNER_CLASS_ACCESS`**`InnerClass`**`[INNER_CLASS_NAME`**`=`**`]? INNER_CLASS_INFO [`**`of`**`OUTER_CLASS_INFO]? ;`

_**`INNER_CLASS_NAME`**_:  
`IDENT | CPX_name`

_**`INNER_CLASS_INFO`**_:  
`CONSTANT_CELL(class)`

_**`OUTER_CLASS_INFO`**_:  
`CONSTANT_CELL(class)`

_**`INNER_CLASS_ACCESS`**_`: list of`  
`[public|protected|private][static][final][interface][abstract][synthetic][annotation][enum]`

Example:

        InnerClass InCl=class test$InCl of class test;

------------------------------------------------------------------------

<span id="annots"></span>

# Annotation Declarations

<span id="memberannots"></span>

## Member Annotations

Member annotations are a subset of the basic annotations support
provided in JDK 5.0 (1.5). These are annotations that ornament Packages,
Classes, and Members either visibly (accessible at runtime) or invisibly
(not accessible at runtime). In JASM, visible annotations are denoted by
the token **@**, while invisible annotations are denoted by the token
**@-**.

#### <u>Synopsis</u>

_**`ANNOTATIONS`**_:
`[ANNOTATION_DECLARATION]+`;

_**`ANNOTATION_DECLARATION`**_:  
**`@+`**`|`**`@-`**`ANNOTATION_NAME [ANNOTATION_VALUE_DECLARATIONS]`

The '**@+**' token identifies a Runtime Visible Annotation, where the
'**@-' token identifies a Runtime Invisible Annotation.**

_**`ANNOTATION_NAME`**_:  
`IDENT`

_**`ANNOTATION_VALUE_DECLARATIONS`**_: list of (comma separated)
`ANNOTATION_VALUE_DECLARATION`

_**`ANNOTATION_VALUE_DECLARATION`**_:  
`[ANNOTATION_VALUE_IDENT=] [ANNOTATION_VALUE]`

_**`ANNOTATION_VALUE_IDENT`**_:  
`IDENT`

_**`ANNOTATION_VALUE`**_:  
`ANNOTATION_VALUE_PRIMITIVE | Array of ANNOTATION_VALUE_PRIMITIVE`

_**`ANNOTATION_VALUE_PRIMITIVE`**_:  
`PRIMITIVE_TYPE | STRING | CLASS | ENUM | ANNOTATION_DECLARATION`

<!-- -->

_**`CLASS`**_:  
**`class`**`CONSTANT_CELL(class)`

<!-- -->

_**`ENUM`**_:  
**`enum CONSTANT_CELL(class) CONSTANT_CELL(string) (where string is Enum type name)`**

<!-- -->

_**`PRIMITIVE_TYPE`**_:  
`BOOLEAN | BYTE | CHAR | SHORT | INTEGER | LONG | FLOAT | DOUBLE`

_**<u>Note</u>**_  
    Types (Boolean, Byte, Char, and Short) are normalized into Integer's
within the constant pool.  
    Annotation values with these types may be identified with a keyword
in front of an integer value.  
  
     eg.       **boolean** true (or: boolean 1)  
                 **byte** 20  
                 **char** 97  
                 **short** 2130  
  
    Other primitive types are parsed according to normal prefix and
suffix conventions  
    (eg. Double = xxx.x**d**, Float = xxx.x**f**, Long = xxx**L**).
    Strings are identified and delimited by '"' (quotation marks).  
  
   Keywords '**class**' and '**enum**' identify those annotation types
explicitly. Values within classes and enums may  
   either be identifiers (strings) or Constant Pool IDs.  
  
   Annotations specified as the value of an Annotation field are
identified by the JASM annotation keywords '**@+**' and '**@-**'.  
  
   Arrays are delimited by '{' and '}' marks, with individual elements
delimited by ',' (comma).  

#### <u>Examples</u>

<u>Example 1 (Class Annotation, Visible)</u>  

    @+ClassPreamble { 
         author = "John Doe", 
         date = "3/17/2002", 
         currentRevision = 6, 
         lastModified = "4/12/2004", 
         lastModifiedBy = "Jane Doe", 
         reviewers = {
            "Alice",
            "Bob",
            "Cindy"}
    }

    super public class MyClass
       version 50:0
    {
     ...

<span style="text-decoration: underline;">Example 2 (Field Annotation,
Invisible)</span>  
  
    @-FieldPreamble {
         author = "Mustafa", 
         date = "3/17/2009", 
         currentRevision = 4
    }
    Field foo:I;

...  
  
<span style="text-decoration: underline;">Example 3 (Field Annotation,
All subtypes)</span>  
  
    @+FieldPreamble { 
     boolAnnot      = boolean 1,                     // Boolean
      charBear       = char 97,                   // Char
     sharkByte      = byte 17,                   // Byte
     shortCircuit   = short 4386,                    // Short 
       integerHead    = 42,                        // Int 
     longJohnSilver = 55l,                       // Long 
        floatBoat      = 1.0f,                      // Float 
       doubleDip      = 10.0d,                     // Double 
      stringBeans    = "foo",                     // String 
      severity       = enum FieldPreamble$Severity IMPORTANT,     // Enum
     classAnnot     = class FieldPreamble$FooBall,           // Class
        tm = @+Trademark { description = "embedded", owner = "ktl"} // Annotation
    }
    Field foo:I;

...  

<u>Example 4 (Module Annotation, Visible)</u>  

    @+java/lang/Deprecated { 
         since = "9", 
         forRemoval = boolean true 
    }

    moduleContent my.moduleContent
       version 53:0
    {
     ...

Note:  
 JASM does not enforce the annotation value declarations like a compiler
would.  It only checks to see that an annotation structure is
well-formed.  

## Type Annotations

Member annotations are a subset of the basic annotations support
provided in JDK 7.0 (1.7). These are annotations that ornament Packages,
Classes, and Members either visibly (accessible at runtime) or invisibly
(not accessible at runtime). In JASM, visible annotations are denoted by
the token **@T+**, while invisible annotations are denoted by the token
**@T-**.

#### <u>Synopsis</u>

_**`TYPE_ANNOTATION_DECLARATION`**_:

**`@T+`**`|`**`@T-`**`ANNOTATION_NAME [TYPE_ANNOTATION_VALUE_DECLARATIONS]`

_**`TYPE_ANNOTATION_VALUE_DECLARATIONS`**_: list of (comma separated)  

`TYPE_ANNOTATION_VALUE_DECLARATION`

_**`TYPE_ANNOTATION_VALUE_DECLARATION`**_:

**`{`**``**`{`**`ANNOTATION_VALUE_DECLARATION`**<sup>`+`</sup>**``**`}`**` TARGET PATH`` `**`}`**

_**`TARGET`**_:

**`{`**` TARGET_TYPE TARGET_INFO `**`}`**  

****  

_**`TARGET_TYPE`**_`:`

``

<table data-border="0">
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr class="odd">
<td
style="vertical-align: top"><strong><em><code>TARGET_TYPE</code></em></strong><code>:</code></td>
<td
style="vertical-align: top"><strong><em><code>TARGET_INFO_TYPE</code></em></strong><code>:</code></td>
</tr>
<tr class="even">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>CLASS_TYPE_PARAMETER </code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEPARAM</code><em></em></strong></td>
</tr>
<tr class="odd">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;">METHOD_TYPE_PARAMETER
</span><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code> </code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEPARAM</code><em></em></strong></td>
</tr>
<tr class="even">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;">CLASS_EXTENDS</span><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code> </code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>SUPERTYPE</code><em></em></strong></td>
</tr>
<tr class="odd">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>CLASS_TYPE_PARAMETER_BOUND </code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEPARAM_BOUND</code><em></em></strong></td>
</tr>
<tr class="even">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>METHOD_TYPE_PARAMETER_BOUND</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEPARAM_BOUND</code><em></em></strong></td>
</tr>
<tr class="odd">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>FIELD </code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>EMPTY</code></strong></td>
</tr>
<tr class="even">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>METHOD_RETURN </code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>EMPTY</code></strong>|<br />
</td>
</tr>
<tr class="odd">
<td><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>METHOD_RECEIVER</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>EMPTY</code></strong></td>
</tr>
<tr class="even">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>METHOD_FORMAL_PARAMETER</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>METHODPARAM</code><em></em></strong></td>
</tr>
<tr class="odd">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>THROWS</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>EXCEPTION</code><em></em></strong></td>
</tr>
<tr class="even">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>LOCAL_VARIABLE</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><em><code>LOCALVAR</code></em></strong></td>
</tr>
<tr class="odd">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>RESOURCE_VARIABLE</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><em><code>LOCALVAR</code></em></strong></td>
</tr>
<tr class="even">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>EXCEPTION_PARAM</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>CATCH</code></strong></td>
</tr>
<tr class="odd">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>INSTANCEOF</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><em><code>OFFSET</code></em></strong></td>
</tr>
<tr class="even">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>NEW</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><em><code>OFFSET</code></em></strong></td>
</tr>
<tr class="odd">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>CONSTRUCTOR_REFERE CE_RECEIVER</code></span><br />
</td>
<td
style="vertical-align: top; font-style: italic"><strong><em><code>OFFSET</code></em></strong></td>
</tr>
<tr class="even">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>METHOD_REFERENCE_RECEIVER</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><em><code>OFFSET</code></em></strong> </td>
</tr>
<tr class="odd">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>CAST</code></span><br />
</td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEARG</code><em></em></strong></td>
</tr>
<tr class="even">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT</code></span><br />
</td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEARG</code><em></em></strong></td>
</tr>
<tr class="odd">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>METHOD_INVOCATION_TYPE_ARGUMENT</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEARG</code><em></em></strong></td>
</tr>
<tr class="even">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEARG</code><em></em></strong></td>
</tr>
<tr class="odd">
<td style="vertical-align: top"><span
style="font-size: 8pt; font-family: &#39;NimbusMonL-Regu-Extend_850&#39;;"><code>METHOD_REFERENCE_TYPE_ARGUMENT</code></span></td>
<td
style="vertical-align: top; font-style: italic"><strong><code>TYPEARG</code><em></em></strong></td>
</tr>
</tbody>
</table>

_**`TARGET_INFO`**_<span style="font-weight: bold;">`_TYPE`</span>`:`

``

`TYPEPARAM | SUPERTYPE | TYPEPARAM_BOUND | EMPTY | METHODPARAM | EXCEPTION | LOCALVAR | CATCH |`` OFFSET |  TYPEARG`

****  

**`TYPEPARAM`**`:`  

`paramIndex(`_`INTEGER`_`)`` `

**`SUPERTYPE`**`:`  

`typeIndex(`_`INTEGER`_`)``typeIndex(``INTEGER``)`` `

**`TYPEPARAM_BOUND`**`:`  

`paramIndex(`_`INTEGER`_`) ``boundIndex(`_`INTEGER`_`)`` `

**`EMPTY`**`:`

``  

**`METHODPARAM`**`:`  

`index(`

`paramIndex(`_`INTEGER`_`)`

**`EXCEPTION`**`:`  

`typeIndex(`_`INTEGER`_`)`_``_``

****  

_**`LOCALVAR`**_`:`  

<span style="font-weight: bold;">`{`</span>``**`LVENTRY }`<sup>`+numEntries`</sup>**``

``

_**`LVENTRY`**_`:`  

`startpc(`_`INTEGER)`_`length``(`_`INTEGER)`_`index(`_`INTEGER)`<span style="font-weight: bold;"></span>_``

**`CATCH`**`:`  

`catch(`_`INTEGER)`_

``  

_**`OFFSET`**_`:`  

``

`offset(`_`INTEGER)`_

``

**`TYPEARG`**`:`  

`offset(`_`INTEGER`_`) ``typeIndex(`_`INTEGER`_`)`` `

_**`PATH`**_`: list of (space separated)`  
``

**`{`**_``<span style="font-weight: bold;">`PATH_ENTRY`</span>_<sup>**`+`**</sup>``**`}`**

_**`PATH_ENTRY`**_`:`  

``

<span style="font-style: italic;">`{`<span style="font-weight: bold;">`PATH_KIND PATH_INDEX }`</span></span>  

_**`PATH_KIND`**_`:`

``

`ARRAY | INNER_TYPE | WILDCARD | TYPE_ARGUMENT`

**`PATH_INDEX`**`:`  

_`INTEGER`_``

****  

**** <span id="paramannots"></span>

## Parameter Names and Parameter Annotations

Parameter annotations are another subset of the basic annotations
support provided in JDK 5.0 (1.5). These are annotations that ornament
Parameters to methods either visibly (accessible at runtime) or
invisibly (not accessible at runtime). In JASM, visible parameter
annotations are denoted by the token **@+**, while invisible parameter
annotations are denoted by the token **@-**.  

Parameter names come from an attribute introduced in JDK 8.0 (1.8).
These are fixed parameter names that are used to ornament parameters on
methods.  In Jasm, parameter names are identified by the token \#
followed by { } braclets  

#### <u>Synopsis</u>

_**`METHOD DECLARATION`**_:
`MODIFIERS Method METHOD_NAME:"METHOD_SIGNATURE" [STACK_DECL] [LOCALS_DECL] [PARAMETERS_DECL] {[CODE]}`

_**`PARAMETERS_DECL`**_:
`[PARAMETER_DECL]`<sup>`N`</sup>`(where N < number of params in method, each N is a unique param number)`

_**`PARAMETER_DECL`**_:
`PARAM_NUM : [PARAM_NAME_DECL] [ANNOTATION_DECLARATIONS]`

<!-- -->

_**`PARAM_NAME_DECL`**_:
`#{ name PARAM_ACCESS}`

_**`PARAM_ACCESS`**_`: list of`  
`[final][synthetic][mandated]`

#### Examples

<u>Example 1 (Parameter Annotation)</u>  
_**<u>Java Code</u>**_  

    public class MyClass2 {
        
     ...

        public int doSomething(
     @VisParamPreamble ( author = "gummy" )  @InVisParamPreamble ( author = "bears" )  int barber,
       boolean of, 
            @VisParamPreamble ( author = "sour" )  @InVisParamPreamble ( author = "worms" )    int seville,
            @InVisParamPreamble1 ( reviewers = {"Dilbert", "Garfield"} ) boolean pastrami) { 
             ...
        }
     ...
    }

_**<u>JASM Code</u>**_  
  
<span style="text-decoration: underline;">Note</span>:  The first two
parameters are named ('P0'- 'P3').  Since this is a compiler controlled
option, there is no way to specify parameter naming in Java source.  

    super public class MyClass2
        version 50:0
    {
     ...

      public Method doSomething:"(IZIZ)I"
      stack 2 locals 5

        0: #{P0 mandated} @+VisParamPreamble { author = "gummy" } @-InVisParamPreamble { author = "bears" } 
        1: #{P1 final synthetic mandated}
            2: #{P2 mandated} @+VisParamPreamble { author = "sour" } @-InVisParamPreamble { author = "worms" } 
       3: #{P3 mandated} @-InVisParamPreamble1 { reviewers = {  "Dilbert",  "Garfield"} } 
       {
     ...
       }

    } // end Class MyClass2

<span id="paramannots"></span>

## Default Annotations

  Default annotations are another subset of the basic annotations
support provided in JDK 5.0 (1.5). These are annotations that ornament
Annotations either visibly (accessible at runtime) or invisibly (not
accessible at runtime). Default annotations specify a default value for
a given annotation field.  

#### <u>Synopsis</u>

_**`ANNOTATION INTERFACE DECLARATION`**_:
`@interface ANNOTATION_NAME { ANNOTATION_FIELD_DECL`<sup>`+`</sup>`}`  

_**`ANNOTATION_FIELD_DECL`**_:
`ANNOT_FIELD_TYPE ANNOTATION_NAME [ANNOTATION_DEFAULT_VALUE_DECL];`  

_**`ANNOTATION_DEFAULT_VALUE_DECL`**_:
`default ANNOTATION_VALUE (where value must be of the type ANNOT_FIELD_TYPE)`  

#### <u>Examples</u>

<u>Example 1 (Default Annotation)</u>  
_**<u>Java Code</u>**_  

    import java.lang.annotation.*; 
    @Retention(RetentionPolicy.RUNTIME)

    @interface Meth2Preamble {
       String author() default "John Steinbeck";
    }

_**<u>JASM Code</u>**_  

    interface  Meth2Preamble
        implements java/lang/annotation/Annotation
        version 50:0

    {
       public abstract Method author:"()Ljava/lang/String;" default { "John Steinbeck" } ;
    } // end Class Meth2Preamble

------------------------------------------------------------------------

<span id="module_properties"></span>

# Module properties

<span id="module_requires"></span>

## Requires

_**`MODULE_REQUIRES`**_: sequence of  
requires `REQUIRES_FLAGS` `CONSTANT_CELL(moduleContent)`;

_**`REQUIRES_FLAGS`**_: set of  
\[`transitive`\] \[`static`\]

Example:

            requires transitive static foo.bar;

<span id="module_exports"></span>

## Exports

_**`MODULE_EXPORTS`**_: sequence of  
exports `CONSTANT_CELL(package)` \[to `EXPORT_TO_MODULES`\];

_**`EXPORT_TO_MODULES`**_: list of  
`CONSTANT_CELL(moduleContent)`

Example:

            exports com/foo/bar to 
                foo2.bar2,
                foo3.bar3;

            exports org/foo/bar;

<span id="module_opens"></span>

## Opens

_**`MODULE_OPENS`**_: sequence of  
opens `CONSTANT_CELL(package)` \[to `OPENS_TO_MODULES`\];

_**`OPENS_TO_MODULES`**_: list of  
`CONSTANT_CELL(moduleContent)`

Example:

            opens com/foo/bar to 
                foo2.bar2,
                foo3.bar3;

            opens org/foo/bar;

<span id="module_uses"></span>

## Uses

_**`MODULE_USES`**_: sequence of  
uses `CONSTANT_CELL(class)`;

Example:

            uses com/foo/bar;

<span id="module_provides"></span>

## Provides

_**`MODULE_PROVIDES`**_: sequence of  
provides `CONSTANT_CELL(class)` \[with `MODULE_PROVIDES_WITH_CLASSES`\];

_**`MODULE_PROVIDES_WITH_CLASSES`**_: list of  
`CONSTANT_CELL(class)`

Example:

            provides com/foo/bar with 
                foo2.bar2,
                foo3.bar3;

            provides com/foo/bar;

------------------------------------------------------------------------

<span id="pico"></span>

# PicoJava Instructions

These instructions takes 2 bytes: prefix (254 for non-privileged variant
and 255 for privileged) and the opcode itself. These instructions can be
coded in assembler in 2 ways: as single mnemocode identical to the
description or using "priv" and "nonpriv" instructions followed with an
integer representing the opcode.

<table data-border="0" data-cellpadding="0" data-cellspacing="0"
width="100%">
<tbody>
<tr class="odd" data-bgcolor="#cccccc">
<td><p>Java Assembler Tools (AsmTools) User's Guide</p></td>
<td><p>000-0000-00</p></td>
<td data-valign="top"><p><a href="index.html"><img
src="shared/toc01.gif" id="graphics5" data-align="bottom"
data-border="0" width="30" height="26" alt="Table Of Contents" /></a> <a
href="chapter3.html"><img src="shared/prev01.gif" id="graphics6"
data-align="bottom" data-border="0" width="30" height="26"
alt="Previous Chapter" /></a><a href="appendix2.html"><img
src="shared/next01.gif" id="graphics7" data-align="bottom"
data-border="0" width="30" height="26" alt="Next Chapter" /></a><a
href="ix.html"><img src="shared/index01.gif" id="graphics8"
data-align="bottom" data-border="0" width="30" height="26"
alt="Book Index" /></a></p></td>
</tr>
</tbody>
</table>

------------------------------------------------------------------------

Copyright © 2012, 2017, Oracle and/or its affiliates. All rights
reserved.

# Prev
A compiler for a toy programming language "Prev" for the [MMIX architecture](https://mmix.cs.hm.edu/).

Implemented as part of the 2014/15 Compilers course at the Faculty of Computer and Information Science, University of Ljubljana, Slovenia.

## Dependencies

The compiler is implemented in Java. A working Java development is required for building.

It produces `.mms` assembly files. To be able to run the programs the MMIX assembler and simulator are required. They can be obtained at https://mmix.cs.hm.edu. In the scripts described below we assume that the MMIX tools are available in the `mmix/` subdirectory.

## Building the compiler

The compiler is implemented in Java in the `prev/` subdirectory.

Run `01_build_compiler.sh` to build it.

## Using the compiler

There are some sample Prev programs in the tests directory.

You can run the `02_compile_tests.sh` to compile them to `.mms` assembly files.

Then use `03_assemble_tests.sh` to asseble the object files with the MMIX assembler.

The resulting programs can be executed in the MMIX simulator. For example:
```
  ./mmix/mmix tests/gcd.mmo
```

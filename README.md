# FakeMake
Author: Anton Surkis

FakeMake is a simplified build system that imitates Make.
FakeMake uses a single config file: `fake.yaml` in the working directory.
Example of `fake.yaml`:
```yaml
default:
  dependencies:
    - run

run:
  dependencies:
    - build
    - main
  run: ./main

compile:
  dependencies:
    - main.c
  target: main.o
  run: gcc -c main.c -o main.o

build:
  dependencies:
    - compile
  target: main
  run: gcc main.o -o main

clean:
  run: rm -rf *.o main
```

Example of running FakeMake:
```
[workdir]$ ./fake clean run
Running command: rm -rf *.o main
Running command: gcc -c main.c -o main.o
Running command: gcc main.o -o main
Running command: ./main
Hello, world!
```
Sections are satisfied in order in which they are listed,
so `fake run clean` would build and run the program and then delete all build files.
The `default` section would be run if it's present if no other section is specified.

`dependencies` are a list of files and other sections.
First all dependency sections are satisfied and dependency files are checked against the `target` file.
If at least one dependency section updated, the `target` file is missing,
or a dependency file was modified after the `target` file,
the `run` command is run and the section is considered updated
(i.e. all sections dependent on it would need an update as well).
If a loop is detected then the execution halts and a non-zero exit code is returned.

`dependencies`, `target` and `run` are all arbitrary:
- with no or empty `dependencies` the section will update unconditionally;
- with no `target` the section will update unconditionally;
- with no `run` the section is considered satisfied as soon as all its dependencies are satisfied,
  except if some of these dependencies are missing files &mdash;
  in that case the section would not be satisfied.
  The section would still be considered "updated" as normal.

No section command would be run more than once in one run of FakeMake.

## Source code
The entire program is small enough to fit in one
[source file](src/main/kotlin/ru/itmo/asurkis/test/fakemake/Main.kt)
a bit over 100 lines of code.

Unit tests can be found in a
[separate file](src/test/kotlin/ru/itmo/asurkis/test/fakemake/tests/Tests.kt).

For CI testing I used [GitHub Actions](https://github.com/asurkis/FakeMake/actions)
(the result of the last run should be
visible on the right of the last commit message
in GitHub interface).

The main idea is to read the entire config file into a single `Map`
of immutable sections, and pass this `Map` (by reference)
into a depth-first search by sections.

For reading YAML I used [kaml](https://github.com/charleskorn/kaml)
and [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization/).

#!/bin/bash

for i in tests/*.prev
do
    echo "Compiling $i"

    java -cp ./prev/bin compiler.Main $i

    echo
done

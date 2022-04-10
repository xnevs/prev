#!/bin/bash

for i in tests/*.mms
do
    echo "Assembling $i"

    ./mmix/mmixal "$i"

    echo
done

#!/bin/bash

find prev/src/ -iname '*.java' | xargs javac -d prev/bin

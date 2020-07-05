#!/bin/bash

rm *.add
rm -r ADDS

for i in $(seq 0 $2)
do
    java -jar reanaE.jar --all-configurations --uml-model=$1/bm$1$i.xml --feature-model=$1/fm$1$i.txt
done

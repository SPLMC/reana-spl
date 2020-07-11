#!/bin/bash

rm -r AnalysisLog
rm *.add
rm -r ADDS

mkdir AnalysisLog

for i in $(seq 0 $2)
do
    java -Xms1024m -Xmx15360m -jar reanaE.jar --all-configurations --uml-model=$1/bm$1$i.xml --feature-model=$1/fm$1$i.txt >> AnalysisLog/evolution$i.out
done

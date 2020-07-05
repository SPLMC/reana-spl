#!/bin/bash

rm -r AnalysisLog
rm *.add
rm -r ADDS

mkdir AnalysisLog

for i in $(seq 0 $1)
do
    java -Xms1024m -Xmx15360m -jar reanaE.jar --uml-model=bmBSN$i.xml --feature-model=fmBSN$i.txt >> AnalysisLog/evolution$i.out
done

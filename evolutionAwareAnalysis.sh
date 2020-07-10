#!/bin/bash

# Default values
xms=1024m
xmx=15360m
spl=${spl:-BSN}
initial_evolution=0
final_evolution=5
iterations=3

# Usage mode ./evolutionAwareAnalysis <<spl>> <<initial_evolution>> <<final_evolution>> <<iterations>> <<persist>> <<xms>> <<xmx>>
# Declare parameters and assign their values.

while [ $# -gt 0 ]; do 
	if [[ $1 == *"--"* ]]; then
		param="${1/--/}"
		declare $param="$2"
	fi

	shift
done



# 1st step: perform the analysis of the original spl
echo "1st step - analysis of the original spl"
rm -r AnalysisLog*
rm *.add
rm -r ADDS

mkdir AnalysisLog

java -Xms$xms -Xmx$xmx -jar reanaEOrdering.jar --all-configurations --uml-model=$spl/bm$spl$initial_evolution.xml --feature-model=$spl/fm$spl$initial_evolution.txt


# 2nd step: perform the analysis of the evolutions

for i in $(seq 1 $iterations ); do 
	for e in $(seq $(expr $initial_evolution + 1) $final_evolution); do
		mkdir AnalysisLog$i
		echo ----------   Iteration $i     Evolution $e   ---------- 
		java -Xms$xms -Xmx$xmx -jar reanaEOrdering.jar --all-configurations --uml-model=$spl/bm$spl$e.xml -feature-model=$spl/fm$spl$e.txt >> AnalysisLog$i/evolution$e.out
	done
done

# 3rd step: recover all analyses and total times of all evolutions
for e in $(seq $(expr $initial_evolution + 1) $final_evolution); do
	echo ---------- Evolution $e ---------- >> AnalysisLog/analysisTime.out
	echo ---------- Evolution $e ---------- >> AnalysisLog/totalTime.out
	for i in $(seq 1 $iterations ); do
		cat AnalysisLog$i/evolution$e.out | grep analysis | awk '{print $4}' >> AnalysisLog/analysisTime.out	
		cat AnalysisLog$i/evolution$e.out | grep total | awk '{print $4}' >> AnalysisLog/totalTime.out	
	done
done

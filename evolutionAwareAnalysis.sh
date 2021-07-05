#!/bin/bash

# Runtime configuration
LIBS="./libs/*"
CLASSES="./bin/"  # Change this to the path where your IDE puts compiled classes
CLASSPATH=$CLASSES:$LIBS

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

COMMAND="CLASSPATH=$CLASSPATH java -Xms$xms -Xmx$xmx ui.CommandLineInterface"
# Path were we dump and read ADDs for previous analyses
PERSISTED_ANALYSES_PATH=Analyses/$spl
# Path were we save analysis stats
LOGS_DIR=$PERSISTED_ANALYSES_PATH/logs

# 1st step: perform the analysis of the original spl
echo "1st step - analysis of the original spl"
rm -rf $PERSISTED_ANALYSES_PATH
# TODO: we have to parameterize the variableStore's location
rm variableStore.add

mkdir -p $PERSISTED_ANALYSES_PATH
mkdir -p $LOGS_DIR

eval "$COMMAND --all-configurations --uml-model=$spl/bm$spl$initial_evolution.xml --feature-model=$spl/fm$spl$initial_evolution.txt --persisted-analyses=$PERSISTED_ANALYSES_PATH"


# 2nd step: perform the analysis of the evolutions

for i in $(seq 1 $iterations ); do
	for e in $(seq $(expr $initial_evolution + 1) $final_evolution); do
		mkdir -p $LOGS_DIR/$i
		echo ----------   Iteration $i     Evolution $e   ----------
		eval "$COMMAND --all-configurations --uml-model=$spl/bm$spl$e.xml --feature-model=$spl/fm$spl$e.txt --persisted-analyses=$PERSISTED_ANALYSES_PATH >> $LOGS_DIR/$i/evolution$e.out"
	done
done

# 3rd step: recover all analyses and total times of all evolutions
for e in $(seq $(expr $initial_evolution + 1) $final_evolution); do
	echo ---------- Evolution $e ---------- >> $LOGS_DIR/analysisTime.out
	echo ---------- Evolution $e ---------- >> $LOGS_DIR/totalTime.out
	for i in $(seq 1 $iterations ); do
		cat $LOGS_DIR/$i/evolution$e.out | grep "Total analysis" | awk '{print $4}' >> $LOGS_DIR/analysisTime.out
		cat $LOGS_DIR/$i/evolution$e.out | grep "Total running" | awk '{print $4}' >> $LOGS_DIR/totalTime.out
	done
done

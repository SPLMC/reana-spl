#!/bin/bash

# Runtime configuration
LIBS="libs/*"
CLASSES="bin/"  # Change this to the path where your IDE puts compiled classes
CLASSPATH=$CLASSES:$LIBS

# Default values
xms=1024m
xmx=15360m
spl=${spl:-BSN}
initial_evolution=0
final_evolution=5
iterations=1

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
PERSISTED_ANALYSES_PATH=Verification/$spl
# Path were we save analysis stats
LOGS_DIR=$PERSISTED_ANALYSES_PATH/logs
RESULTS_DIR=$PERSISTED_ANALYSES_PATH/results
CONFIGURATIONS_DIR=$PERSISTED_ANALYSES_PATH/configurations
# results separator in log files
separator="========================================="

# 1st step: perform the analysis of the original spl
echo "1st step - analysis of the original spl"
rm -rf "$PERSISTED_ANALYSES_PATH"
# TODO: we have to parameterize the variableStore's location
rm variableStore.add

mkdir -p "$PERSISTED_ANALYSES_PATH"
mkdir -p "$LOGS_DIR"
mkdir -p "$RESULTS_DIR"
mkdir -p "$CONFIGURATIONS_DIR"

eval "$COMMAND --all-configurations                                \
               --uml-model=$spl/bm$spl$initial_evolution.xml       \
               --feature-model=$spl/fm$spl$initial_evolution.txt   \
               --persisted-analyses=$PERSISTED_ANALYSES_PATH       \
               >> $LOGS_DIR/evolution$initial_evolution.out"

# Extract the results of the initial model

awk "/$separator/{flag=1;next}/$separator/{flag=0;next}flag" "$LOGS_DIR/evolution$initial_evolution.out" | head -n -2 >> "$RESULTS_DIR/evolution$initial_evolution.out"

# Extract the configurations of the initial model

CONFIGURATIONS=configurations$initial_evolution.txt
grep -Po "(?<=\[).*?(?=\])" "$RESULTS_DIR/evolution$initial_evolution.out" | tr -d " \t\r" >> "$CONFIGURATIONS_DIR/$CONFIGURATIONS"

# generate configuration with first optional feature

cat "$CONFIGURATIONS_DIR/$CONFIGURATIONS" >> "$CONFIGURATIONS_DIR/configurations$((initial_evolution+1)).txt"
sed "s/$/,o_$((initial_evolution+1))/" "$CONFIGURATIONS_DIR/$CONFIGURATIONS" >> "$CONFIGURATIONS_DIR/configurations$((initial_evolution+1)).txt"

# 2nd step: perform the analysis of the evolutions

for e in $(seq $((initial_evolution + 1)) $final_evolution); do
  CONFIGURATIONS=configurations$e.txt
	echo "---------- Evolution $e ----------"
	eval "$COMMAND --configurations-file=$CONFIGURATIONS_DIR/$CONFIGURATIONS  \
                 --uml-model=$spl/bm$spl$e.xml                              \
                 --feature-model=$spl/fm$spl$e.txt                          \
                 --persisted-analyses=$PERSISTED_ANALYSES_PATH              \
                 >> $LOGS_DIR/evolution$e.out"
  # extract results
  awk "/$separator/{flag=1;next}/$separator/{flag=0;next}flag" "$LOGS_DIR/evolution$e.out" | head -n -2 >> "$RESULTS_DIR/evolution$e.out"

  # extract configurations and add new features for next evolution
  CONFIGURATIONS=configurations$((e+1)).txt
  NEW_CONFIGURATIONS=configurations$((e+1)).txt.new
  grep -Po "(?<=\[).*?(?=\])" "$RESULTS_DIR/evolution$e.out" | tr -d " \t\r" > "/tmp/$CONFIGURATIONS"
  sed "s/$/,o_$((e+1))/" "/tmp/$CONFIGURATIONS" > "/tmp/$NEW_CONFIGURATIONS"
  cat "/tmp/$CONFIGURATIONS" "/tmp/$NEW_CONFIGURATIONS" >> "$CONFIGURATIONS_DIR/$CONFIGURATIONS"

done

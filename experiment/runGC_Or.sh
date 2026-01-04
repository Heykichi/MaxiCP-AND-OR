#!/usr/bin/env bash
cd "$(dirname "$0")"
set -u
set -o pipefail

TIMEOUT_SECONDS=300
MAIN_CLASS="org.maxicp.modeling.gc_dimacs.GraphColoring_Or"

usage() {
    echo "Usage: $0"
    echo "  - process GC instances and write to results_GC_Cut.csv"
}
cd ../
CSV_FILE="resultsGC_Or.csv"

echo "Results file : $CSV_FILE"
echo "instance,choice,fail,solutions,completed,exec_time_ms,status" > "experiment/$CSV_FILE"

extract_num() {
  printf '%s\n' "$1" | awk -v k="$2" '
    {
      if (match($0, k "[[:space:]]*:[[:space:]]*[0-9]+")) {
        s = substr($0, RSTART, RLENGTH)
        gsub(/[^0-9]/, "", s)
        print s
        exit
      }
    }
  '
}

process_root() {
    local ROOT_DIR="$1"

    echo "=== Processing root directory: $ROOT_DIR ==="

    while IFS= read -r instance; do

        echo "==> Processing $instance"
        inst_name=$(basename "$instance")

        tmp_out="tmp_output_$$.txt"
        timeout "${TIMEOUT_SECONDS}s" mvn -q exec:java \
            -Dexec.mainClass="$MAIN_CLASS" \
            -Dexec.args="$instance" \
            -Dexec.jvmArgs="-Xms1g -Xmx4g" > "$tmp_out" 2>&1

        exit_code=$?
        output=$(<"$tmp_out")
        rm -f "$tmp_out"

        if [ $exit_code -eq 124 ]; then
            status="TIMEOUT"
        elif [ $exit_code -ne 0 ]; then
            status="ERROR_$exit_code"
        else
            status="OK"
        fi

        choice=$(extract_num "$output" "#choice")
        fail=$(extract_num "$output" "#fail")
        sols=$(extract_num "$output" "#sols")
        completed=$(printf '%s\n' "$output" | grep 'completed' | awk '{print $3}' || echo "")
        exec_ms=$(extract_num "$output" "Execution time")

        echo "$inst_name,$choice,$fail,$sols,$completed,$exec_ms,$status" >> "experiment/$CSV_FILE"

    done < <(find "$ROOT_DIR" -type f -name '*.col' | sort)
}

process_root "graph_coloring/instance"

echo "Done. Results written to $CSV_FILE"

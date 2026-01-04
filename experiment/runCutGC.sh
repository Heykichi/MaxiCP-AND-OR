#!/usr/bin/env bash
cd "$(dirname "$0")"
set -u
set -o pipefail

TIMEOUT_SECONDS=300
MAIN_CLASS="org.maxicp.modeling.gc_dimacs.GraphColoringTestCut"

usage() {
    echo "Usage: $0"
    echo "  - process GC instances and write to results_GC_Cut.csv"
}
cd ../
CSV_FILE="results_GC_Cut.csv"

echo "Results file : $CSV_FILE"
echo "instance,var,cut,subset,largest" > "experiment/$CSV_FILE"

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

    echo "==== Processing root directory: $ROOT_DIR ===="

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

        var=$(extract_num "$output" "var")
        cut=$(extract_num "$output" "cut")
        subset=$(extract_num "$output" "subset")
        largest=$(extract_num "$output" "largest")

        echo "$inst_name,$var,$cut,$subset,$largest" >> "experiment/$CSV_FILE"

    done < <(find "$ROOT_DIR" -type f -name '*.col' | sort)
}

process_root "graph_coloring/instance"


echo "Done. Results written to $CSV_FILE"

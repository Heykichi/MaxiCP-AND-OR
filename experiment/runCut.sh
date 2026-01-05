#!/usr/bin/env bash
cd "$(dirname "$0")"
set -u
set -o pipefail

TIMEOUT_SECONDS=300
MAIN_CLASS="org.maxicp.modeling.xcsp3.XCSP3TestCut"

usage() {
    echo "Usage: $0 [<root_directory_with_instances>]"
    echo "  - No argument : process minicsp23, minicsp24, minicsp25 and write to results.csv"
    echo "  - With argument : process only the given directory and write to <directory>_results.csv"
}

cd ../
ROOT_DIRS=()
CSV_FILE=""

if [ $# -eq 0 ]; then
    ROOT_DIRS=(minicsp23 minicsp24 minicsp25)
    CSV_FILE="results_Cut.csv"
else
    if [ $# -gt 1 ]; then
        usage
        exit 1
    fi
    ROOT_DIRS=("$1")
    CSV_FILE="${ROOT_DIRS[0]}_results_Cut.csv"
fi

VALID_ROOTS=()
for dir in "${ROOT_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        VALID_ROOTS+=("$dir")
    else
        echo "Warning: directory '$dir' not found, skipping."
    fi
done

if [ ${#VALID_ROOTS[@]} -eq 0 ]; then
    echo "Error: none of the specified root directories exist."
    exit 1
fi

echo "Results file : $CSV_FILE"
printf "Processing root directories:"
for d in "${VALID_ROOTS[@]}"; do
    printf " %s" "$d"
done
printf "\n"

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

    done < <(find "$ROOT_DIR" -type f -name '*.xml' | sort)
}

# Process all valid root directories
for root in "${VALID_ROOTS[@]}"; do
    process_root "$root"
done

echo "Done. Results written to $CSV_FILE"

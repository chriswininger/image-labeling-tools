#!/usr/bin/env bash
#
# Runs the test 5 times and reports pass/fail ratio.
#
# Provide the test name for example test_a_middle_aged_man_having_a_beer
#

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

TEST_CLASS="com.wininger.cli_image_labeler.image.tagging.services.ImageInfoServiceTest"
TEST_METHOD=$1
RUNS=5

pass=0
fail=0

for i in $(seq 1 "$RUNS"); do
    echo "========================================="
    echo "  Run $i of $RUNS"
    echo "========================================="

    ./gradlew cleanTest test --tests "${TEST_CLASS}.${TEST_METHOD}" 2>&1

    if [ $? -eq 0 ]; then
        echo ">>> Run $i: PASS"
        ((pass++))
    else
        echo ">>> Run $i: FAIL"
        ((fail++))
    fi

    echo ""
done

echo "========================================="
echo "  Results: $pass passed / $fail failed out of $RUNS runs"
echo "  Pass ratio: $pass/$RUNS"
echo "========================================="

#!/bin/bash
# A look run: the golem pictured in the real game beside a zombie and a player, for judging how it looks.
# Runs the golems self-test from a run folder with a golems/look.properties above it (GolemsLookTest; the usual tests stand aside), in
# the harness's hidden clients, then copies the pictures to local/run/golems-look/ and makes contact sheets of them there.
# The file sits above this run's folder only, so other self-test runs stay ordinary ones.
cd "$(dirname "$0")/../../.." || exit 1
OUT=local/run/golems-look
mkdir -p $OUT/golems
touch $OUT/golems/look.properties
if [ -f tools/gwl.sh ]; then
	bash tools/gwl.sh $OUT/gradle.log :harness:librarySelftest -Plibrary.mods=golems -Plibrary.runDir=$OUT/run
else
	./gw :harness:librarySelftest -Plibrary.mods=golems -Plibrary.runDir=$OUT/run --console=plain > $OUT/gradle.log 2>&1
fi
rc=$?
cp $OUT/run/selftest/single-player/shots/golems/look-*.png $OUT/ 2>/dev/null || { echo "no pictures: see $OUT/gradle.log"; exit 1; }
python library/golems/tools/look_sheets.py $OUT
grep -E "FAIL|BUILD|=== selftest:" $OUT/gradle.log | cut -c1-300 | head -20
ls $OUT/*.png
exit $rc

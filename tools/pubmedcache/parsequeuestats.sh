#!/bin/sh
# Parses the queue stats log
# 2018-07-02 20:38:11.973 DEBUG 23063 --- [ask-scheduler-6] de.dfki.nlp.SiaPubmedAnnotator           : Message counts - input queue: 2184 output queue: 0
regex="([0-9]+-[0-9]+-[0-9]+ [0-9]+:[0-9]+:[0-9]+).*input queue: ([0-9]+) output queue: *([0-9]+)"
echo "Elapsed\tInputQueue\tOutputQueue"
while read line
do
  # check if line matches
  if [[ $line =~ $regex ]]; then
    datestring="${BASH_REMATCH[1]}"
    epoch=`date -j -f "%Y-%m-%d %H:%M:%S" "$datestring" "+%s"`

    baseline="${baseline:-$epoch}"

    input="${BASH_REMATCH[2]}"
    output="${BASH_REMATCH[3]}"

    elapsed=`expr $epoch - $baseline`

    echo "$elapsed\t$input\t$output"

  fi
done < "${1:-/dev/stdin}"
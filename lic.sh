#!/bin/bash


for i in $(find . -name '*.java');
do
  if ! grep -q Copyright $i
  then
    cat lic.txt $i >$i.new && mv $i.new $i
  fi
done

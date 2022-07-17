#!/bin/sh
RUNID=$((`ps | head -5|grep "java com/Client"| awk '{print $1}'`))
kill $RUNID
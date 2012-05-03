#!/bin/sh

dicomfile="$*"

PIXELMEDDIR=.

java -Xmx1g -cp "${PIXELMEDDIR}/pixelmed.jar:${PIXELMEDDIR}/lib/additional/commons-codec-1.3.jar" com.pixelmed.displaywave.ECGViewer "${dicomfile}"

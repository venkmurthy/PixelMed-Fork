#
# Note that PATHTOROOT must have been specified prior to including this file
#

PATHTOHOME = /Users/dclunie

JUNITJAR = ${PATHTOROOT}/lib/junit/junit-4.8.1.jar

ADDITIONALJARDIRINROOT = lib/additional

PATHTODCTOOLSUPPORTFROMROOT = ../../dctool.support

PATHTODCTOOLSFROMROOT = ../../dicom3tools

PATHTOSCPECGSAMPLESFROMROOT = ../../../Documents/Medical/stuff/ECG/OpenECG

PATHTOTESTFILESFROMROOT = ./testpaths

PATHTOTESTRESULTSFROMROOT = ./testresults

PATHTOADDITIONAL = ${PATHTOROOT}/${ADDITIONALJARDIRINROOT}

PATHTODCTOOLSUPPORT = ${PATHTOROOT}/${PATHTODCTOOLSUPPORTFROMROOT}

PATHTODCTOOLS = ${PATHTOROOT}/${PATHTODCTOOLSFROMROOT}

# excalibur not needed for compile, but useful for execution if available ...
# commons-codec not needed for compile, but useful for execution if available ...
DICOMADDITIONALJARS = ${PATHTOADDITIONAL}/excalibur-bzip2-1.0.jar:${PATHTOADDITIONAL}/commons-codec-1.3.jar:${PATHTOADDITIONAL}/vecmath1.2-1.14.jar

DISPLAYADDITIONALJARS = ${DICOMADDITIONALJARS}:${PATHTOADDITIONAL}/jai_imageio.jar:${PATHTOADDITIONAL}/clibwrapper_jiio.jar

DATABASEADDITIONALJARS = ${PATHTOADDITIONAL}/hsqldb.jar

FTPADDITIONALJARS = ${PATHTOADDITIONAL}/commons-net-ftp-2.0.jar

VIEWERADDITIONALJARS = ${DISPLAYADDITIONALJARS}:${DATABASEADDITIONALJARS}:${PATHTOADDITIONAL}/jmdns.jar

SERVERADDITIONALJARS = ${VIEWERADDITIONALJARS}

.SUFFIXES:	.class .java .ico .png

#JAVACOPTIONS = -O -target 1.5 -encoding "UTF8" -Xlint:deprecation -Xlint:unchecked
JAVACOPTIONS = -O -target 1.5 -encoding "UTF8" -Xlint:deprecation
#JAVACOPTIONS = -g -O -target 1.5 -encoding "UTF8" -Xlint:deprecation

.java.class:
	javac ${JAVACOPTIONS} -classpath ${PATHTOROOT} -sourcepath ${PATHTOROOT} $<

.png.ico:
	# http://www.winterdrache.de/freeware/png2ico/
	png2ico $@ $<

clean:
	rm -f *~ *.class core *.bak test.*


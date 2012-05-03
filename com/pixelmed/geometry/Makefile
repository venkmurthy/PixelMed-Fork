PATHTOROOT = ../../..

OBJS = \
	GeometryOfSlice.class \
	GeometryOfVolume.class \
	LocalizerPoster.class \
	LocalizerPosterFactory.class \
	ProjectSlice.class \
	IntersectSlice.class \
	IntersectVolume.class
	
all:	${OBJS}

include ${PATHTOROOT}/Makefile.common.mk

.SUFFIXES:	.class .java

.java.class:
	javac ${JAVACOPTIONS} -classpath ${PATHTOROOT}:${PATHTOADDITIONAL}/vecmath1.2-1.14.jar \
		-sourcepath ${PATHTOROOT} $<

archive:
	tar -cvf - Makefile *.java | gzip -best > ../../../geometry.`date '+%Y%m%d'`.tar.gz


OBJS = \
	ConvertAmicasJPEG2000FilesetToDicom.class

all:	${OBJS}

PATHTOROOT = ../../..

include ${PATHTOROOT}/Makefile.common.mk

testamicas:	ConvertAmicasJPEG2000FilesetToDicom.class
	mkdir -p /tmp/todicomMR1
	java -cp ${PATHTOROOT}:${PATHTOADDITIONAL}/jai_imageio.jar com.pixelmed.convert.ConvertAmicasJPEG2000FilesetToDicom "${PATHTOHOME}/Pictures/Medical/AmicasJ2K/ELMR20030206" /tmp/todicomMR1
	mkdir -p /tmp/todicomCT1
	java -cp ${PATHTOROOT}:${PATHTOADDITIONAL}/jai_imageio.jar com.pixelmed.convert.ConvertAmicasJPEG2000FilesetToDicom "${PATHTOHOME}/Pictures/Medical/AmicasJ2K/CD0105153529" /tmp/todicomCT1
	mkdir -p /tmp/todicomCT2
	java -cp ${PATHTOROOT}:${PATHTOADDITIONAL}/jai_imageio.jar com.pixelmed.convert.ConvertAmicasJPEG2000FilesetToDicom "${PATHTOHOME}/Pictures/Medical/AmicasJ2K/CD0114154520" /tmp/todicomCT2

testamicasdialog:	ConvertAmicasJPEG2000FilesetToDicom.class
	java -cp ${PATHTOROOT}:${PATHTOADDITIONAL}/jai_imageio.jar com.pixelmed.convert.ConvertAmicasJPEG2000FilesetToDicom

PATHTOROOT = ../../..

OBJS = \
	DicomInstanceValidator.class \
	DicomIODDescriptionsCompiled.xsl \
	DicomSRValidator.class \
	DicomSRDescriptionsCompiled.xsl

all:	${OBJS}

include ${PATHTOROOT}/Makefile.common.mk

.java.class:
	javac ${JAVACOPTIONS} -classpath ${PATHTOROOT} \
		-sourcepath ${PATHTOROOT} $<

clean:
	rm -f *~ *.class core *.bak ${OBJS} DicomIODDescriptionsCompiled.xsl DicomSRDescriptionsCompiled.xsl

archive:
	tar -cvf - Makefile *.java *.xml *.xsl | gzip -best > ../../../validate.`date '+%Y%m%d'`.tar.gz

DicomIODDescriptionsCompiled.xsl:	DicomIODDescriptionsSource.xml CompileDicomIODDescriptionsIntoXSLT.class ExecuteTranslet.class
	java \
		-Xms512m -Xmx512m \
		-cp ${PATHTOROOT} \
		com.pixelmed.validate.ExecuteTranslet CompileDicomIODDescriptionsIntoXSLT.xsl DicomIODDescriptionsSource.xml DicomIODDescriptionsCompiled.xsl
	#rm ExecuteTranslet.class

CompileDicomIODDescriptionsIntoXSLT.class:	CompileDicomIODDescriptionsIntoXSLT.xsl CompileXSLTIntoTranslet.class
	java \
		-Xms512m -Xmx512m \
		-cp ${PATHTOROOT} \
		com.pixelmed.validate.CompileXSLTIntoTranslet CompileDicomIODDescriptionsIntoXSLT.xsl
	#rm CompileXSLTIntoTranslet.class

testvalidate:	DicomInstanceValidator.class DicomIODDescriptionsCompiled.xsl
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomInstanceValidator \
		${PATHTOHOME}/work/nemamfmr/DISCIMG/IMAGES/CARANGIO \
		DicomIODDescriptionsCompiled.xsl

DicomSRDescriptionsCompiled.xsl:	DicomSRDescriptionsSource.xml CompileDicomSRDescriptionsIntoXSLT.class ExecuteTranslet.class
	java \
		-Xms512m -Xmx512m \
		-cp ${PATHTOROOT} \
		com.pixelmed.validate.ExecuteTranslet CompileDicomSRDescriptionsIntoXSLT.xsl DicomSRDescriptionsSource.xml DicomSRDescriptionsCompiled.xsl
	#rm ExecuteTranslet.class

CompileDicomSRDescriptionsIntoXSLT.class:	CompileDicomSRDescriptionsIntoXSLT.xsl DicomContextGroupsSource.xml CompileXSLTIntoTranslet.class
	java \
		-Xms512m -Xmx512m \
		-cp ${PATHTOROOT} \
		com.pixelmed.validate.CompileXSLTIntoTranslet CompileDicomSRDescriptionsIntoXSLT.xsl
	#rm CompileXSLTIntoTranslet.class

testvalidatesr:	DicomSRValidator.class DicomSRDescriptionsCompiled.xsl
	echo "Validate original MammoCADSR ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 ${PATHTOHOME}/work/dctool.support/images/dicom/sr/mammocad/12946
	echo "Copy MammoCADSR into BasicTextSR SOP Class and validate ..."
	dccp ${PATHTOHOME}/work/dctool.support/images/dicom/sr/mammocad/12946 /tmp/crap \
		-nodisclaimer -r SOPClassUID "1.2.840.10008.5.1.4.1.1.88.11"
	java -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 /tmp/crap
	echo "Copy MammoCADSR into EnhancedSR SOP Class and validate ..."
	dccp ${PATHTOHOME}/work/dctool.support/images/dicom/sr/mammocad/12946 /tmp/crap \
		-nodisclaimer -r SOPClassUID "1.2.840.10008.5.1.4.1.1.88.22"
	java -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 /tmp/crap
	echo "Copy MammoCADSR into ComprehensiveSR SOP Class and validate ..."
	dccp ${PATHTOHOME}/work/dctool.support/images/dicom/sr/mammocad/12946 /tmp/crap \
		-nodisclaimer -r SOPClassUID "1.2.840.10008.5.1.4.1.1.88.33"
	java -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 /tmp/crap
	echo "Validate ChestCADSR masquerading as Comprehensive ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 ${PATHTOHOME}/work/dctool.support/images/dicom/sr/mammocad/132214_11508_0000000008.sr
	echo "Copy ChestCADSR masquerading as Comprehensive into ChestCADSR SOP Class and validate ..."
	dccp ${PATHTOHOME}/work/dctool.support/images/dicom/sr/mammocad/132214_11508_0000000008.sr /tmp/crap \
		-nodisclaimer -r SOPClassUID "1.2.840.10008.5.1.4.1.1.88.65"
	java -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 /tmp/crap
	echo "Validate DoseSR masquerading as Enhanced ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 ${PATHTOHOME}/Pictures/Medical/DoseSR/getestctinenhsr.dcm
	echo "Copy DoseSR masquerading as Comprehensive into ChestCADSR SOP Class and validate ..."
	dccp  ${PATHTOHOME}/Pictures/Medical/DoseSR/getestctinenhsr.dcm /tmp/crap \
		-nodisclaimer -r SOPClassUID "1.2.840.10008.5.1.4.1.1.88.67"
	java -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		 /tmp/crap
	rm -f /tmp/crap

testvalidateko:	DicomSRValidator.class DicomSRDescriptionsCompiled.xsl
	echo "Validate original KOS ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		${PATHTOHOME}/Pictures/Medical/SRExamples/IHEPDI2005/GE/PAT00000/ST000000/SE000004/KO000000

testvalidateko2:	DicomSRValidator.class DicomSRDescriptionsCompiled.xsl
	echo "Validate original KOS ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		${PATHTOHOME}/Pictures/Medical/KOFromPDI2004/STD3KO1

testvalidatetcemanifest1:	DicomSRValidator.class DicomSRDescriptionsCompiled.xsl
	echo "Validate TCE manfiest KOS ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		${PATHTOHOME}/Pictures/Medical/TCE/KO.1.2.276.0.7230010.3.1.4.6446.1145570765726.32791.4.dcm

testvalidatetcemanifest2:	DicomSRValidator.class DicomSRDescriptionsCompiled.xsl
	echo "Validate TCE manfiest KOS ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		${PATHTOHOME}/Pictures/Medical/TCE/testKOS13.dcm

testvalidatetceaddinfo:	DicomSRValidator.class DicomSRDescriptionsCompiled.xsl
	echo "Validate TCE manfiest KOS ..."
	java  -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.validate.DicomSRValidator \
		${PATHTOHOME}/Pictures/Medical/TCE/testATFI13.dcm


